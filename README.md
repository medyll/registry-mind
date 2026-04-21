# Registry-Mind V1

**Date:** 2026-04-12
**Status:** 🌱 Germination
**Standard:** Core-Standard V1

---

## Sources

- [`ferule-core/README.md`](../../../README.md)
- [`../../USER-NOTES.md`](../../USER-NOTES.md)
- [`../../BLUEPRINT.md`](../../BLUEPRINT.md) — origin technical specification

---

## Overview

Registry-Mind is a sovereign Android 16 capture sensor for the Oppo Find X9 series. It replaces proprietary AI Mind Space, acting as a "Cognitive Sensor" that relays screen captures + OCR text to the OpenClaw desktop intelligence hub.

**Principle:** capture on mobile, think on desktop.

---

## Structure

| File | Purpose |
|------|---------|
| [phase-1-repair.md](phase-1-repair.md) | V1 scaffold, pipeline implementation, ClawConnector design |
| [reports/bug-reports.md](reports/bug-reports.md) | Bug log |
| [reports/deployment-report.md](reports/deployment-report.md) | Deployment report |

---

## Summary Table

| Step | Task | Effort | Depends on | Status |
|------|------|--------|------------|--------|
| **1.1** | Scaffold Android project (Kotlin 2.1, Coroutines) | 30 min | — | 📋 |
| **1.2** | Implement Snap Key binding → Transparent Overlay | 45 min | 1.1 | 📋 |
| **1.3** | Implement `RegistryIngestor` (MediaProjection + ML Kit OCR) | 2h | 1.2 | 📋 |
| **1.4** | Define `ClawConnector` interface + first transport (HTTP/WebSocket) | 1h | 1.3 | 📋 |
| **1.5** | Implement `BroadcastReceiver` for OpenClaw → mobile signals | 45 min | 1.4 | 📋 |
| **1.6** | Performance validation (<100ms wake, <350ms OCR, <60MB) | 30 min | 1.5 | 📋 |

---

## Current System State

| Component | Status |
|-----------|--------|
| BLUEPRINT.md | ✅ Captured |
| USER-NOTES.md | ✅ Derived from BLUEPRINT (2026-04-12) |
| V1 scaffold | ✅ Created (2026-04-12) |
| Android project | 📋 Not started |
| ClawConnector | 📋 Not started |
