# Tasks: Firebase Cloud Persistence

Change: `firebase-cloud-persistence` · Store: `openspec` · Strict TDD · Review budget: 400 changed lines

**Path convention** (all Kotlin paths below are relative to these roots):
`main/` = `app/src/main/java/com/example/dam_t1_evaluacion_practica/`
`test/` = `app/src/test/java/com/example/dam_t1_evaluacion_practica/`
`androidTest/` = `app/src/androidTest/java/com/example/dam_t1_evaluacion_practica/`

**Runner** (from repo root): `java -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain <tasks>`

**Existing gate**: 42 tests (41 in `test/ui/viewmodel/InspectionViewModelTest.kt` + `ExampleUnitTest`) are green today and MUST stay green.

**Out of scope** (do not add): Firebase Storage, Google Sign-In, anonymous auth, scoring/Critical Stop/85%–24h changes, PDF redesign, admin/backoffice, legacy migration.

## Phase S0 — Foundation, version pinning, build proof

- [x] S0-1 Resolve and pin the UNVERIFIED `androidx.work` versions: read `https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/maven-metadata.xml`, pick latest stable, prove with `Runner :app:dependencies --configuration debugRuntimeClasspath` (prints the pinned `work-runtime`, no `FAILED`); confirm BoM `34.12.0`, plugin `4.5.0`, `cloudinary-android` `3.1.3` with `Runner :app:dependencies --configuration releaseRuntimeClasspath`. BoM was pinned down from `35.1.0` because `firebase-auth` 24.1.0+ ships Kotlin metadata newer than this project's Kotlin 2.2.10.
- [x] S0-2 Add `firebaseBom`, `googleServices`, `cloudinary`, the resolved `workManager` versions and libraries `firebase-bom`, `firebase-auth`, `firebase-firestore`, `cloudinary-android`, `androidx-work-runtime`, `androidx-work-testing`, plus plugin `google-services`, to `gradle/libs.versions.toml` (no `-ktx`).
- [x] S0-3 Apply `libs.plugins.google.services` with `apply false` in `build.gradle.kts`.
- [x] S0-4 In `app/build.gradle.kts`: place `google.services` last, set `buildConfig = true`, add BoM + auth/firestore (no `-ktx`) + work runtime + testing, and inject `CLOUDINARY_CLOUD_NAME`/`CLOUDINARY_UPLOAD_PRESET` `buildConfigField`s read from `local.properties` via `providers.fileContents(...).asText`.
- [x] S0-5 Add explicit `INTERNET`, `android:name=".SecurityInspectionsApp"` and a non-exported `FileProvider` (`${applicationId}.fileprovider`) to `app/src/main/AndroidManifest.xml`; create `app/src/main/res/xml/file_paths.xml` with `files-path name="evidence" path="evidence/"`.
- [x] S0-6 Create `main/SecurityInspectionsApp.kt` (Application owns `container`) and `main/AppContainer.kt` skeleton exposing only `db`; add `CLOUDINARY_CLOUD_NAME=f2ge9jhj` and `CLOUDINARY_UPLOAD_PRESET=ml_default` to `local.properties` (untracked).
- [x] S0-7 Build proof before any feature code: `Runner :app:assembleDebug` twice (second logs `Reusing configuration cache`) and `Runner test` reports 42 passing; confirm the APK at `app/build/outputs/apk/debug/app-debug.apk` (read-only).

## Phase S1 — Domain contracts, Room v3, repository interface extraction

- [x] S1-1 Create `main/domain/model/Evidence.kt`, `SyncState.kt`, `AuthSession.kt`; add `inspectionId`/`ownerUid`/`syncState`/`updatedAt` to `main/domain/model/InspectionSession.kt`, `inspectionId` to `Incidence.kt`, `uid` to `User.kt`.
- [x] S1-2 Create `main/domain/error/AuthError.kt`, `AuthException.kt`, `SyncError.kt`, `EvidenceError.kt` with the exact enums from design §2.
- [x] S1-3 Create interfaces `main/domain/repository/InspectionRepository.kt` (full design §2 surface), `AuthRepository.kt`, `EvidenceRepository.kt`.
- [x] S1-4 Room v3: modify `main/data/local/entity/InspectionEntity.kt` (PK `inspectionId`, sync columns) and `IncidenceEntity.kt` (`inspectionId`); update `main/data/local/dao/InspectionDao.kt`/`IncidenceDao.kt` (`deleted = 0`, id/owner lookups); create `ProfileEntity.kt`, `ProfileDao.kt`, `EvidenceEntity.kt`, `EvidenceDao.kt`; set `main/data/local/AppDatabase.kt` to version 3 with the new entities and delete the seeded demo user.
- [x] S1-5 Update `main/data/mapper/InspectionMapper.kt`/`IncidenceMapper.kt` for the new fields; replace `UserMapper.kt` with `ProfileMapper.kt`.
- [x] S1-6 Rename `main/data/repository/InspectionRepository.kt` to `InspectionRepositoryImpl.kt` implementing the interface with sync disabled; update `main/AppContainer.kt`, `main/MainActivity.kt`, `main/ui/viewmodel/ViewModelFactory.kt`.
- [x] S1-7 Regression gate (requirement #6): change only the import in `test/ui/viewmodel/InspectionViewModelTest.kt` to `domain.repository.InspectionRepository`; prove `Runner test` keeps all 42 tests green before any behavior change.

## Phase AUTH — Firebase Email/Password

- [x] AUTH-1 Write the failing test first in `test/domain/validation/AuthValidatorTest.kt` (invalid `"correo"`, 6-char boundary, register-vs-login asymmetry); then implement `main/domain/validation/AuthValidator.kt` until green.
- [x] AUTH-2 Write the failing test first in `test/data/remote/auth/AuthErrorMapperTest.kt` (full `errorCode` → `AuthError` table, unmapped → `UNKNOWN`); then implement `main/data/remote/auth/FirebaseAuthDataSource.kt` with a local `awaitTask()` bridge until green.
- [x] AUTH-3 Write the failing test first in `test/data/remote/mapper/ProfileRemoteMapperTest.kt` (round-trip, no credential key); then implement `main/data/remote/mapper/ProfileRemoteMapper.kt` and `main/data/repository/FirebaseAuthRepository.kt` (`session` callbackFlow, `cachedProfile`, `ensureProfile`).
- [x] AUTH-4 Write the failing test first in `test/ui/viewmodel/InspectionViewModelAuthTest.kt` (`AuthException` maps to the existing Spanish message, no SDK code); then update `main/ui/viewmodel/InspectionViewModel.kt` and remove the demo prefill in `main/ui/screens/LoginScreen.kt`.
- [x] AUTH-5 Write the failing test first in `test/ui/viewmodel/InspectionViewModelSessionTest.kt` (restore mirrors `login`: user set, inspections observed); then add `restoreSession` in `main/ui/viewmodel/InspectionViewModel.kt` and auth-aware navigation in `main/MainActivity.kt`; fill `AppContainer.auth`.

## Phase SYNC — Firestore replica

- [x] SYNC-1 Write the failing tests first in `test/data/remote/mapper/InspectionRemoteMapperTest.kt` and `IncidenceRemoteMapperTest.kt` (round-trip, omitted nulls, unknown keys/enums, evidence URL substitution, illegal map keys dropped); then implement `main/data/remote/mapper/InspectionRemoteMapper.kt`/`IncidenceRemoteMapper.kt` (pure maps, no `org.json`/Firebase types).
- [x] SYNC-2 Write the failing test first in `test/domain/validation/SyncConflictPolicyTest.kt` (remote wins only when `SYNCED` and newer; dirty ignores remote); then implement `main/domain/validation/SyncConflictPolicy.kt`.
- [x] SYNC-3 Write the failing test first in `test/data/sync/SyncCoordinatorTest.kt` with `test/data/sync/SyncFakes.kt` (LOCAL_ONLY→PENDING→SYNCED, FAILED terminal, tombstone, retry); then implement `main/data/sync/SyncCoordinator.kt` and `SyncScheduler.kt` (interface + WorkManager impl, unique work, CONNECTED, 30 s backoff, 5 attempts).
- [ ] SYNC-4 Write the failing test first in `test/data/remote/firestore/FirestoreWritePlanTest.kt` (ownerUid stamped from session, `updatedAt` server-owned, incidence chunks ≤8); then create `main/data/remote/firestore/RemoteInspectionDataSource.kt` and `FirestoreInspectionDataSource.kt`.
- [x] SYNC-5 Write the failing test first in `test/data/sync/CloudMirrorTest.kt` (apply only when clean and newer; `REMOVED` deletes only `SYNCED`); then implement `main/data/sync/CloudMirror.kt` and wire it in `AppContainer`.
- [ ] SYNC-6 Write the failing test first in `test/data/sync/InspectionSyncWorkerTest.kt` (parent before incidences, non-retryable → FAILED, transient → retry, exhaustion → FAILED); then implement `main/data/sync/InspectionSyncWorker.kt` (already thin glue; worker logic lives in `SyncCoordinator`).
- [ ] SYNC-7 Complete `main/data/repository/InspectionRepositoryImpl.kt` (PENDING on save/update, delete tombstones, `retrySync`, evidence delegation) and wire `main/AppContainer.kt`.
- [ ] SYNC-8 Write the failing test first in `test/ui/viewmodel/InspectionViewModelSyncTest.kt` (`inspectionId` UUID stable per session, `retrySync` delegates); then update `main/ui/viewmodel/InspectionViewModel.kt` and add the sync chip + retry to `main/ui/screens/HistorialScreen.kt` and `InspectionDetailScreen.kt`.

## Phase EVID — Capture, Cloudinary, PDF

- [ ] EVID-1 Write the failing test first in `test/domain/validation/EvidenceBudgetTest.kt` (2048 px, q≥70, 2 MiB downsample ladder, never upscale); then implement `main/domain/validation/EvidenceBudget.kt`.
- [ ] EVID-2 Write the failing test first in `test/domain/model/EvidenceIdTest.kt` (deterministic `nameUUIDFromBytes("$inspectionId|$itemId")`, stable across retries); then implement `main/domain/model/Evidence.kt` (`Evidence`, `EvidenceStatus`, id helper).
- [ ] EVID-3 Write the failing test first in `test/data/repository/DefaultEvidenceRepositoryTest.kt` (register PENDING, unique enqueue, `UPLOADED` persists `secureUrl`); then implement `main/data/repository/DefaultEvidenceRepository.kt`.
- [ ] EVID-4 No JVM seam (camera/FileProvider; design §13 forbids Robolectric): create `main/util/EvidenceStorage.kt` and `main/util/EvidencePreparer.kt`, then replace `TakePicturePreview()` with `TakePicture()` in `main/ui/screens/ChecklistModuleScreen.kt`, `ChecklistScreen.kt`, `CriticalStopDialog.kt`; harness is RULES-5.
- [ ] EVID-5 Write the failing test first in `test/data/remote/cloudinary/CloudinaryEvidenceUploaderTest.kt` (public id `inspections/{inspectionId}/{evidenceId}`, no secret); then implement `main/data/remote/cloudinary/CloudinaryEvidenceUploader.kt` and `main/data/sync/EvidenceUploadWorker.kt` (unique, CONNECTED, 5 attempts, enqueue parent sync); init `MediaManager` in `main/SecurityInspectionsApp.kt`.
- [ ] EVID-6 No JVM seam (Compose + bitmap decode): create `main/util/RemoteImageLoader.kt` and `main/ui/components/EvidenceImage.kt`, then use them in `main/ui/screens/ResultsScreen.kt`, `InspectionDetailScreen.kt`, `SummaryScreen.kt`; harness is RULES-5.
- [ ] EVID-7 PDF remote-URL support (requirement #7): write the failing test first in `test/util/EvidenceReferenceTest.kt` (file/`content://`/`https` classified, unresolvable → UNAVAILABLE); then make `main/util/InspectionPdfGenerator.kt` `suspend` on `Dispatchers.IO` (10 s timeout, 2 MiB cap, "Evidencia no disponible" marker) and update the call site in `main/ui/screens/InspectionDetailScreen.kt`.
- [ ] EVID-8 Write the failing test first in `test/ui/viewmodel/InspectionViewModelEvidenceTest.kt` (evidence registered without blocking `saveDictamen`, PENDING/FAILED exposed); then update `main/ui/viewmodel/InspectionViewModel.kt` and pending/failed/retry UI in `main/ui/screens/ResultsScreen.kt`.

## Phase RULES — Emulator Suite, rules, indexes

- [ ] RULES-1 Create `firestore.rules` (design §12 verbatim), `firestore.indexes.json` (ownerUid ASC + updatedAt DESC) and `firebase.json` wiring rules/indexes plus the Auth and Firestore emulators.
- [ ] RULES-2 Emulator setup: add the androidTest emulator dependencies to `app/build.gradle.kts` and create `androidTest/FirebaseEmulatorRule.kt` (points Auth/Firestore at `10.0.2.2`).
- [ ] RULES-3 Write the failing test first in `androidTest/FirestoreRulesTest.kt` (owner read/create/update; cross-uid denied; unknown parent denied; `overallScore` outside 0..100 denied; incidence requires parent owner); run against the emulator until green.
- [ ] RULES-4 Write the failing test first in `androidTest/AuthEmulatorTest.kt` (register, wrong password → `INVALID_CREDENTIALS`, duplicate → `EMAIL_ALREADY_IN_USE`); prove with `Runner :app:connectedDebugAndroidTest`.
- [ ] RULES-5 Assert the merged manifest declares `INTERNET` at `app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml` (read-only) and record the two-device, offline-photo, offline-relaunch and capture-compression manual results in the PR description.

## Phase POL — Polish and cleanup

- [ ] POL-1 Cloudinary preset restriction checklist (console, manual, NOT code): unsigned `ml_default` limited to ~5 MiB, jpg/png only, no delete token, no API secret in `local.properties` or APK.
- [ ] POL-2 Delete superseded `main/data/local/entity/UserEntity.kt`, `main/data/local/dao/UserDao.kt` and remaining `UserMapper` references; remove the demo-user seed path from `main/data/local/SampleData.kt`.
- [ ] POL-3 Final gate: `Runner test`, `Runner :app:assembleDebug`, `Runner :app:lint`; confirm the 42 legacy tests plus all new tests pass and inspect `app/build/reports/lint-results-debug.html` (read-only).

## Review Workload Forecast

| Phase | Est. changed lines (added + removed) |
|---|---|
| S0 foundation | 90 |
| S1 contracts + Room v3 + extraction | 560 |
| AUTH | 450 |
| SYNC | 810 |
| EVID | 620 |
| RULES / emulator | 250 |
| POL / cleanup | 40 |
| **Total** | **~2,820 (range 2,800–3,000)** |

- **400-line budget risk: High** — ~7× the 400-line review budget; 40 tasks across 7 phases, 3 new capabilities, a Room v2→v3 wipe and two WorkManager workers.
- **Chained PRs recommended: Yes** — no single slice below is mergeable as one reviewable PR.
- **Decision needed before apply: Yes** — `ask-on-risk` delivery plus a High forecast requires the chain strategy to be chosen before `sdd-apply`.

```text
Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High
```

### Suggested PR slices (each ≤400 lines; base = previous slice)

| PR | Content | Est. lines | Base boundary |
|---|---|---|---|
| 1 | S0-1..7 foundation and build proof | 90 | main |
| 2 | S1-1..3, S1-5 contracts, errors, interfaces, local mappers | 260 | PR 1 |
| 3 | S1-4, S1-6, S1-7 Room v3, impl rename, 42-test gate | 300 | PR 2 |
| 4 | AUTH-1..2 validators, error mapping, auth data source | 240 | PR 3 |
| 5 | AUTH-3..5 auth repository, VM/login, session restore | 210 | PR 4 |
| 6 | SYNC-1..2 remote mappers and conflict policy | 300 | PR 5 |
| 7 | SYNC-3, SYNC-5, SYNC-6 coordinator, mirror, sync worker | 390 | PR 6 |
| 8 | SYNC-4, SYNC-7, SYNC-8 data source, repo wiring, VM/UI chip | 220 | PR 7 |
| 9 | EVID-1..3 budget, evidence id, evidence repository | 250 | PR 8 |
| 10 | EVID-4..5 capture, compression, Cloudinary uploader/worker | 300 | PR 9 |
| 11 | EVID-6..8 preview, PDF remote URLs, VM evidence states | 170 | PR 10 |
| 12 | RULES-1..5 and POL-1..3 emulator, rules, cleanup | 290 | PR 11 |
