# registry-mind — Status Report
*Generated: 2026-04-27 | Role: Developer - Manon*

## Product Overview
Android cognitive sensor local-first. Capture via Snap Key → OCR on-device → **LLM on-device (NPU)** → stockage local enrichi → export agnostique vers toute plateforme cible.

> **Pivot 2026-04-27:** Traitement LLM distant (OpenClaw server-side) annulé. Tout le traitement AI se fait sur le téléphone via MediaPipe + Gemma 2B-IT. ClawConnector rétrogradé du flow principal vers futur connecteur d'export optionnel.

## Progress: Sprint 4 ✅ COMPLETE — 95%

## Architecture — Flow actif

```
[Snap Key] → [ScreenCapturer] → [OcrProcessor] → [LocalLlm] → [EnrichedEntry DB]
                                                       ↓                ↓
                                              summarize/rewrite    Room local (v3)
                                                                        ↓
                                                         [ExportConnector] (interface prête)
                                                         ClawConnector / API / filesystem / ...
```

## Sprint 1 — Foundation & Capture Pipeline ✅ COMPLETE
| Story | Titre | Tests |
|-------|-------|-------|
| S1-01 | Android Project Scaffold | ✅ pass |
| S1-02 | Snap Key Binding & Overlay | ✅ partial |
| S1-03 | RegistryIngestor Pipeline | ✅ pass |
| S1-04 | ClawConnector Interface | ✅ partial |
| S1-05 | BroadcastReceiver for Desktop Signals | ✅ partial |
| S1-06 | Performance Validation | ✅ partial |

## Sprint 2 — Mind-Chat & Advanced UI ✅ COMPLETE
| Story | Titre | Tests |
|-------|-------|-------|
| S2-01 | Mind-Chat Hold-to-Talk UI | ✅ pass |
| S2-02 | Speech-to-Text Local (VoiceProcessor) | ✅ pass |
| S2-03 | Liquid Button Drag-to-Float | ✅ pass |
| S2-04 | Session State Management | ✅ pass |
| S2-05 | Veto Tempo UI | ✅ pass |
| S2-06 | Haptic Feedback Patterns | ✅ pass |

## Sprint 3 — Connectivity, Security & Polish ✅ COMPLETE
| Story | Titre | Tests |
|-------|-------|-------|
| S3-01 | Auth Key → SettingsManager (EncryptedSharedPreferences) | ✅ pass |
| S3-02 | WorkManager retry + connectivity constraints | ✅ pass |
| S3-03 | Endpoint config Tailscale / IP fallback | ✅ pass |
| S3-04 | PeripheralGlow color-coded (Blue/Green/Red) | ✅ pass |
| S3-05 | Long-press radial menu (Urgent/Personal/Work) | ✅ pass |
| S3-06 | Tests ClawConnector + RegistryIngestor (7+9 tests) | ✅ pass |

## Sprint 4 — On-device LLM Pipeline ✅ COMPLETE
| Story | Titre | Tests |
|-------|-------|-------|
| S4-01 | LocalLlm — MediaPipe wrapper (init, summarize, rewrite) | ✅ pass |
| S4-02 | Refactor RegistryIngestor — LLM branché, sendPacket retiré du flow | ✅ pass |
| S4-03 | EnrichedEntry — Room DB v3 (rawText, summary, rewrite, tag, timestamp) | ✅ pass |
| S4-04 | ExportConnector — interface agnostique | ✅ pass |
| S4-05 | ModelDownloadManager — download Gemma avec progress | ✅ pass |
| S4-06 | Tests — 42 tests, 0 failures | ✅ pass |

## Composants actifs

| Composant | Rôle |
|-----------|------|
| `LocalLlm` | MediaPipe wrapper — summarize/rewrite on NPU |
| `RegistryIngestor` | capture → OCR → LocalLlm → EnrichedEntry → Room |
| `EnrichedEntry` | unité de stockage primaire (Room v3) |
| `ExportConnector` | interface d'export agnostique (aucun connecteur actif) |
| `ModelDownloadManager` | download + vérification Gemma 2B-IT Q4 |
| `ClawConnector` | futur ExportConnector optionnel |

## Next Action
**Sprint 5 — planification:** UI enriched entries, implémentations ExportConnector, ou gestion modèle.
`bmad-sprint`

---
*BMAD — Last updated: 2026-04-27 (Sprint 4 complete — 42 tests pass — local-first LLM pipeline opérationnel)*
