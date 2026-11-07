# Apply Progress: Firebase Cloud Persistence

Change: `firebase-cloud-persistence` · Store: `openspec` · Slice 1 of 12 (Phase S0) · Mode: Standard
Status: **COMPLETE** — S0-1..7 done. Firebase BoM pinned down to `34.12.0`; `assembleDebug` green twice and the 42-test gate passes.

## Slice boundary

Start: `77fdcbe` (`feat: implement local persistence with room and user authentication`), branch
`feature/firebase-cloud-persistence`. End: intended `:app:assembleDebug` ×2 + `test` green. The slice
did not reach its end state.

## What changed (working tree, uncommitted)

| File | Action | What was done |
|---|---|---|
| `gradle/libs.versions.toml` | Modified | Added `firebaseBom = "35.1.0"`, `googleServices = "4.5.0"`, `cloudinary = "3.1.3"`, `workManager = "2.12.0"`; libraries `firebase-bom`, `firebase-auth`, `firebase-firestore`, `cloudinary-android`, `androidx-work-runtime`, `androidx-work-testing`; plugin `google-services`. No `-ktx` artifacts. |
| `build.gradle.kts` | Modified | `alias(libs.plugins.google.services) apply false`. |
| `app/build.gradle.kts` | Modified | `google.services` applied last; `buildConfig = true`; `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_UPLOAD_PRESET` `buildConfigField`s read from `local.properties` through `providers.fileContents(...).asText` with an empty-string fallback; BoM + auth + firestore + work runtime + work testing + cloudinary dependency. |
| `app/src/main/AndroidManifest.xml` | Modified | Explicit `INTERNET`; `android:name=".SecurityInspectionsApp"`; non-exported `FileProvider` with `${applicationId}.fileprovider`. |
| `app/src/main/res/xml/file_paths.xml` | Created | `<files-path name="evidence" path="evidence/" />`. |
| `app/src/main/java/.../SecurityInspectionsApp.kt` | Created | `Application` owning `container`. |
| `app/src/main/java/.../AppContainer.kt` | Created | Skeleton exposing only `db`. |
| `local.properties` (untracked) | Modified | `CLOUDINARY_CLOUD_NAME=f2ge9jhj`, `CLOUDINARY_UPLOAD_PRESET=ml_default`. No API key or secret. |

Pre-existing uncommitted edits in `ui/screens/ChecklistModuleScreen.kt` and
`ui/viewmodel/InspectionViewModel.kt`, and the untracked `Diagrama Clases Arquitectura.md`, were left
untouched. `app/google-services.json` and `local.properties` were not staged.

## S0-1 version resolution (verified)

Source: `https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/maven-metadata.xml`
(`<release>2.12.0</release>`, no alpha/beta/rc newer).

| Artifact | Resolved | Proof |
|---|---|---|
| `androidx.work:work-runtime` | **2.12.0** (pinned) | `:app:dependencies --configuration debugRuntimeClasspath` → `+--- androidx.work:work-runtime:2.12.0`; zero `FAILED` entries |
| `com.google.firebase:firebase-bom` | 35.1.0 | `:app:dependencies --configuration releaseRuntimeClasspath` |
| `com.google.firebase:firebase-auth` | 25.0.0 (BoM-managed) | same report |
| `com.google.firebase:firebase-firestore` | 27.1.0 (BoM-managed) | same report |
| `com.cloudinary:cloudinary-android` | 3.1.3 | same report |
| `com.google.gms:google-services` | 4.5.0 | plugin applied and `:app:processDebugGoogleServices` executed successfully |

## Blocker (exact text)

```
> Task :app:compileDebugKotlin FAILED
e: file:///C:/Users/diego/.gradle/caches/9.6.0/transforms/628630723691a2a5ff90a59bdb1812ab/transformed/firebase-auth-25.0.0-api.jar!/META-INF/java.com.google.android.gmscore.integ.client.firebase-auth-api_firebase-auth-api.kotlin_module Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.4.0, expected version is 2.2.0.

FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:compileDebugKotlin' (registered by plugin 'com.android.internal.application').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details
BUILD FAILED in 4m 50s
```

Diagnosis: BoM 35.1.0 resolves `firebase-auth:25.0.0`, whose public API jar is compiled with Kotlin 2.4.0
metadata. The project's Kotlin Gradle plugin is `2.2.10` (`libs.versions.toml:9`), so the compiler refuses
the module. `kotlin-gradle-plugin` 2.4.x is available on Maven Central, but raising Kotlin also forces
`ksp` (`2.2.10-2.0.2`, compiler-coupled) and the Compose compiler plugin to move. Not decided here.

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused command and exact result | `:app:dependencies --configuration debugRuntimeClasspath` → BUILD SUCCESSFUL, `work-runtime:2.12.0`, 0 `FAILED`. `:app:dependencies --configuration releaseRuntimeClasspath` → BUILD SUCCESSFUL, BoM 35.1.0 / auth 25.0.0 / firestore 27.1.0 / cloudinary 3.1.3. |
| Runtime harness | `:app:assembleDebug` → **FAILED** at `:app:compileDebugKotlin` (text above). APK at `app/build/outputs/apk/debug/app-debug.apk` is stale (20/09/2026) and is **not** evidence for this change. |
| Rollback boundary | Revert the eight files above; `local.properties` is untracked and outside version control. Nothing else in the repo depends on them yet. |

## Resolution (2026-11-06)

Root cause narrowed to a single artifact. Kotlin metadata was read directly out of each published AAR
(`classes.jar` entries carrying `Lkotlin/Metadata;`, then `javap -v`):

| Artifact | Metadata | Readable by Kotlin 2.2.10 |
|---|---|---|
| `firebase-auth` 25.0.0 (BoM 35.1.0) | 2.4.0 | no |
| `firebase-auth` 24.2.0 / 24.1.0 (BoM 34.13.0-34.19.0) | 2.3.0 | no |
| `firebase-auth` 24.0.1 (BoM <= 34.12.0) | 2.1.0 | yes |
| `firebase-firestore` 27.1.0 | 2.1.0 | yes |
| `firebase-common` 23.0.0 | 2.1.0 | yes |

Raising Kotlin was rejected on evidence, not preference: KSP moved to independent versioning at `2.3.0`
and its latest release is `2.3.13`, so no KSP exists for Kotlin `2.4.x`; a Kotlin bump would break the
Room/KSP step. BoM `34.12.0` is the highest release still mapping `firebase-auth` 24.0.1 together with
`firebase-firestore` 26.2.0 and `firebase-common` 22.0.1.

| Evidence | Value |
|---|---|
| Focused command and exact result | `:app:dependencies --configuration debugRuntimeClasspath` -> `firebase-auth -> 24.0.1`, `firebase-firestore -> 26.2.0`, `firebase-common 22.0.1`, `work-runtime 2.12.0`, `cloudinary-android 3.1.3`; no `FAILED`. |
| Runtime harness | `:app:assembleDebug` -> **BUILD SUCCESSFUL in 2m 21s**; second run -> **BUILD SUCCESSFUL in 4s**, logged `Reusing configuration cache` and `Configuration cache entry reused.` |
| Test gate | `:app:testDebugUnitTest` -> BUILD SUCCESSFUL; `ExampleUnitTest` 1/1 and `InspectionViewModelTest` 41/41; total **42 tests, 0 failures, 0 errors, 0 skipped**. |
| APK | `app/build/outputs/apk/debug/app-debug.apk`, 34,515,245 bytes, 2026-11-06 19:03 - a fresh artifact, replacing the stale 20/09/2026 one. |

Nothing remains unproven for S0.

---

# Apply Progress: Slice 2 (S1a) — Domain Contracts

Change: `firebase-cloud-persistence` · Store: `openspec` · Slice 2a of 12 (PR 2 in the forecast) · Mode: Standard
Status: **COMPLETE** — S1-1, S1-2, S1-3 done. Purely additive; no existing behavior changed and the 42-test gate is untouched.

## Slice boundary

Start: `abba82c` (`build(s0): add Firebase, Cloudinary and WorkManager toolchain`), branch
`feature/firebase-cloud-persistence`. End: domain models, domain errors and repository interfaces
exist; `:app:assembleDebug` and `:app:testDebugUnitTest` green. Room v3, mappers, the repository impl
and all wiring stay in S1b (S1-4..S1-7).

## What changed

| File | Action | What was done |
|---|---|---|
| `main/domain/model/SyncState.kt` | Created | `LOCAL_ONLY`, `PENDING`, `SYNCED`, `FAILED`. |
| `main/domain/model/AuthSession.kt` | Created | `AuthSession(uid, email)`. |
| `main/domain/model/Evidence.kt` | Created | `EvidenceStatus` (`PENDING`, `UPLOADING`, `UPLOADED`, `FAILED`) and `Evidence(evidenceId, inspectionId, itemId, localRef, remoteUrl, status, attempts, lastError)`. |
| `main/domain/model/InspectionSession.kt` | Modified | `InspectionSessionRecord` gains `inspectionId`, `ownerUid`, `syncState`, `updatedAt`. |
| `main/domain/model/Incidence.kt` | Modified | `Incidence` gains `inspectionId`. |
| `main/domain/model/User.kt` | Modified | `uid: String = ""` appended. |
| `main/domain/error/AuthError.kt` | Created | `INVALID_CREDENTIALS`, `EMAIL_ALREADY_IN_USE`, `WEAK_PASSWORD`, `NETWORK_UNAVAILABLE`, `UNKNOWN`. |
| `main/domain/error/AuthException.kt` | Created | `class AuthException(val error: AuthError) : Exception(error.name)`. |
| `main/domain/error/SyncError.kt` | Created | `NETWORK`, `PERMISSION_DENIED`, `NOT_FOUND`, `INVALID_DATA`, `EXHAUSTED`, `UNKNOWN`. |
| `main/domain/error/EvidenceError.kt` | Created | `NETWORK`, `REJECTED`, `TOO_LARGE`, `STORAGE_IO`, `EXHAUSTED`, `UNKNOWN`. |
| `main/domain/repository/InspectionRepository.kt` | Created | Interface with the full design §2 surface (queries, saves, auth lookups, delete/retry tombstone, evidence subset). |
| `main/domain/repository/AuthRepository.kt` | Created | `session`, `signIn`, `register`, `signOut`, `observeProfile`, `cachedProfile`. |
| `main/domain/repository/EvidenceRepository.kt` | Created | `observe`, `register`, `retry`. |

## Deviations from design (§2 is authoritative)

1. **`inspectionId` carries a `""` default** in `InspectionSessionRecord` and `Incidence`. Design §7
   declares it without a default, but its only construction sites are `data/mapper/*` and
   `ui/viewmodel/InspectionViewModel.kt`, which are S1b files. A required field would break
   `compileDebugKotlin` in this slice. Defaults keep the change additive; S1b supplies the real UUID.
2. **`Evidence` fields are not enumerated by design §2.** The shape uses only identifiers design names
   elsewhere: `evidenceId`/`inspectionId`/`itemId` (design §8 deterministic id, public-id convention),
   `localRef` (`register(..., localRef)`), `remoteUrl` (design §8 "reference of record"), and
   `status`/`attempts`/`lastError` (design §6). `lastError` is typed `EvidenceError?`; only its `name`
   is persisted. No `updatedAt` column was invented — S1b/EVID may add one.
3. **No new tests in this slice.** `strict_tdd: true` is configured, but the assigned work is pure
   declaration (data classes, enums, interfaces) with no behavioral seam, and the slice boundary
   forbids `app/src/test/`. The RED/GREEN cycle applies from AUTH-1 onward where logic exists. The
   compile plus the unchanged 42-test suite is the verification for this unit.

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `:app:testDebugUnitTest` -> BUILD SUCCESSFUL; XML totals from `app/build/test-results/testDebugUnitTest/*.xml`: **42 tests, 0 failures, 0 errors, 0 skipped** (2 suites: `ExampleUnitTest` 1, `InspectionViewModelTest` 41). |
| Runtime harness command and exact result | `:app:assembleDebug` -> **BUILD SUCCESSFUL in 30s**, 7 executed / 33 up-to-date, `Configuration cache entry reused.` Kotlin compiled the new contracts; only pre-existing deprecation warnings. |
| Rollback boundary | Revert the six created and three modified files under `domain/`; `tasks.md` and this artifact revert with them. Nothing outside `domain/` references the new declarations yet, so rollback removes no unrelated work. |

## Remaining in PR 2

S1-4 (Room v3), S1-5 (mappers), S1-6 (impl rename + wiring) and S1-7 (42-test regression gate) are not
part of this batch. The 42-test gate currently still imports `data.repository.InspectionRepository`;
that single-line move belongs to S1-7.

---

## Slice S1b - Room v3 and repository extraction (S1-4..S1-7)

Status: **COMPLETE**. `assembleDebug` green and the full suite passes with the design's `suspend` DAO
surface intact.

### What changed

| Area | Files | What was done |
|---|---|---|
| Entities | `InspectionEntity.kt`, `IncidenceEntity.kt`, `EvidenceEntity.kt` (new), `ProfileEntity.kt` (new) | `inspectionId` is the `inspections` PK; sync columns (`ownerUid`, `syncState`, `updatedAt`, `deleted`, `lastSyncedAt`); `incidences.inspectionId` FK; new `evidence` upload-queue and `profiles` cache tables |
| DAOs | `InspectionDao.kt`, `IncidenceDao.kt`, `EvidenceDao.kt` (new), `ProfileDao.kt` (new) | `deleted = 0` filters, id/owner lookups, blocking `findBy...` reads, evidence queue access, profile read/write |
| Database | `AppDatabase.kt` | version **3**, five DAOs registered, `fallbackToDestructiveMigration()`, demo-user `RoomDatabase.Callback` and `Executors` seed removed |
| Mappers | `InspectionMapper.kt`, `IncidenceMapper.kt`, `ProfileMapper.kt` (new), `UserMapper.kt` (deleted) | map the new domain fields; credential-bearing user mapper replaced by a profile mapper |
| Repository | `data/repository/InspectionRepository.kt` -> `InspectionRepositoryImpl.kt` | implements `domain.repository.InspectionRepository`; sync disabled (writes stay `LOCAL_ONLY`/`PENDING`); `registerEvidence` is a block body so the override returns `Unit` |
| Wiring | `AppContainer.kt`, `MainActivity.kt`, `ViewModelFactory.kt` | construct/consume the impl behind the domain interface |
| ViewModel | `InspectionViewModel.kt` | six design section 7 identity points: interface import, `activeInspectionId`, rotate on `startNewSession`, `saveDictamen`, `submitAnswer`, `attachFindingEvidence` |
| Regression gate | `InspectionViewModelTest.kt` | only the import line changed to `domain.repository.InspectionRepository` |

### Room 2.6.1 cannot run under KSP2 (blocker and resolution)

The first verification run failed with `e: [ksp] java.lang.IllegalStateException: unexpected jvm
signature V`, and after that was worked around, with `EvidenceDao_Impl is not abstract and does not
override ... upsert(EvidenceEntity, Continuation<? super Long>)`.

Root cause is **not** project code. Room 2.6.1 emits JVM-invalid DAO implementations for any `suspend`
DAO method under KSP2 (`Continuation<Long>` where Kotlin requires `Continuation<? super Long>`), and
KSP2 reports the JVM signature of a `suspend`+`Unit` `@Insert` as `V`, which Room 2.6.1 cannot parse.
The failure surfaces through `ShortcutMethodProcessor.extractReturnType` and
`InsertionMethodProcessor`. This project never hit it before because no DAO had a `suspend` method;
S1b introduces nine. Upstream reference: google/ksp#2957; the documented fix is Room 2.7.0+.

Resolution (user-approved): Room bumped `2.6.1 -> 2.8.5` in `gradle/libs.versions.toml`, and
`app/build.gradle.kts` now consumes `libs.room.runtime`, `libs.room.compiler` and `libs.room.ktx`
instead of hardcoded `2.6.1` coordinates (the module previously ignored the version catalog entirely).
The DAO surface keeps `suspend` as design section 2/6 requires; no signature was weakened.

### S1a deviations resolved

`InspectionSession.inspectionId` and `Incidence.inspectionId` no longer carry a `""` default - both are
now required, and the ViewModel/mappers supply the real value, which removes deviation 1 from the S1a
section.

### Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test command and exact result | `:app:testDebugUnitTest` -> BUILD SUCCESSFUL; XML totals from `app/build/test-results/testDebugUnitTest/*.xml`: **42 tests, 0 failures, 0 errors, 0 skipped** (`ExampleUnitTest` 1, `InspectionViewModelTest` 41). |
| Runtime harness command and exact result | `:app:assembleDebug` -> **BUILD SUCCESSFUL in 1m 33s** after the Room 2.8.5 bump; `:app:kspDebugKotlin` green with the new DAOs. |
| Schema delta | `AppDatabase` reports `version = 3` with `InspectionEntity`, `IncidenceEntity`, `UserEntity`, `ProfileEntity`, `EvidenceEntity`; destructive migration accented by design (clean start). |
| Rollback boundary | Revert the S1b files (entities, DAOs, mappers, repository impl rename, wiring, ViewModel identity points) and the Room bump; `tasks.md`/this artifact revert with them. `ChecklistModuleScreen.kt` carries a pre-existing user edit and is not part of this slice. |

### Known follow-ups

- `EvidenceEntity` has no `updatedAt` column; EVID may add one.
- Room 2.8.x deprecates the no-arg `fallbackToDestructiveMigration()`; still functional and intentional
  for the clean start, revisit if migrations become real.


---

# Apply Progress: Slice 3 (AUTH-1..2) � Validator and error mapping

Change: `firebase-cloud-persistence` � Store: `openspec` � Slice 3 of 12 (PR 4 in the forecast) � Mode: Strict TDD
Status: **COMPLETE** � AUTH-1 and AUTH-2 are green. Purely additive: 4 new files, no existing source modified, 60/60 tests pass and `assembleDebug` succeeds.

## Slice boundary

Start: `a7aca1b` (`feat(s1b): migrate room to v3 and extract repository interface`), branch
`feature/firebase-cloud-persistence`. End: `AuthValidator` and the auth error-mapping seam exist and are
unit-tested; the data source can sign in/register/observe a session. `FirebaseAuthRepository`
(AUTH-3), the ViewModel/login screen (AUTH-4), session restore and navigation (AUTH-5) remain pending.

## What changed

| File | Action | What was done |
|---|---|---|
| `test/domain/validation/AuthValidatorTest.kt` | Created | 11 cases: malformed `"correo"`, email with spaces, valid email, 5/6/0-char password boundary, login-vs-registration asymmetry (`"pass"` passes login, same input fails registration). |
| `main/domain/validation/AuthValidator.kt` | Created | Pure object: `MIN_PASSWORD_LENGTH = 6`, `isValidEmail`, `validateLogin`, `validateRegistration`; returns `AuthError?` (null = sendable). No `android.*` imports. |
| `test/data/remote/auth/AuthErrorMapperTest.kt` | Created | 7 cases locking the full design section 9 table plus unmapped/null/blank -> `UNKNOWN`. |
| `main/data/remote/auth/AuthErrorMapper.kt` | Created | Pure `String? -> AuthError` mapper so the table is JVM-testable without constructing `FirebaseAuthException`. |
| `main/data/remote/auth/FirebaseAuthDataSource.kt` | Created | `FirebaseAuth` adapter: `session` `callbackFlow` over `addAuthStateListener`, `currentUid`, `signIn`, `register`, `signOut`, local `awaitTask()` via `suspendCancellableCoroutine`, Firebase failures translated to `AuthException`. |

## Deviations from design / task text

1. **`AuthErrorMapper.kt` is a new file separate from `FirebaseAuthDataSource.kt`.** AUTH-2 names
   `AuthErrorMapperTest.kt` as the test file, so the mapper is the test target; splitting it keeps the
   test free of any `FirebaseAuthDataSource` class load and satisfies design section 9's "raw SDK
   types/codes never leave `data/remote/auth`". `FirebaseAuthDataSource` delegates to it.
2. **The data source does not write `users/{uid}`.** Design section 9's registration sequence
   (create user -> write profile -> cache) is split: AUTH-2 owns the auth adapter, AUTH-3's
   `FirebaseAuthRepository` owns the Firestore profile write and local cache. `register(email, password)`
   therefore returns only the `AuthSession`; `name`/`cedula` never reach the auth SDK.
3. **Invalid email maps to `INVALID_CREDENTIALS`.** Design's `AuthError` enum has no `INVALID_EMAIL`
   member and the section 9 table already routes `ERROR_INVALID_EMAIL` to `INVALID_CREDENTIALS`;
   the validator uses the same domain error instead of inventing a code.
4. **`validateLogin` does not check password length.** Exactly as design section 2 requires
   (`login("test@seguridad.mx", "pass")` must keep succeeding); the 6-character rule is
   registration-only.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| AUTH-1 | `test/domain/validation/AuthValidatorTest.kt` | Unit (pure) | ? 42/42 baseline | ? `compileDebugUnitTestKotlin` failed: `Unresolved reference 'AuthValidator'` (12 sites) | ? `AuthValidatorTest` 11 tests, 0 failures | ? 11 cases across email, 5/6/0 boundary and register/login asymmetry | ? Regex/extracted `validateEmail`; tests stayed green |
| AUTH-2 | `test/data/remote/auth/AuthErrorMapperTest.kt` | Unit (pure) | ? 42/42 baseline + AUTH-1 green | ? `compileDebugUnitTestKotlin` failed: unresolved `AuthErrorMapper` | ? `AuthErrorMapperTest` 7 tests, 0 failures | ? Every row of the table plus unmapped/null/blank | ? `when` table is the final shape; data source delegates through one `awaitAuthTask` helper |

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test commands and exact result | AUTH-1: `:app:testDebugUnitTest --tests "...AuthValidatorTest"` -> BUILD SUCCESSFUL, XML 11 tests / 0 failures / 0 errors / 0 skipped. AUTH-2: `--tests "...AuthErrorMapperTest"` -> BUILD SUCCESSFUL, XML 7 tests / 0 failures / 0 errors / 0 skipped. |
| Runtime harness | No runtime boundary exists for these units: the validator is a pure object and the mapper is a pure string table. `:app:assembleDebug` -> **BUILD SUCCESSFUL in 9s** (9.8s wall), proving the Firebase-typed data source compiles against `firebase-auth` 24.0.1. |
| Full regression | `:app:testDebugUnitTest` -> BUILD SUCCESSFUL; XML totals from `app/build/test-results/testDebugUnitTest/*.xml`: `ExampleUnitTest` 1, `InspectionViewModelTest` 41, `AuthValidatorTest` 11, `AuthErrorMapperTest` 7 = **60 tests, 0 failures, 0 errors, 0 skipped**. |
| Rollback boundary | Delete the 5 created files listed above (2 main, 2 test, 1 mapper) and revert the AUTH-1/AUTH-2 checkboxes plus this section. Nothing existing imports them yet, so rollback removes no unrelated work. `ChecklistModuleScreen.kt` was not touched. |

## Remaining in Phase AUTH

AUTH-3 (`ProfileRemoteMapper` + `FirebaseAuthRepository`), AUTH-4 (ViewModel error mapping + `LoginScreen`
prefill removal), AUTH-5 (`restoreSession` + auth-aware navigation + `AppContainer.auth`).

---

# Apply Progress: Slice 4 (AUTH-3..5) - Profile mapper, auth repository, error mapping, session restore

Change: `firebase-cloud-persistence` - Store: `openspec` - Slice 4 of 12 (PR 5 in the forecast) - Mode: Strict TDD
Status: **COMPLETE** - AUTH-3, AUTH-4 and AUTH-5 are green. 71/71 unit tests pass and `assembleDebug` succeeds.

## Slice boundary

Start: `859a3c7` (`feat(auth): add auth validator and firebase auth data source`), branch
`feature/firebase-cloud-persistence`. End: a pure profile mapper, an `AuthRepository` over Firebase Auth +
the Room `profiles` cache, Spanish error mapping in the ViewModel, and an auth-aware navigation that restores
a session without network. SYNC, EVID, RULES and POL stay pending.

## What changed

| File | Action | What was done |
|---|---|---|
| `test/data/remote/mapper/ProfileRemoteMapperTest.kt` | Created | 4 cases: remote field names without a credential, exact round-trip, defaults for missing keys plus unknown keys ignored, `Number` -> `Long` for `createdAt`. |
| `main/data/remote/mapper/ProfileRemoteMapper.kt` | Created | Pure `object`: `toMap(User)` / `fromMap(uid, Map<String, Any?>)`. Keys `email`, `displayName`, `cedula`, `createdAt`; `updatedAt` is server-owned and never written. No Firebase types, no `org.json`, no reflection. |
| `main/data/repository/FirebaseAuthRepository.kt` | Created | `AuthRepository` over `FirebaseAuthDataSource` + `ProfileDao`: `session` is the provider `callbackFlow`, `signIn`/`register` map the session to a profile, `observeProfile`/`cachedProfile` read the `profiles` table, `ensureProfile` rebuilds a missing cache row from the session identity and writes the local cache (remote write is a non-fatal injected hook). |
| `test/ui/viewmodel/InspectionViewModelAuthTest.kt` | Created | 4 cases: `INVALID_CREDENTIALS` -> existing Spanish message, `EMAIL_ALREADY_IN_USE` -> existing Spanish message, `NETWORK_UNAVAILABLE` -> Spanish copy, and a sweep over every `AuthError` asserting no provider code reaches the UI. |
| `main/ui/viewmodel/InspectionViewModel.kt` | Modified | Catches `AuthException` before the generic handler and maps the enum to the existing Spanish copy (`authErrorMessage`); new `restoreSession` plus the shared `applyAuthenticatedUser` helper now used by `login`, `register` and `restoreSession`. |
| `main/ui/screens/LoginScreen.kt` | Modified | Demo prefill removed: the login email and password start empty. |
| `test/ui/viewmodel/InspectionViewModelSessionTest.kt` | Created | 3 cases: restore sets the user and observes the user inspections, restore replaces the global list with the user scope, restore mirrors the login success branch. |
| `main/MainActivity.kt` | Modified | Reads `AppContainer.auth`; collects the session flow and reacts: restored session -> `viewModel.restoreSession(profile)` -> `Screen.Dashboard` after the splash, lost session on a protected screen -> `Screen.Login`; logout also terminates the provider session. |
| `main/AppContainer.kt` | Modified | `auth: AuthRepository` wired with `FirebaseAuthDataSource` and `db.profileDao()`. |

Pre-existing uncommitted edits in `ui/screens/ChecklistModuleScreen.kt` and the untracked
`Diagrama Clases Arquitectura.md` / `app/google-services.json` were left untouched. No Gradle file was
changed, no dependency was added, and the Room schema, DAOs and entities were not modified.

## Deviations from design / task text

1. **`ProfileRemoteMapper.toMap` also writes `createdAt` (Long millis).** Design section 4 lists
   `email, displayName, cedula, updatedAt` for `users/{uid}`. `updatedAt` is server-owned (section 4) and is
   therefore never written, but `createdAt` is added so `fromMap(uid, toMap(user)) == user` holds for the whole
   domain model, as the section 5 round-trip contract requires. It is a plain number: no credential, no SDK type.
2. **`FirebaseAuthRepository` writes the local profile cache; the remote `users/{uid}` write is an injected
   hook.** Design section 9 assigns the profile write to this repository, but the Firestore data source belongs
   to SYNC-4. `remoteProfileWriter: suspend (User) -> Unit = {}` is the seam SYNC-4 fills; the repository owns
   the call and treats its failure as non-fatal, exactly as section 9 requires.
3. **`ensureProfile` keeps the cached display name and cedula.** A missing cache row is rebuilt from the
   session identity (`email`, empty `name`, empty `cedula`); an existing row is refreshed, not overwritten.
4. **New Spanish copy for the two errors that had none.** `WEAK_PASSWORD` -> "La contrase�a debe tener al menos
   6 caracteres." and `NETWORK_UNAVAILABLE` -> "Sin conexi�n a internet. Revisa tu red e int�ntalo de nuevo.".
   `INVALID_CREDENTIALS` and `EMAIL_ALREADY_IN_USE` reuse the messages the ViewModel already showed.
5. **`AuthValidator` is still not called by the ViewModel.** The "Domain-Layer Validation" requirement (invalid
   input must not trigger a request) is still open: AUTH-1 shipped the pure validator, but wiring it into
   `login`/`register` is not named by AUTH-3..5 and was left out of this slice.
6. **`FirebaseAuthRepository` has no JVM unit test.** `FirebaseAuthDataSource` is a final class bound to
   `FirebaseAuth`, so the repository has no JVM seam; design section 13 assigns Firebase Auth to the instrumented
   emulator phase (RULES-3/RULES-4). The mapper, which is the test target named by AUTH-3, is fully covered.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| AUTH-3 | `test/data/remote/mapper/ProfileRemoteMapperTest.kt` | Unit (pure) | 60/60 baseline | `compileDebugUnitTestKotlin` failed: `Unresolved reference 'ProfileRemoteMapper'` | 4 tests / 0 failures | field names, round-trip, missing keys, unknown keys, `Number` -> `Long` | key constants extracted; the mapper stayed a pure `object` |
| AUTH-4 | `test/ui/viewmodel/InspectionViewModelAuthTest.kt` | Unit (mocked repo) | 60/60 baseline + AUTH-3 green | 4 tests / 4 failed (`expected:<Credenciales incorrectas...> but was:<INVALID_CREDENTIALS>`) | 4 tests / 0 failures | 3 named errors + a sweep over every `AuthError` | `authErrorMessage(error, fallback)` extracted; login/register share it |
| AUTH-5 | `test/ui/viewmodel/InspectionViewModelSessionTest.kt` | Unit (mocked repo) | 60/60 baseline + AUTH-4 green | `compileDebugUnitTestKotlin` failed: `Unresolved reference 'restoreSession'` | 3 tests / 0 failures | user scope, global-scope replacement, login/restore parity | `applyAuthenticatedUser` shared by login, register and restore |

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused test commands and exact result | AUTH-3: `:app:testDebugUnitTest --tests "...ProfileRemoteMapperTest"` -> BUILD SUCCESSFUL, XML 4 tests / 0 failures / 0 errors / 0 skipped. AUTH-4: `--tests "...InspectionViewModelAuthTest"` -> BUILD SUCCESSFUL, XML 4 / 0 / 0 / 0. AUTH-5: `--tests "...InspectionViewModelSessionTest"` -> BUILD SUCCESSFUL, XML 3 / 0 / 0 / 0. |
| Runtime harness | No runtime boundary exists on the JVM for these units (the repository and the session flow are bound to the Firebase SDK and to Compose navigation). `:app:assembleDebug` -> **BUILD SUCCESSFUL in 22s**, compiling the Firebase-typed repository, the `AppContainer` wiring and the auth-aware navigation against `firebase-auth` 24.0.1. Device behaviour (offline relaunch, wrong password) is the RULES-3/RULES-4 emulator and manual gate. |
| Full regression | `:app:testDebugUnitTest` -> BUILD SUCCESSFUL; XML totals from `app/build/test-results/testDebugUnitTest/*.xml`: `ExampleUnitTest` 1, `InspectionViewModelTest` 41, `AuthValidatorTest` 11, `AuthErrorMapperTest` 7, `ProfileRemoteMapperTest` 4, `InspectionViewModelAuthTest` 4, `InspectionViewModelSessionTest` 3 = **71 tests, 0 failures, 0 errors, 0 skipped**. The 60 pre-existing tests stay green. |
| Rollback boundary | Delete `ProfileRemoteMapper.kt`, `FirebaseAuthRepository.kt` and the 3 new test files; revert `AppContainer.kt`, `MainActivity.kt`, `LoginScreen.kt` and `InspectionViewModel.kt` to `859a3c7`, plus the AUTH-3..5 checkboxes and this section. `ChecklistModuleScreen.kt` is not part of this slice. |
| Authored line count | 489 changed lines (140 modified + 349 new). The PR 5 forecast estimated ~210; the excess is the 3 test files AUTH-3..5 mandate (240 lines) plus the 2 new production files. The code was not compressed to fit the budget. |

## Cumulative task status

| Phase | Tasks | State |
|---|---|---|
| S0 | S0-1..S0-7 | complete |
| S1 | S1-1..S1-7 | complete |
| AUTH | AUTH-1..AUTH-5 | complete |
| SYNC | SYNC-1..SYNC-8 | pending |
| EVID | EVID-1..EVID-8 | pending |
| RULES | RULES-1..RULES-5 | pending |
| POL | POL-1..POL-3 | pending |

## Known follow-ups

- `AuthValidator` is not wired into the ViewModel yet (domain-layer validation requirement still open).
- `InspectionRepositoryImpl.login`/`registerUser` still read the legacy local `users` credential table; SYNC-7
  must delegate them to `AuthRepository` so the ViewModel's `AuthException` path is reached end to end.
- `FirebaseAuthRepository.remoteProfileWriter` is still the default no-op; SYNC-4 supplies the Firestore writer.
- The legacy `users` table and `UserMapper` are removed by POL-2.

---

# Apply Progress: Slice 5 (S2a) — Uncommitted SYNC work stabilised

Change: `firebase-cloud-persistence` - Store: `openspec` - Slice 5 of 12 - Mode: Strict TDD
Status: **STABILISED** - the working tree compiles again and the full suite is green. The
uncommitted SYNC code (SYNC-3..SYNC-7 groundwork) still has **no tests** for its behavioral seams.

## Slice boundary

Start: `2eca311` (`feat(sync): add Firestore remote mappers and conflict policy`), branch
`feature/firebase-cloud-persistence`. End: `:app:assembleDebug` and `:app:testDebugUnitTest` green
over the uncommitted SYNC groundwork, with the exact baseline recorded.

## Blocker found and fixed

The checked-out working tree did not compile:

```
> Task :app:kspDebugKotlin FAILED
e: [ksp] [MissingType]: Element '...data.local.dao.IncidenceDao' references a type that is not present
```

Root cause: `IncidenceDao.kt` declared `@Update fun updateInspection(...)` without importing
`androidx.room.Update`. KSP could not resolve the annotation type and aborted before Kotlin compiled.

Fix: added the missing `import androidx.room.Update` (one line, no behavior change).

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused command and exact result | `Runner :app:assembleDebug` -> **BUILD SUCCESSFUL in 2m 8s** (51 tasks, 15 executed), only pre-existing deprecation warnings. |
| Test gate | `Runner :app:testDebugUnitTest` -> BUILD SUCCESSFUL; XML totals from `app/build/test-results/testDebugUnitTest/*.xml`: **93 tests, 0 failures, 0 errors, 0 skipped** across 10 suites (`InspectionViewModelTest` 41, `AuthValidatorTest` 11, `InspectionRemoteMapperTest` 8, `SyncConflictPolicyTest` 8, `AuthErrorMapperTest` 7, `IncidenceRemoteMapperTest` 6, `ProfileRemoteMapperTest` 4, `InspectionViewModelAuthTest` 4, `InspectionViewModelSessionTest` 3, `ExampleUnitTest` 1). |
| Rollback boundary | Revert the single import line; the previously recorded 71-test baseline returns. |

## Gaps confirmed in the uncommitted SYNC groundwork

1. `CloudMirror` is never constructed in `AppContainer` - no Firestore -> Room download path exists.
2. Incidences are never marked `PENDING` on save, so the worker only ever pushes the parent document.
3. `SyncConflictPolicy` lives inside `domain/validation/AuthValidator.kt` instead of its own file
   (its test already targets the object independently).
4. `InspectionSyncWorker` imports `ListenableWorker`, `CoroutineScope`, `SupervisorJob`, `launch`,
   `InspectionDao` and `RemoteInspectionDataSource` without using them.
5. No tests exist for `SyncCoordinator`, `SyncScheduler`, `CloudMirror`, `FirestoreInspectionDataSource`
   or `InspectionSyncWorker`, although `strict_tdd: true` is active for this change.
6. `firestore.rules`, `firestore.indexes.json` and `firebase.json` do not exist yet (RULES-1).

## Product decisions recorded

- Evidence images go to **Cloudinary** (unsigned preset `ml_default`), not Firebase Storage: the
  Firebase project is on the Spark plan and the instructor authorised Cloudinary explicitly.
  Firebase remains the infrastructure for authentication (Auth) and information (Firestore).
