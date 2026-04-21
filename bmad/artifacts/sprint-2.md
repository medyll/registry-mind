# Sprint 2 — Mind-Chat & Advanced UI

**Goal:** Implement voice interaction session (Mind-Chat) and Liquid Button advanced behaviors

**Duration:** 1 week  
**Status:** Ready to start

---

## Stories

### S2-01: Mind-Chat Hold-to-Talk UI
**Title:** Implement voice capture button with Waveform animation  
**Effort:** 1h30  
**Priority:** P1  
**Dependencies:** S1-02 (Overlay system)

**Acceptance Criteria:**
- [ ] Hold-to-Talk button appears in floating mode
- [ ] Waveform animation plays during voice capture
- [ ] Release sends voice command to processing
- [ ] Visual feedback (color change) during capture

**Tasks:**
1. Create `HoldToTalkButton.kt` Composable
2. Implement waveform animation (Compose Animation)
3. Integrate with audio recording
4. Add visual state feedback

---

### S2-02: Speech-to-Text Local
**Title:** Integrate ML Kit Speech-to-Text for voice commands  
**Effort:** 2h  
**Priority:** P1  
**Dependencies:** S2-01

**Acceptance Criteria:**
- [ ] Audio captured via MediaRecorder
- [ ] ML Kit Speech API transcribes to text
- [ ] Transcription completes in <2s
- [ ] Commands parsed (e.g., "pause", "new file", "sync now")

**Tasks:**
1. Add ML Kit Speech dependency
2. Implement `VoiceProcessor.kt`
3. Define command grammar (keywords)
4. Integrate with RegistryIngestor for actions

---

### S2-03: Liquid Button Drag-to-Float
**Title:** Implement anchored vs floating state logic  
**Effort:** 1h  
**Priority:** P2  
**Dependencies:** S1-02

**Acceptance Criteria:**
- [ ] Button anchored to edge by default
- [ ] Drag away from edge → becomes floating
- [ ] Drag to edge → re-anchors
- [ ] State persisted across app restarts

**Tasks:**
1. Update `LiquidButton.kt` with state machine
2. Implement edge detection (snap-to-edge)
3. Add settings toggle (always anchored / always floating)
4. Save state with DataStore/SharedPreferences

---

### S2-04: Session State Management
**Title:** Implement active_chat session state  
**Effort:** 1h  
**Priority:** P1  
**Dependencies:** S2-01

**Acceptance Criteria:**
- [ ] Session starts on first voice capture
- [ ] `navigation_meta.session_state = "active_chat"` during session
- [ ] Session ends after 30s inactivity
- [ ] Visual indicator shows active session

**Tasks:**
1. Create `SessionManager.kt` (singleton)
2. Define session lifecycle (start, active, end)
3. Update `RegistryPacket` with session state
4. Add session timeout timer

---

### S2-05: Veto Tempo UI
**Title:** Implement 3-second auto-commit progress bar  
**Effort:** 45min  
**Priority:** P2  
**Dependencies:** S1-02

**Acceptance Criteria:**
- [ ] Progress bar appears after capture
- [ ] 3-second countdown with visual feedback
- [ ] Tap to cancel (veto)
- [ ] Auto-commit on completion

**Tasks:**
1. Create `VetoTempoBar.kt` Composable
2. Implement countdown animation
3. Add cancel callback
4. Integrate with `RegistryIngestor.captureAndSend()`

---

### S2-06: Haptic Feedback Patterns
**Title:** Implement differentiated haptic feedback  
**Effort:** 30min  
**Priority:** P3  
**Dependencies:** S1-02

**Acceptance Criteria:**
- [ ] Single tick: Capture initiated
- [ ] Double pulse: Sync successful
- [ ] Long vibration: Error / cache stored
- [ ] Haptics respect system settings

**Tasks:**
1. Create `HapticFeedback.kt` utility
2. Define vibration patterns (VibrationEffect)
3. Integrate with capture/sync flow
4. Add settings toggle (enable/disable)

---

## Definition of Done (All Stories)

- [ ] Code reviewed by Reviewer role
- [ ] Unit tests written (target: 50% coverage)
- [ ] No lint warnings
- [ ] Artifacts written to `bmad/artifacts/stories/`

---

*Created by Scrum Master - Léo — 2026-04-18*
