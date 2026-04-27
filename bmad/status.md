# registry-mind — Status Report
*Generated: 2026-04-27 | Role: Architect - Orion*

## Product Overview
Android cognitive sensor local-first. Capture via Snap Key → OCR on-device → **LLM on-device (NPU)** → stockage local enrichi → export agnostique vers toute plateforme cible.

> **Pivot 2026-04-27:** Traitement LLM distant (OpenClaw server-side) annulé. Tout le traitement AI se fait sur le téléphone via MediaPipe + Gemma 2B-IT. ClawConnector rétrogradé du flow principal vers futur connecteur d'export optionnel.

## Progress: Sprint 3 ✅ COMPLETE — Sprint 4 PLANNED

## Architecture — Nouveau Flow

```
[Snap Key] → [ScreenCapturer] → [OcrProcessor] → [LocalLlm] → [EnrichedEntry DB]
                                                       ↓                ↓
                                              summarize/rewrite    Room local
                                                                        ↓
                                                         [ExportConnector] (futur, agnostique)
                                                         OpenClaw / API / filesystem / ...
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

## Sprint 4 — On-device LLM Pipeline 🔜 PLANNED
| Story | Titre | Effort | Priorité |
|-------|-------|--------|---------|
| S4-01 | LocalLlm — MediaPipe wrapper (init, summarize, rewrite) | 2h | P0 |
| S4-02 | Refactor RegistryIngestor — brancher LLM, supprimer sendPacket du flow principal | 2h | P0 |
| S4-03 | EnrichedEntry — Room DB local pour résultats LLM | 1h30 | P0 |
| S4-04 | ExportConnector — interface agnostique (pas d'impl) | 1h | P1 |
| S4-05 | ModelDownloadManager — téléchargement Gemma au premier lancement | 2h | P0 |
| S4-06 | Tests LocalLlm + RegistryIngestor refactorisé | 1h30 | P1 |

## Composants impactés par le pivot

| Composant | Avant | Après |
|-----------|-------|-------|
| `RegistryIngestor` | capture → `sendPacket()` | capture → `LocalLlm` → `EnrichedEntry` |
| `ClawConnector` | flow principal | futur `ExportConnector` optionnel |
| `SyncWorker` | retry réseau principal | retry export optionnel |
| `RegistryPacket` | payload réseau | structure de transport interne |
| `CacheManager` | retry queue réseau | stockage local primaire |
| `aiGuess` | envoyé au serveur | rempli localement par LLM |

## Next Action
**Sprint 4 — S4-01:** Implémenter `LocalLlm` (MediaPipe wrapper).
`bmad-continue`

---
*BMAD — Last updated: 2026-04-27 (Pivot architecture local-first LLM — Sprint 4 planifié)*
