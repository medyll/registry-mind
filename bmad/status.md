# registry-mind — Status Report
*Generated: 2026-04-27 | Role: Developer - Léo*

## Product Overview
Android cognitive sensor local-first. Snap Key → OCR → on-device LLM (MediaPipe/Gemma) → enriched local storage → agnostic export.

> **Pivot 2026-04-27:** All AI enrichment on-device via MediaPipe + Gemma 2B-IT. ClawConnector demoted to optional future ExportConnector.

## Progress: Sprint 5 ✅ COMPLETE — 98%

## Architecture

```
[Snap Key] → [ScreenCapturer] → [OcrProcessor] → [LocalLlm] → [EnrichedEntry DB]
                ↓ VetoBar                              ↓                ↓
           processPacket()                    summarize/rewrite    Room v3
           (no double-capture)                                          ↓
                                                         [ExportConnector] (interface — no impl yet)
```

## Sprint 1–3 ✅ COMPLETE
Foundation, UI, Connectivity, Security — all stories pass.

## Sprint 4 — On-device LLM Pipeline ✅ COMPLETE
| Story | Tests |
|-------|-------|
| S4-01 LocalLlm MediaPipe wrapper | ✅ pass |
| S4-02 RegistryIngestor refactor | ✅ pass |
| S4-03 EnrichedEntry Room v3 (+ rewrite field) | ✅ pass |
| S4-04 ExportConnector interface | ✅ pass |
| S4-05 ModelDownloadManager | ✅ pass |
| S4-06 Tests (42 total) | ✅ pass |

## Sprint 5 — LLM Wiring & Entries UI ✅ COMPLETE
| Story | Tests |
|-------|-------|
| S5-01 Fix double-capture — processPacket() | ✅ pass |
| S5-02 LocalLlm init in CaptureService | ✅ pass |
| S5-03 First-launch model download + progress UI | ✅ pass |
| S5-04 EntriesScreen — LazyColumn captures view | ✅ pass |
| S5-05 Tests — 42 tests, 0 failures | ✅ pass |

## What works end-to-end
- Snap Key → capture → VetoBar → processPacket() (same screenshot, no double-capture)
- CaptureService inits LocalLlm async if Gemma model present
- First launch: ModelDownloadDialog triggers ~1.3GB Gemma download with progress
- EntriesScreen: browse enriched captures (summary, tag, sourceApp, timestamp, export status)
- Settings: endpoint, auth token, haptics, model URL

## What's left for release
- ClawConnector as `ExportConnector` impl (HTTP POST enriched entry)
- README / CHANGELOG
- Performance validation (NFR-01: <100ms wake, <350ms OCR)

## Next Action
**Sprint 6** — ClawConnector ExportConnector impl + release prep, or `bmad-audit` first.
`bmad-sprint`

---
*BMAD — Last updated: 2026-04-27 (Sprint 5 complete — full LLM pipeline wired, 42 tests pass)*
