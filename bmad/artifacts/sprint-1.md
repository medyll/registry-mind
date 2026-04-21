# Sprint 1 — Foundation & Capture Pipeline

**Goal:** Establish Android project scaffold and implement core capture pipeline (Snap Key → OCR → Packet)

**Duration:** 1 week  
**Status:** Ready to start

---

## Stories

### S1-01: Android Project Scaffold
**Title:** Set up Android project with Kotlin 2.1, Coroutines, and dependencies  
**Effort:** 30 min  
**Priority:** P0  
**Dependencies:** None

**Acceptance Criteria:**
- [ ] Android project created with minSdk 37 (Android 16)
- [ ] Kotlin 2.1 configured
- [ ] Coroutines + Flow dependencies added
- [ ] ML Kit OCR dependency added
- [ ] Retrofit 2 + OkHttp dependencies added
- [ ] Room DB dependencies added
- [ ] WorkManager dependency added
- [ ] Project builds successfully (`./gradlew build`)

**Tasks:**
1. Create Android project structure
2. Configure `build.gradle.kts` with all dependencies
3. Set up package structure (`com.registry.mind.*`)
4. Verify build succeeds

---

### S1-02: Snap Key Binding & Overlay
**Title:** Implement Snap Key hardware trigger and Transparent Overlay UI  
**Effort:** 45 min  
**Priority:** P0  
**Dependencies:** S1-01

**Acceptance Criteria:**
- [ ] Snap Key single press detected from any app
- [ ] Snap Key long press detected (for future radial menu)
- [ ] Transparent Overlay Layer appears without displacing foreground app
- [ ] Peripheral Glow (glassmorphic border) renders correctly
- [ ] Overlay auto-dismisses after 500ms
- [ ] No lag or stutter when overlay appears

**Tasks:**
1. Implement Snap Key listener service
2. Create Transparent Overlay composable (Jetpack Compose)
3. Implement Peripheral Glow animation
4. Test overlay from multiple apps

---

### S1-03: RegistryIngestor Pipeline
**Title:** Implement MediaProjection capture + ML Kit OCR pipeline  
**Effort:** 2h  
**Priority:** P0  
**Dependencies:** S1-02

**Acceptance Criteria:**
- [ ] MediaProjection captures full-resolution frame buffer
- [ ] ML Kit OCR extracts text with >95% accuracy on printed content
- [ ] OCR completes in <350ms (1080p content)
- [ ] App context (package name) scraped via Accessibility API
- [ ] Processing runs in background (Dispatchers.IO)
- [ ] Memory stays under 60MB during capture

**Tasks:**
1. Implement `RegistryIngestor` class
2. Integrate MediaProjection API
3. Add ML Kit OCR processor with NPU acceleration
4. Implement Accessibility API scraper for app context
5. Profile performance (latency, memory)

---

### S1-04: ClawConnector Interface
**Title:** Define ClawConnector abstraction + HTTP transport implementation  
**Effort:** 1h  
**Priority:** P0  
**Dependencies:** S1-03

**Acceptance Criteria:**
- [ ] `ClawConnector` interface defined (transport-agnostic)
- [ ] HTTP/REST transport implemented via Retrofit 2
- [ ] RegistryPacket JSON schema implemented (header, payload, navigation_meta)
- [ ] X-Registry-Auth header authentication
- [ ] Tailscale MagicDNS support (configurable base URL)
- [ ] Packet sends successfully to test endpoint

**Tasks:**
1. Define `ClawConnector` interface
2. Create `RegistryPacket` data classes
3. Implement Retrofit service with JSON serialization
4. Add authentication header interceptor
5. Test packet transmission to mock server

---

### S1-05: BroadcastReceiver for Desktop Signals
**Title:** Implement BroadcastReceiver to handle OpenClaw → mobile signals  
**Effort:** 45 min  
**Priority:** P1  
**Dependencies:** S1-04

**Acceptance Criteria:**
- [ ] `BroadcastReceiver` registered for `com.registry.mind.ACTION_*` intents
- [ ] Can receive "Action Required" signals from desktop
- [ ] Visual notification shows when signal received
- [ ] Receiver survives app restarts

**Tasks:**
1. Define intent filter actions
2. Implement `OpenClawReceiver` class
3. Register receiver in manifest
4. Test signal reception from test harness

---

### S1-06: Performance Validation
**Title:** Validate performance targets and optimize  
**Effort:** 30 min  
**Priority:** P0  
**Dependencies:** S1-03, S1-04

**Acceptance Criteria:**
- [ ] Wake-up latency <100ms (verified via profiling)
- [ ] OCR completion <350ms (verified via profiling)
- [ ] Memory usage <60MB during capture (verified via Memory Profiler)
- [ ] Battery drain 0% when idle (verified via Battery Historian)
- [ ] All acceptance criteria from S1-01 to S1-05 met

**Tasks:**
1. Run Android Profiler on all metrics
2. Document baseline performance
3. Optimize bottlenecks if any
4. Write performance validation report

---

## Definition of Done (All Stories)

- [ ] Code reviewed by Reviewer role
- [ ] Unit tests written and passing
- [ ] E2E test (capture → send) passing
- [ ] No lint warnings
- [ ] Performance targets met
- [ ] Artifacts written to `bmad/artifacts/stories/`

---

*Created by Scrum Master - Léo — 2026-04-18*
