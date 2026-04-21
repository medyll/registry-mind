# Product Requirements Document: registry-mind

**Version:** 1.0  
**Date:** 2026-04-18  
**Status:** Draft  
**Platform:** Android 16 (Oppo Find X9 Series)

---

## 1. Executive Summary

**registry-mind** is a sovereign system-level utility that replaces proprietary AI capture tools (AI Mind Space) with an open, privacy-first alternative. It acts as a "Cognitive Sensor" — capturing screen content + OCR text and relaying structured data to the OpenClaw desktop intelligence hub for semantic indexing and long-term storage.

**Core Principle:** Capture on mobile, think on desktop.

---

## 2. Problem Statement

### 2.1 Current Limitations (AI Mind Space)
- Proprietary data lock-in (encrypted partition, no export)
- Limited to visible screen pixels (no scroll capture)
- Regional API dependencies for advanced features
- No bidirectional automation capabilities
- Closed ecosystem (cannot integrate with external tools)

### 2.2 User Needs
- Sovereign ownership of captured data
- Frictionless capture with hardware integration
- Offline-first reliability
- Universal connectivity (not tied to one cloud provider)
- Extensible architecture for future automation

---

## 3. Product Vision

### 3.1 Core Value Proposition
A invisible, event-driven Android service that transforms fleeting screen moments into structured, searchable knowledge — without cognitive load or vendor lock-in.

### 3.2 Target Users
- Power users managing high-volume information flows
- Privacy-conscious professionals
- OpenClaw ecosystem users
- Developers building on sovereign AI infrastructure

### 3.3 Success Metrics
| Metric | Target |
|--------|--------|
| Wake-up latency | <100ms |
| OCR completion | <350ms |
| Memory footprint | <60MB active, 0MB idle |
| Battery drain | 0% idle (event-driven) |
| Capture success rate | >99% |
| Offline resilience | Unlimited local cache |

---

## 4. Functional Requirements

### 4.1 Capture System (FR-01)
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-01.1 | Single press of Snap Key triggers instant capture (screenshot + OCR) | P0 |
| FR-01.2 | Long press invokes radial menu for quick tagging (Urgent/Personal/Work) | P1 |
| FR-01.3 | Three-finger swipe gesture as software alternative to Snap Key | P1 |
| FR-01.4 | Capture includes: full-resolution frame, OCR text, source app package name | P0 |
| FR-01.5 | Visual feedback via Peripheral Glow (glassmorphic border overlay) | P1 |
| FR-01.6 | Auto-dismissal 500ms after successful packet delivery | P1 |

### 4.2 Processing Pipeline (FR-02)
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-02.1 | MediaProjection API for screen buffer capture | P0 |
| FR-02.2 | ML Kit OCR with NPU acceleration (NNAPI) | P0 |
| FR-02.3 | Concurrent processing (OCR + image encoding in parallel) | P0 |
| FR-02.4 | App context scraping via Accessibility API | P1 |
| FR-02.5 | Local AI classification (ai_guess field: e.g., "finance_facture") | P2 |

### 4.3 Connectivity (FR-03)
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-03.1 | ClawConnector abstraction layer (transport-agnostic) | P0 |
| FR-03.2 | HTTP/REST transport via Retrofit 2 | P0 |
| FR-03.3 | Tailscale MagicDNS for zero-config networking | P0 |
| FR-03.4 | X-Registry-Auth static key authentication | P0 |
| FR-03.5 | Offline cache with Room DB (SQLite) | P0 |
| FR-03.6 | WorkManager background sync with connectivity constraints | P1 |
| FR-03.7 | Retry logic with exponential backoff | P1 |

### 4.4 User Interface (FR-04)
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-04.1 | Transparent Overlay Layer (does not displace foreground app) | P0 |
| FR-04.2 | Peripheral Glow visual indicator (color-coded: Blue=Capturing, Green=Synced, Red=Error) | P1 |
| FR-04.3 | Liquid Button: Anchored (edge-fixed) vs Floating (bubble) modes | P2 |
| FR-04.4 | Drag-to-float, drop-to-anchor gesture for button state | P2 |
| FR-04.5 | Mind-Chat session mode with hold-to-talk voice input | P2 |
| FR-04.6 | Waveform animation during voice capture | P2 |
| FR-04.7 | 3-second Veto Tempo bar (auto-commit with cancel option) | P2 |

### 4.5 Bidirectional Communication (FR-05)
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-05.1 | BroadcastReceiver for OpenClaw → mobile signals | P1 |
| FR-05.2 | Handle "Action Required" notifications from desktop | P1 |
| FR-05.3 | Future: Android Intent automation (create reminders, open apps) | P3 |

### 4.6 Haptic Feedback (FR-06)
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-06.1 | Single tick: Capture initiated | P2 |
| FR-06.2 | Double pulse: Sync successful | P2 |
| FR-06.3 | Long vibration: Cache stored / error occurred | P2 |

---

## 5. Non-Functional Requirements

### 5.1 Performance (NFR-01)
| ID | Requirement |
|----|-------------|
| NFR-01.1 | Cold start to capture: <100ms |
| NFR-01.2 | OCR processing: <350ms (1080p content) |
| NFR-01.3 | Memory usage: <60MB during capture, 0MB when idle |
| NFR-01.4 | Battery impact: 0% idle drain |
| NFR-01.5 | Network transmission: <2s on Wi-Fi (1MB payload) |

### 5.2 Privacy & Security (NFR-02)
| ID | Requirement |
|----|-------------|
| NFR-02.1 | All OCR processing on-device (no cloud OCR) |
| NFR-02.2 | Encrypted local storage (Android EncryptedSharedPreferences) |
| NFR-02.3 | Static auth key stored in Android Keystore |
| NFR-02.4 | No analytics, telemetry, or third-party SDKs |
| NFR-02.5 | User owns all captured data (exportable JSON) |

### 5.3 Reliability (NFR-03)
| ID | Requirement |
|----|-------------|
| NFR-03.1 | Offline-first: unlimited local cache capacity |
| NFR-03.2 | Automatic retry on network failure (WorkManager) |
| NFR-03.3 | Graceful degradation (OCR fails → image-only capture still sent) |
| NFR-03.4 | Crash recovery (pending packets preserved) |

### 5.4 Compatibility (NFR-04)
| ID | Requirement |
|----|-------------|
| NFR-04.1 | Android 16 (API 37) minimum |
| NFR-04.2 | Oppo Find X9 series (ColorOS 16) optimized |
| NFR-04.3 | Universal: works with any desktop HTTP endpoint |
| NFR-04.4 | Tailscale optional (manual IP config fallback) |

---

## 6. Data Model

### 6.1 Registry Packet Schema
```json
{
  "header": {
    "protocol": "registry-mind-v1",
    "device": "Oppo_Find_X9_Native",
    "timestamp": "ISO_8601_Z",
    "auth_token": "X-Registry-Auth-Static-Key"
  },
  "payload": {
    "image_data": "base64_webp_blob",
    "ocr_content": "Full extracted text...",
    "source_app": "com.android.chrome",
    "ai_guess": "finance_facture"
  },
  "navigation_meta": {
    "role": "registry_sensor",
    "session_state": "active_chat"
  }
}
```

### 6.2 Local Cache Schema (Room DB)
```kotlin
@Entity(tableName = "pending_packets")
data class PendingPacket(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val packet_json: String,
    val created_at: Long,
    val retry_count: Int,
    val last_attempt: Long?
)
```

---

## 7. Out of Scope (V1)

| Feature | Reason | Future Version |
|---------|--------|----------------|
| Scroll capture (multi-page) | Complex UI, not core use case | V2 |
| Real-time multi-speaker transcription | Regional API dependency | V2 |
| Continuous streaming mode | Battery impact, complexity | V3 (MCP) |
| Multi-personality config sync | Advanced use case | V3 (MCP) |
| Full Android Intent automation | Security review needed | V3 (MCP) |

---

## 8. Technical Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.1 |
| Concurrency | Coroutines + Flow |
| UI | Jetpack Compose (Overlay) |
| OCR | ML Kit (on-device) |
| Network | Retrofit 2 + OkHttp |
| Local DB | Room (SQLite) |
| Background Sync | WorkManager |
| Networking | Tailscale (MagicDNS) |
| Auth | Android Keystore + Static Key |

---

## 9. Acceptance Criteria (V1 Launch)

- [ ] Snap Key triggers capture from any app (including AOD)
- [ ] OCR extracts text with >95% accuracy on printed content
- [ ] Packet transmitted to OpenClaw endpoint successfully
- [ ] Offline cache stores packets when network unavailable
- [ ] Auto-retry syncs cached packets when network returns
- [ ] Visual feedback (glow) visible without blocking content
- [ ] Memory stays under 60MB during active capture
- [ ] No battery drain when idle (verified via battery historian)
- [ ] App survives process death (pending packets preserved)

---

## 10. Open Questions

| Question | Owner | Resolution Needed |
|----------|-------|-------------------|
| Tailscale integration: bundled app or user installs separately? | Architect | Before Sprint 1 |
| Auth key rotation strategy? | Security | Before Sprint 2 |
| Handwriting OCR accuracy threshold for launch? | PM | Before Sprint 1 |
| Floating bubble permission requirements (draw over other apps)? | Developer | Sprint 1 |

---

*Generated by BMAD PM role — 2026-04-18*
