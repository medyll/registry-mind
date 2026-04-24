# Sprint 3 — Connectivity, Security & Polish

**Goal:** Fermer les P0/P1 manquants du PRD V1 : sécurisation de la clé auth, sync offline robuste, config endpoint, UX glow coloré, radial menu de tags, couverture de tests.

**Duration:** 1 semaine  
**Status:** In Progress  
**Depends on:** Sprint 2 complete ✅

---

## Stories

### S3-01: Auth Key → Android Keystore
**Title:** Stocker et injecter la clé d'authentification via Android Keystore  
**Effort:** 1h  
**Priority:** P0  
**PRD Ref:** NFR-02.3

**Context:**  
`"YOUR_STATIC_API_KEY_HERE"` est hardcodé dans `RegistryIngestor`, `CacheManager`, et `NetworkConfig`. Toute personne qui ouvre le APK ou lit le code source a la clé. Violation P0 du PRD.

**Acceptance Criteria:**
- [ ] `KeystoreManager` singleton expose `getAuthToken(): String` et `setAuthToken(key: String)`
- [ ] Clé stockée dans `EncryptedSharedPreferences` (alias Keystore AES256-GCM)
- [ ] `Header.authToken` injecté depuis `KeystoreManager` partout
- [ ] Valeur par défaut vide → Settings UI obligatoire avant premier envoi

**Tasks:**
1. Créer `security/KeystoreManager.kt`
2. Remplacer les 4 occurrences hardcodées
3. Initialiser dans `RegistryMindApplication`

---

### S3-04: PeripheralGlow Color-Coded
**Title:** Glow Blue=Capturing / Green=Synced / Red=Error  
**Effort:** 45min  
**Priority:** P1  
**PRD Ref:** FR-04.2

**Context:**  
Le glow actuel est monochrome. Le PRD exige un feedback visuel différencié par état : capture en cours (bleu), sync réussi (vert), erreur (rouge). `CaptureService` orchestre les états — il doit les transmettre à `CaptureOverlayManager`.

**Acceptance Criteria:**
- [ ] `GlowState` enum : `CAPTURING`, `SYNCED`, `ERROR`
- [ ] `PeripheralGlow` reçoit `GlowState` et affiche la couleur correspondante
- [ ] `CaptureService` émet CAPTURING au déclenchement, SYNCED sur success, ERROR sur failure
- [ ] Glow reste visible 500ms (CAPTURING) ou 800ms (SYNCED/ERROR)

**Tasks:**
1. Ajouter `GlowState` enum dans `ui/overlay/`
2. Mettre à jour `PeripheralGlow.kt` avec param `state: GlowState`
3. Mettre à jour `CaptureOverlayManager.showPeripheralGlow(state)`
4. Câbler dans `CaptureService.startCapture()`

---

### S3-02: WorkManager Sync Robuste
**Title:** Retry offline avec exponential backoff et constraint réseau  
**Effort:** 1h30  
**Priority:** P1  
**PRD Ref:** FR-03.6, FR-03.7

**Context:**  
`SyncWorker.kt` existe mais n'est pas déclenché depuis le flow de capture. `CacheManager.storeForRetry()` stocke les packets en Room DB mais ne planifie pas de WorkManager job. Il manque aussi la contrainte de connectivité et le backoff.

**Acceptance Criteria:**
- [ ] `CacheManager.storeForRetry()` enqueue un `SyncWorker` via WorkManager
- [ ] Constraint : `NetworkType.CONNECTED`
- [ ] Retry policy : exponential backoff (initial 30s, max 1h)
- [ ] `SyncWorker` lit les packets en cache, tente l'envoi, supprime si succès
- [ ] `HapticFeedback.syncSuccessful()` déclenché après flush cache réussi

**Tasks:**
1. Mettre à jour `CacheManager.storeForRetry()` → `WorkManager.enqueueUniqueWork()`
2. Mettre à jour `SyncWorker.doWork()` → read cache → send → delete on success
3. Ajouter `KeystoreManager` dans le path d'envoi (S3-01 dépendance)

---

### S3-03: Endpoint Config (Tailscale / IP Fallback)
**Title:** URL d'endpoint configurable — Tailscale MagicDNS ou IP manuelle  
**Effort:** 1h  
**Priority:** P1  
**PRD Ref:** FR-03.3, FR-03.4

**Context:**  
`NetworkConfig` hardcode l'URL de base. Le PRD requiert Tailscale MagicDNS zero-config avec fallback IP manuelle. Un `SettingsManager` expose les préférences persistées, et `NetworkConfig` les lit pour reconfigurer Retrofit dynamiquement.

**Acceptance Criteria:**
- [ ] `SettingsManager` singleton : `getEndpointUrl()`, `setEndpointUrl(url)`, `getAuthToken()`, `setAuthToken(key)`
- [ ] `NetworkConfig` construit Retrofit depuis `SettingsManager.getEndpointUrl()`
- [ ] Default URL : `http://openiris-desktop.tailscale.net:8080`
- [ ] Fallback : saisie IP/port manuelle (persistée)
- [ ] Retrofit se reconfigure si l'URL change (singleton invalidé)

**Tasks:**
1. Créer `settings/SettingsManager.kt`
2. Mettre à jour `NetworkConfig` → lit depuis `SettingsManager`
3. Migrer `KeystoreManager` auth token → `SettingsManager` (unifier les prefs)
4. Exposer `SettingsManager.initialize(context)` dans `RegistryMindApplication`

---

### S3-05: Long-Press Radial Menu (Urgent/Personal/Work)
**Title:** Radial tag menu sur long-press du Liquid Button  
**Effort:** 1h30  
**Priority:** P1  
**PRD Ref:** FR-01.2

**Context:**  
`LiquidButton.onLongPress` est câblé mais ne fait rien de visible. Le PRD demande un radial menu avec 3 tags rapides. Le tag enrichit le prochain packet capturé via un champ `tag` ajouté à `NavigationMeta`.

**Acceptance Criteria:**
- [ ] `RadialTagMenu` composable : 3 options (🔴 Urgent / 🔵 Personal / 🟢 Work)
- [ ] Apparaît sur long-press, disparaît sur sélection ou tap ailleurs
- [ ] Tag sélectionné persisté dans `SessionManager.currentTag`
- [ ] `NavigationMeta` étendu avec champ `tag: String?`
- [ ] `RegistryIngestor.captureOnly()` inclut le tag courant dans le packet

**Tasks:**
1. Créer `ui/components/RadialTagMenu.kt`
2. Étendre `NavigationMeta` avec `tag: String?`
3. Ajouter `currentTag` dans `SessionManager`
4. Câbler dans `CaptureOverlayManager.showLiquidButton()` (onLongPress → showRadialMenu)
5. Injecter tag dans `RegistryIngestor.captureOnly()`

---

### S3-06: Tests — ClawConnector + RegistryIngestor
**Title:** Couverture de test pour les couches réseau et ingestor  
**Effort:** 2h  
**Priority:** P1  
**PRD Ref:** Audit recommendation

**Context:**  
Coverage actuel ~15%. `ClawConnector` à 0%, `RegistryIngestor` à 0%. L'audit identifie MockWebServer comme approche recommandée pour les tests réseau.

**Acceptance Criteria:**
- [ ] `ClawConnectorTest` : success 200, failure 500, timeout, auth header présent
- [ ] `RegistryIngestorTest` : `captureOnly()` retourne `Result.failure` si `!isReady`
- [ ] `RegistryIngestorTest` : `sendPacket()` appelle `CacheManager.storeForRetry()` sur failure
- [ ] Coverage réseau + ingestor : >60%

**Tasks:**
1. Créer `ClawConnectorTest.kt` avec MockWebServer
2. Créer `RegistryIngestorTest.kt` avec mocks
3. Vérifier `build.gradle.kts` contient MockWebServer + Mockito

---

## Definition of Done (Sprint 3)

- [ ] Aucune clé auth hardcodée dans le codebase
- [ ] WorkManager déclenché sur chaque `storeForRetry()`
- [ ] Endpoint configurable sans rebuild
- [ ] Glow différencié par état (3 couleurs)
- [ ] Long-press → tag → packet enrichi
- [ ] Coverage réseau + ingestor > 60%

---

## Ordre d'exécution recommandé

```
S3-01 (Keystore)        → fondation sécurité, bloque S3-02 et S3-03
S3-04 (Glow colors)     → indépendant, rapide
S3-02 (WorkManager)     → dépend S3-01
S3-03 (Endpoint config) → absorbe S3-01 (unification prefs)
S3-05 (Radial menu)     → feature autonome
S3-06 (Tests)           → en dernier, couvre le code final
```

---

*Created by BMAD — 2026-04-22*
