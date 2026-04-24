# Registry Mind

Android screen-capture sensor for Oppo Find X9. Captures via Snap Key → on-device OCR (ML Kit) → syncs to OpenClaw desktop over Tailscale. Replaces Oppo's proprietary AI Mind Space with a self-hosted pipeline.

```
Snap Key press
    → MediaProjection capture
    → ML Kit OCR (on-device, no cloud)
    → RegistryPacket (JSON)
    → Tailscale → OpenClaw /ingest
```

---

## Requirements

### Desktop

- **OpenClaw** running and reachable via Tailscale, exposing `/health` and `/ingest`
- **Tailscale** connected — note your machine's MagicDNS hostname (e.g. `your-machine.tail12345.ts.net`)
- **Android Studio** Ladybug or later (for build/deploy only)

### Device

- Oppo Find X9 / Find X9 Pro, Android 16
- Tailscale installed and authenticated on the same account as desktop
- USB debugging enabled (Settings → About Phone → tap Build Number ×7 → Developer Options → USB Debugging)

---

## Build & Deploy

```bash
# Clone and open in Android Studio, or build from CLI:
./gradlew assembleDebug

# Deploy to connected device:
./gradlew installDebug
```

Android Studio: open the project root, wait for Gradle sync, hit **Run ▶** with the device selected in the toolbar.

The app requires no signing configuration for debug builds. Release signing is out of scope for V1.

---

## First-Run Configuration

On launch, open **Settings** (bottom of main screen) and configure:

| Field | Value |
|-------|-------|
| Endpoint URL | `http://your-machine.tail12345.ts.net:8080` |
| Auth Token | Value of `X-Registry-Auth` expected by OpenClaw |
| Timeout | Connection + read timeout in seconds (default: 30) |
| Haptic Feedback | Toggle vibration on capture/sync/error events |

Hit **Test Connection** to verify reachability before saving. The button fires a GET to `{endpoint}/health` and reports latency. Any HTTP response counts as reachable.

Settings are stored in `EncryptedSharedPreferences` (AES256-GCM via Android Keystore). The auth token is masked in the UI by default.

---

## Permissions

The following permissions are declared and must be granted at runtime:

| Permission | Purpose |
|------------|---------|
| `SYSTEM_ALERT_WINDOW` | Floating button + peripheral glow overlay |
| `BIND_ACCESSIBILITY_SERVICE` | Snap Key interception via `SnapKeyService` |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Screen capture via `MediaProjection` |
| `RECORD_AUDIO` | Voice dictation (`VoiceProcessor`) |
| `RECEIVE_BOOT_COMPLETED` | Service auto-restart via `BootReceiver` |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Tailscale transport + WorkManager connectivity constraint |

If a permission is denied post-install: Settings → Apps → Registry Mind → Permissions.

---

## Usage

**Screen capture:** Press the physical Snap Key. A blue peripheral glow confirms capture initiation. A 3-second veto bar appears at the bottom — tap ✕ to discard, or let it expire to commit and send.

**Tagging:** Long-press the floating button to open the radial tag menu (Urgent / Personal / Work). The selected tag is applied to the next capture only (consumed on send).

**Voice capture:** Hold the floating button to record. Release to submit. Transcription is processed locally via Android's `SpeechRecognizer`.

**Glow states:**
- 🔵 Blue — capture in progress
- 🟢 Green — packet sent successfully
- 🔴 Red — send failed; packet queued for retry

**Offline resilience:** Failed packets are persisted to a local Room database. `SyncWorker` (WorkManager) retries with exponential backoff whenever `NetworkType.CONNECTED` is satisfied. No manual intervention required.

---

## Architecture

```
SnapKeyService (AccessibilityService)
    └─ CaptureService.startCapture()
          ├─ ScreenCapturer        → Bitmap via MediaProjection
          ├─ OcrProcessor          → ML Kit text extraction
          ├─ AppContextScraper     → foreground app package + activity
          ├─ SessionManager        → session state, tag, NavigationMeta
          └─ CaptureOverlayManager → glow / veto bar / radial menu (WindowManager overlays)

RegistryIngestor
    ├─ captureOnly()   → Result<RegistryPacket>
    └─ sendPacket()    → ClawConnector → Retrofit → OpenClaw /ingest
                       → on failure: CacheManager → Room → SyncWorker (WorkManager)

RegistryMindApplication.onCreate()
    ├─ SettingsManager.initialize()   (EncryptedSharedPreferences)
    ├─ ClawConnector.initialize()     (reads from SettingsManager)
    ├─ CacheManager.initialize()
    └─ HapticFeedback.initialize()
```

---

## Packet Schema

```json
{
  "id": "uuid-v4",
  "timestamp": "2026-04-22T14:32:00Z",
  "ocr_text": "...",
  "app_context": {
    "package": "com.example.app",
    "activity": ".MainActivity"
  },
  "navigation_meta": {
    "tag": "WORK"
  },
  "auth_token": "X-Registry-Auth value"
}
```

`tag` is `null` if no radial selection was made before the capture.

---

## OpenClaw + Tailscale Integration

### Role split

Registry Mind and OpenClaw are two halves of the same system with a strict separation of responsibilities:

| Component | Role | Responsibilities |
|-----------|------|-----------------|
| **Registry Mind** (this app) | Capture Sensor | Snap Key binding, screen capture, on-device OCR, packet assembly, local cache, transport |
| **OpenClaw** (desktop) | Intelligence Hub | LLM restructuring, semantic indexing, vector storage, permanent archiving, bidirectional signaling |

Registry Mind has no long-term storage and no LLM. It is a relay. All interpretation, classification, and archiving is delegated to OpenClaw. The mobile side stays minimal by design — the constraint is intentional, not a limitation.

### Why Tailscale

The mobile-to-desktop transport requirement is: fixed addressing with zero network configuration, across NAT, without exposing a public IP or setting up port forwarding.

Tailscale solves this via WireGuard tunnels managed through a coordination server. Once both devices are authenticated to the same Tailscale account, they get stable private IPs and MagicDNS hostnames (e.g. `your-machine.tail12345.ts.net`) that work regardless of which network either device is on. From registry-mind's perspective, OpenClaw is always at the same address.

There is no fallback transport in V1. If Tailscale is not active on the device, sends fail and packets queue locally until connectivity is restored.

### ClawConnector design

`ClawConnector` is destination-agnostic. It is configured with an endpoint URL and an auth token — both runtime values from `SettingsManager`. It does not hardcode any OpenClaw-specific logic.

This means the same connector can point to any REST endpoint that accepts the packet schema below. OpenClaw is the default target, but the architecture does not depend on it. Swapping the destination is a settings change, not a code change.

```
SettingsManager (endpoint_url, auth_token)
    └─ ClawConnector
          └─ Retrofit + OkHttp → POST {endpoint}/ingest
                                   Header: X-Registry-Auth: {token}
```

`ClawConnector.refreshConfig()` re-reads from `SettingsManager` without restarting the service — changes take effect on the next send.

### Full data flow

```
Snap Key
    │
    ▼
ScreenCapturer (MediaProjection)
    │  full-resolution Bitmap
    ▼
OcrProcessor (ML Kit, on-device)
    │  raw text strings
    ▼
AppContextScraper (Accessibility API)
    │  package name + activity name
    ▼
RegistryIngestor.captureOnly()
    │  assembles RegistryPacket
    ▼
[3s veto window]
    │  user lets it expire or taps Commit
    ▼
ClawConnector.sendPacket()
    │  POST over Tailscale WireGuard tunnel
    ▼
OpenClaw /ingest
    ├─ LLM restructuring
    ├─ Semantic / vector indexing
    └─ Permanent archiving
```

On send failure: packet is persisted to Room (SQLite), and `SyncWorker` retries with exponential backoff under a `NetworkType.CONNECTED` constraint.

### Packet schema

Every capture is wrapped in a standardized `RegistryPacket`. This is the contract between registry-mind and OpenClaw:

```json
{
  "header": {
    "protocol": "registry-mind-v1",
    "device": "Oppo_Find_X9_Native",
    "timestamp": "2026-04-22T14:32:00Z",
    "auth_token": "X-Registry-Auth-value"
  },
  "payload": {
    "image_data": "base64_webp_blob",
    "ocr_content": "Full extracted text from display...",
    "source_app": "com.android.chrome",
    "ai_guess": null
  },
  "navigation_meta": {
    "role": "registry_sensor",
    "tag": "WORK"
  }
}
```

Fields of note: `image_data` is a base64-encoded WebP blob of the full capture. `ai_guess` is reserved for future local pre-classification — currently always `null`. `tag` is `null` if no radial selection was made before the capture; it is consumed on send and reset to `null` afterward.

### Bidirectional signaling

The channel is not unidirectional. OpenClaw can push signals back to the device via Android broadcasts. `OpenClawReceiver` (`BroadcastReceiver`) listens for three intents:

| Intent action | Purpose |
|---------------|---------|
| `com.registry.mind.ACTION_SIGNAL` | General signal from desktop (e.g. "Action Required" notification) |
| `com.registry.mind.ACTION_SYNC` | Trigger an immediate sync of the local cache |
| `com.registry.mind.ACTION_FLUSH` | Flush pending queue |

These intents can be sent from OpenClaw to the device over the Tailscale tunnel via ADB or a desktop-side Android intent bridge. The handling logic for `ACTION_SIGNAL` payloads (e.g. surfacing a notification or triggering a UI response) is not yet implemented in V1 — the receiver is wired but the downstream behavior is a V2 concern.

---

## Testing

Unit tests cover `ClawConnector` (MockWebServer, 6 tests) and `RegistryIngestor` (9 tests).

```bash
./gradlew test
```

Key test cases: HTTP 200/401/500 handling, auth header injection, endpoint path correctness, `NavigationMeta.tag` serialization, `classifyContent` for text/link/code/mixed content.

---

## Performance Targets (V1)

| Metric | Target |
|--------|--------|
| Wake latency (Snap Key → capture start) | < 100 ms |
| OCR processing | < 350 ms |
| Memory footprint | < 60 MB |

Not yet validated against physical device — pending testing on Oppo Find X9.

---

## Project Status

Three sprints complete. See [`bmad/status.md`](bmad/status.md) for full story breakdown and execution chain documentation.

**Remaining before V1 ship:**
- Physical device testing (Oppo Find X9)
- Performance profiling against targets above
- JSON packet export (NFR-02.5)
