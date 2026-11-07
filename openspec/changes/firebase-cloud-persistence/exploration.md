# Exploration: Firebase Cloud Persistence

Change: `firebase-cloud-persistence`
Mode: read-only exploration. No design, spec, or task decisions are finalized here; every option is compared with tradeoffs for the proposal phase.
Related but explicitly out of scope: `openspec/changes/compliance-score-reinspection/` (8/23 tasks).

Evidence basis: source files cited by path and line, the current Gradle model, and Firebase/Android documentation checked on 2026-11-06. Anything not directly observed is marked **unverified**.

## Current State

### 1.1 Persistence path (ViewModel -> repository -> Room)

- `MainActivity.onCreate` builds the whole graph by hand: `AppDatabase.getDatabase`, then
  `InspectionRepository(database.inspectionDao(), database.incidenceDao(), database.userDao())`,
  then `InspectionViewModelFactory` -> `ViewModelProvider` (`app/src/main/java/.../MainActivity.kt:62-65`).
  No Hilt/Koin, no service locator, no repository interface.
- `InspectionViewModel(repository: InspectionRepository)` depends on the **concrete** class
  (`app/src/main/java/.../ui/viewmodel/InspectionViewModel.kt:93-95`).
- `InspectionRepository` is a concrete final class over three DAOs; every write wraps
  `withContext(Dispatchers.IO)` (`app/src/main/java/.../data/repository/InspectionRepository.kt:16-20`, `:40-46`, `:54-56`).
- DAO writes are **non-suspend** (`InspectionDao.kt:23-30`, `IncidenceDao.kt:15-17`, `UserDao.kt:18-19`);
  the repository is what makes them IO-safe.
- Room database: `inspection_database`, **version 2**, `exportSchema = false`,
  `fallbackToDestructiveMigration()` and **no Migration object** (`data/local/AppDatabase.kt:16-24`, `:41`).
- On first creation the DB seeds a demo account `inspector@seguridad.mx` / `password123`
  (`AppDatabase.kt:42-56`).
- Save of a finished inspection: `saveDictamen()` builds `InspectionSessionRecord` with a
  **client-generated folio**, immediately merges it into `uiState.savedInspections` for a responsive UI,
  then fires `repository.saveInspection(record)` in `viewModelScope` with no retry
  (`InspectionViewModel.kt:490-533`).
- Folio generation is local-count based: `existing.count { folio.contains("$year-") } + 1`
  (`InspectionViewModel.kt:550-554`). **Two devices offline in the same year will generate the same
  folio** (see Risks).
- Incidences are persisted **per answer, before any inspection/folio exists**:
  `submitAnswer` -> `repository.saveIncidence(Incidence(id = UUID, ...))`
  (`InspectionViewModel.kt:354-374`), and again on evidence attach (`:377-412`).
  The `Incidence` domain model has **no `inspectionId`** (`domain/model/Inspection.kt` is unused legacy;
  actual model is `domain/model/Incidence.kt:7-16`). Inspections and incidences are therefore only
  loosely related today.
- Read path: `InspectionViewModel` exposes a single `StateFlow<InspectionUiState>`; all screens read
  `uiState.savedInspections` (`HistorialScreen.kt:108`, `DashboardScreen.kt:63-64`, `:300-303`,
  `ProfileScreen.kt:69-72`, and the detail refresh in `MainActivity.kt:215-227`). There is **no per-screen repository
  call** - the VM is the only read surface.
- When no user is logged in the VM observes **all** inspections
  (`InspectionRepository.kt:22-26` -> `InspectionDao.kt:14-15`; gated in
  `InspectionViewModel.kt:110-120`); after login it observes only the user's rows
  (`InspectionViewModel.kt:122-129`, `InspectionDao.kt:17-18`). On a shared device this "all rows"
  window is a data-exposure risk that cloud rules must close.

### 1.2 Photo / evidence flow (the core breakage)

- All three capture sites use `ActivityResultContracts.TakePicturePreview()` (a **low-resolution
  preview bitmap**, not a full camera capture):
  `ChecklistModuleScreen.kt:562-579`, `ChecklistScreen.kt:259-264`, `CriticalStopDialog.kt:92-111`.
- The bitmap is written to the **device gallery** through `MediaStore`:
  `GalleryUtils.saveBitmapToGallery` inserts into `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`
  under `Pictures/InspeccionesSeguridad`, JPEG quality 90 (`util/GalleryUtils.kt:12-39`, specifically
  `:21`, `:26`, `:30`).
- The returned `content://media/external/images/media/<id>` string is stored as `photoUri`:
  - inside the `findingsJson` Room blob (`data/mapper/InspectionMapper.kt:84-134`, field `photoUri`);
  - in the `incidences.photoUri` column (`IncidenceEntity.kt:14`).
- Display/detail/PDF all parse that string and open it through `ContentResolver`
  (`ChecklistModuleScreen.kt:557-559`, `ResultsScreen.kt:462`, `InspectionDetailScreen.kt:346-356`, `:712`,
  `SummaryScreen.kt:212`, `InspectionPdfGenerator.kt:200-212`).
- **Consequence:** the URI is device-local. It cannot resolve on another device and is not part of the
  document payload. Cloud persistence of the record without moving the bytes would produce inspections
  whose evidence silently disappears on every other device - and the PDF generator would silently skip
  the photo (`InspectionPdfGenerator.kt:203-226` catches and ignores failures).
- Capture quality is already a limitation: `TakePicturePreview` returns a thumbnail-sized bitmap, so any
  cloud copy will be low-resolution unless capture itself is changed.
- `INTERNET` is **not declared** in `app/src/main/AndroidManifest.xml:5-11`; it appears in the merged
  manifest only because `com.google.android.gms:play-services-maps:18.2.0` contributes it
  (`app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`,
  `app/build/outputs/logs/manifest-merger-debug-report.txt`: `uses-permission#android.permission.INTERNET
  ADDED from [com.google.android.gms:play-services-maps:18.2.0]`). Relying on a transitive permission for
  Firebase is fragile and must be fixed explicitly.

### 1.3 Auth flow

- `login(email, password)` -> `repository.login` -> `UserDao.login` with a **plaintext SQL comparison**
  (`UserDao.kt:15-16`; `UserEntity.kt:7-13` stores `password: String` verbatim). On success the VM stores
  `currentUser`, resets the list and switches the observations (`InspectionViewModel.kt:131-168`).
- `register(...)` first calls `repository.getUserByEmail` (local duplicate check), then
  `repository.registerUser(newUser, password)` (`InspectionViewModel.kt:170-225`;
  `InspectionRepository.kt:58-68`). Blank-field validation lives in the VM (`:183-188`).
- `logout` clears all session state in the VM only; nothing is invalidated server-side
  (`InspectionViewModel.kt:227-243`).
- Session is **not restored**: the app always navigates Splash -> Login (`MainActivity.kt:84-99`).
- The `users` table stores `email` (PK), `name`, `password`, `cedula`, `createdAt`
  (`UserEntity.kt:7-13`); Firebase Auth stores none of `name`/`cedula`, so a profile store is needed.

### 1.4 Domain / data separation today

- `domain/model/*` is clean: scanning `domain/` for `android`, `androidx`, `com.google`, `org.json`
  imports returns **zero** hits. Domain is already Room-free and Firebase-free.
- Mapping lives in `data/mapper` as extension functions (`toEntity()` / `toDomainModel()`), which matches
  the user's "Mappers" requirement.
- However `InspectionMapper` uses Android's `org.json` (`InspectionMapper.kt:11-12`), including for
  nested modules/answers/items (`:136-172`) and findings (`:84-134`). `org.json` is stubbed in JVM unit
  tests, so those mappers are **not unit-testable** today without Robolectric (there is no
  `testOptions.unitTests.returnDefaultValues` in `app/build.gradle.kts`). The remote mapper must not copy
  this dependency if mapper tests are required.
- UI imports a **data** singleton: `MainActivity.kt:20` imports `data.local.SampleData`, and
  `itemsFor()` maps modules to `SampleData.eppItems/machineryItems/environmentItems`
  (`MainActivity.kt:248-252`). This is existing layering drift; it is not caused by Firebase but it
  blocks any clean "domain owns the catalog" story.

### 1.5 Test suite (must stay green)

- 41 `@Test` methods in `InspectionViewModelTest.kt` (verified by count) + 1 template = 42.
- They instantiate `InspectionViewModel(mockk<InspectionRepository>(relaxed = true))`
  (`InspectionViewModelTest.kt:32-38`) and stub `repository.login`, `getUserByEmail`, `registerUser`,
  `getInspectionsByUser` (`:637-638`, `:656`, `:673-675`, `:707`, `:726-727`).
- **Because everything is mocked at the repository boundary, the tests survive any remote
  implementation as long as `InspectionViewModel(repository)` and those method signatures/`Flow` types
  are preserved.** Moving the interface to `domain/repository` requires only an import change in the test
  file; assertions stay identical.
- No DAO, mapper, or repository tests; no Robolectric; no coverage tooling
  (`app/build.gradle.kts:59-61`, `openspec/config.yaml:27-30`).
- Reliable runner on this Windows host: `java -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain test`.

## Affected Areas

| Area | Files | Impact |
|---|---|---|
| Build | `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` | google-services plugin, Firebase BOM, auth/firestore/storage, WorkManager (optional image loader) |
| Build config | `app/google-services.json` (absent), `.gitignore` | New config file; `.gitignore` does not currently exclude it |
| Manifest | `app/src/main/AndroidManifest.xml` | Explicit `INTERNET`; possibly `POST_NOTIFICATIONS` if sync UX needs it |
| Domain | `domain/repository/*` (new interfaces), `domain/model/Incidence.kt` (optional `inspectionId`), `domain/validation/*` (new) | Interfaces belong in domain; models stay Firebase-free |
| Data (new) | `data/remote/entity|dto`, `data/remote/mapper`, `data/remote/datasource` | Firestore DTOs, explicit mappers, Firestore/Auth/Storage data sources |
| Data (existing) | `data/repository/InspectionRepository.kt`, `data/local/*` | Interface extraction, sync columns, Room v3, write-through/mirror logic |
| VM | `ui/viewmodel/InspectionViewModel.kt` | Auth calls delegate to Firebase; persistence gains remote; scoring/critical-stop rules stay |
| UI | `MainActivity.kt`, capture sites (`ChecklistModuleScreen`, `ChecklistScreen`, `CriticalStopDialog`), photo consumers (`ResultsScreen`, `InspectionDetailScreen`, `SummaryScreen`), `util/GalleryUtils.kt`, `util/InspectionPdfGenerator.kt` | Local-vs-remote photo resolution; possibly full-res capture |
| Tests | `InspectionViewModelTest.kt` (import only), new mapper/auth/sync tests | 42 existing must remain green |

## Target Architecture for Firebase (options, not final design)

### 2.1 Layer map

```
domain/
  model/                 (unchanged: Normativ, ChecklistItem, Incidence, InspectionSessionRecord, User)
  repository/            (NEW interfaces: InspectionRepository, AuthRepository, EvidenceRepository)
  validation/            (NEW pure functions: auth field rules, ownership/authorization rules)
data/
  local/                 (Room: cache only)
  remote/entity/         (NEW Firestore/Auth DTOs - Firestore-shaped, never seen by domain/UI)
  remote/mapper/         (NEW explicit DTO <-> domain mappers; Room entity <-> domain mappers stay)
  remote/datasource/     (NEW FirestoreDataSource, AuthDataSource, StorageDataSource)
  repository/            (implementations: Room + remote orchestration)
ui/viewmodel/            (same single VM; calls interfaces)
```

Naming caution: the user's wording "Firebase entities" collides with Room `*Entity`. Recommend
`data/remote/dto` or `data/remote/model` in the design, with `RemoteInspectionDto` etc., so `*Entity`
unambiguously means Room.

Strictness rules that must be enforceable by review:
- `domain/` has no `com.google.firebase.*`, no `androidx.room.*`, no `android.*` imports (today it already
  satisfies this).
- `data/remote/entity` is never referenced from `ui/` or `domain/`.
- Conversion is explicit in `data/remote/mapper` (no reflection-based `DocumentSnapshot.toObject()` on
  Kotlin data classes: no no-arg constructor, no property setters, defaults/nullability are unreliable).

### 2.2 What must move vs what must NOT move

| Rule | Today | Recommendation in this change |
|---|---|---|
| Scoring (HIGH=5/MEDIUM=3/LOW=1, floor division) | `InspectionViewModel.calculateScore` (`:556-562`) | **Do not move.** Covered by 8+ tests and by `compliance-score-reinspection` spec. |
| Critical Stop (sticky on HIGH non-compliance) | VM state (`:314-323`, `:425-449`) | **Do not move.** |
| 85% threshold + 24h reinspection | VM (`:481-487`, `:495-517`) | **Do not move.** |
| Blank-field auth validation | VM (`:134-139`, `:183-198`) | Keep; add pure domain validators only for new rules (see below). |
| Duplicate-email check | VM + local DAO (`:193-199`) | Keep the VM pre-check (test at `:705-721` depends on it) but treat Firebase's `EMAIL_ALREADY_IN_USE` as the authority. |
| Auth credential verification | Room plaintext | **Must move** behind `AuthRepository`; VM keeps its method signatures. |
| Persistence target | Room only | **Must move behind the repository contract**; VM keeps `saveInspection/saveIncidence/updateInspection` calls. |
| Evidence bytes | Gallery content URI | **Must change** to local file + cloud object (see section 3). |

Validation/business-rule placement: keep the VM-owned inspection rules untouched (config guideline
`openspec/config.yaml:44-48` says rules live in the ViewModel). Add domain-owned pure rules only for the
new concerns: credential shape, ownership/authorization decision, and mapping SDK failures to domain
errors (`AuthError.InvalidCredentials`, `.EmailAlreadyInUse`, `.Network`, `.TooManyRequests`). The data
layer translates `FirebaseAuthException`/`FirebaseFirestoreException` into those domain types; the VM
never sees an SDK error code.

### 2.3 Mapping the JSON blobs and nested collections

Today the Room entity flattens everything into three strings (`InspectionEntity.kt:23-25`):
`modulesJson` (module title, normCodes, items, answers map, naAnswers, score/flags),
`findingsJson` (itemId, description, normCode, criticality, isNA, notes, photoUri, timestamp, lat/lng),
`normCodesJson` (string list) - encoded/decoded by `InspectionMapper` (`:64-172`).

| Option | Shape | Pros | Cons |
|---|---|---|---|
| **A. One document + embedded arrays/maps** | `inspections/{docId}` with scalar fields plus `modules: [ {...items, answers...} ]`, `findings: [ {...} ]`, `normCodes: [...]` | Atomic single write; one read per record; trivially mirrors domain; smallest mapping surface; no fan-out cost in Historial/Detail | Whole array must be rewritten on any finding edit (post-save evidence edits would rewrite the document); no per-finding queries; document grows with evidence URLs |
| **B. Subcollections** | `inspections/{folio}/modules/{n}`, `.../findings/{itemId}` | Per-element updates, queryable findings, smaller documents | 1+N reads for every list/detail screen; more mappers and more code; partial-write consistency; indexes per subcollection |
| **C. Store the JSON strings verbatim** | `modulesJson: String` in Firestore | One-to-one reuse of `InspectionMapper`; least code | Opaque, unqueryable, keeps `org.json` coupling, contradicts "Firebase entity" modeling; no server-side queries or rules on content |
| **D. Hybrid** | Scalars + embedded `modules`; `findings` as subcollection | Queryable/individually updatable findings; inspects stay one read | Two read paths; more code than A; only pays off if post-save evidence edits are real |

Sizing check: the largest record is 3 modules x 5 items plus findings. Even with 200-char download URLs,
a record is on the order of 10-30 KB, far below the 1 MiB Firestore document limit. Document
size is not the constraint; **update granularity** is.

Recommendation (for the proposal to confirm): **Option A** for inspections, because the only post-save
mutation today is `updateRecordStatus` (a scalar field, `InspectionViewModel.kt:535-548`) and evidence is
attached **before** the dictamen is saved. Option D becomes justified only if a new requirement allows
editing findings after save or cross-device finding queries.

Incidences: today they are independent rows without `inspectionId`, written before a folio exists
(`InspectionViewModel.kt:354-374`). Two viable shapes: (a) top-level `incidences/{id}` keyed by owner,
matching today; (b) generate the inspection document id at session start, then
`inspections/{id}/findings/{itemId}`. Shape (b) is cleaner but requires a VM change (id at
`startNewSession`) and therefore new tests - an explicit decision for the proposal.

### 2.4 Document identity

`folio` is the Room PK and is generated from the local list (`:550-554`). In Firestore, using `folio` as
the document id makes cross-device collisions silently overwrite another inspector's record (last write
wins). Options: (a) Firestore auto-id / `UUID` as document id and keep `folio` as a display field with a
per-owner uniqueness check; (b) keep `folio` as id and accept collisions. (a) is recommended; it also
removes the need for a compound index on folio for lookups. This changes `InspectionSessionRecord`
identity handling in the repository only, not the VM's folio generation (no test asserts the folio
format; the VM keeps generating `AUD-YYYY-NNNN` and tests reuse the returned `record.folio`, e.g.
`InspectionViewModelTest.kt:610`).

## Photo / Evidence Storage

### 3.1 Path convention

Recommended: `evidence/{ownerUid}/{inspectionId}/{itemId}.jpg` (or `{itemId}-{timestamp}.jpg` if
re-captures must be preserved). Rules mirror the path segments:
`match /evidence/{uid}/{inspectionId}/{file} { allow read, write: if request.auth != null && request.auth.uid == uid; }`.
Coupling to `inspectionId` gives traceability and simple cleanup. Alternative
`evidence/{uid}/{uuid}.jpg` decouples upload from inspection existence but loses traceability.

### 3.2 Lifecycle and durability (the part people get wrong)

- **Firestore writes are durably queued offline** (persistent cache; writes replay when the device comes
  back online; multiple writes to one document are last-write-wins). Verified:
  https://firebase.google.com/docs/firestore/manage-data/enable-offline.
- **Firebase Storage `UploadTask`s are in-process.** They do not survive process death, unlike Firestore
  writes. So "upload at attach time and forget" is not durable. Options:
  1. Add WorkManager (new dependency) + a Room `pendingUpload` flag; retry on connectivity.
  2. Keep the local file and a `pendingUpload` flag; retry on repository init / next save / app start
     (no new dependency, no guaranteed background scheduling).
  3. Upload only while the app is alive and treat failures as UI errors (weakest, not recommended).
- Store `storagePath` as the stable identity and `downloadUrl` as a cache. `getDownloadUrl()` is a network
  call and tokens can change; never treat the URL as the document's identity.

### 3.3 Local-first resolution (protects the 42 tests and the PDF export)

Do **not** overwrite `SessionFinding.photoUri` with an `https://` URL. Keep the domain field meaning
"local, resolvable reference" (as today) and add cloud metadata in the remote DTO only. Then:
- capture -> save to app-private storage (`filesDir/evidence/...`) via `FileProvider` (recommended) or
  keep the MediaStore copy (current behavior, gallery pollution), set `photoUri` to the local path/URI;
- upload the bytes; persist `storagePath`/`downloadUrl` in Firestore and optionally a local
  `remotePhotoUrl` cache column;
- render local first, fall back to the remote URL. A remote fallback needs an image loader (Coil/Glide -
  neither is a dependency today; `GalleryUtils.loadBitmapFromUri` only handles local `content`/file).
- `InspectionPdfGenerator` reads only `content://` or filesystem paths (`:200-212`); leaving `photoUri`
  local keeps PDF export working unchanged.

### 3.4 Size and compression

Current pipeline is already lossy twice: `TakePicturePreview` thumbnail -> JPEG q90 into MediaStore
(`GalleryUtils.kt:20-31`). Recommended for cloud: upload the existing JPEG bytes as-is in the first
slice (no new quality work), then decide whether to move to `TakePicture` + `FileProvider` + downscale
to ~1600 px / 80% (bigger change: manifest provider, file paths, capture UI).
Firebase Storage has a per-object size limit far above a phone photo; cost is the real constraint
(Blaze plan, see Blockers and Prerequisites).

## Auth Options

| Option | Offline behavior | Fit for field app | Cost/effort | Notes |
|---|---|---|---|---|
| **Email/Password only** | Existing session persists; a *new* sign-in needs connectivity | Good: matches current UI (email + password + cedula) | Low | Reuses the current Login/Register screens almost verbatim; `createUserWithEmailAndPassword`, `signInWithEmailAndPassword` |
| Email/Password + Google Sign-In | Same | Nice UX, but Google sign-in is an interactive online flow | Medium | Needs Credential Manager/Google Identity + SHA-1/SHA-256 fingerprints; on a shared field device, account switching is easier than desired |
| Anonymous auth | First auth needs connectivity; uid persists | Poor as the only method: no per-inspector identity, data cannot be attributed and recovered across devices | Low-Medium | Acceptable only as a "try before register" bridge; rules become per-uid with no recovery path |
| Custom backend / phone OTP | Online | Overkill for this assignment | High | Rejected |

Recommendation: **Email/Password only** for this change. Google Sign-In is a clean later increment if
the product needs it.

Existing users:
- The seeded demo user (`inspector@seguridad.mx` / `password123`, `AppDatabase.kt:42-56`) must be created
  in Firebase Auth (console or a one-shot bootstrap), or the seed/UI defaults must change
  (`LoginScreen.kt:96-97` pre-fills those credentials - they would fail against Firebase until created).
- Real local users cannot be auto-migrated without either (a) creating Firebase accounts with the known
  plaintext passwords on first successful login (technically possible because passwords are plaintext -
  and a strong argument to delete the column), or (b) manual account recreation. For a class project,
  **clean start** is the honest option; if migration is required, it must be its own task with network
  and failure handling.
- `name`/`cedula` have no Firebase Auth equivalent: store a `users/{uid}` profile document (Firestore) or
  keep a local profile cache. Inspection records already denormalize `inspectorName`/`inspectorCedula`
  (`domain/model/InspectionSession.kt:44-63`, fields at `:50-51`), so historical records survive profile
  changes.
- `users` table must have its `password` column removed or zeroed; keeping plaintext passwords next to an
  Auth provider is a review blocker.

## Offline-First Decision

Firestore's Android client has persistent cache **enabled by default** (same docs page above), with a
default ~100 MB cache threshold; reads fall back to cache and writes are queued and replayed. So
"Firestore-only" is technically offline-capable. The question is whether it should replace Room.

| Dimension | A. Room as cache + Firestore sync (recommended) | B. Firestore-only |
|---|---|---|
| Read path in UI | Unchanged: VM collects Room `Flow` -> `StateFlow`; identical ordering/instant cache | Must wrap `addSnapshotListener` in `callbackFlow`; emits `fromCache` and server snapshots, ordering must be enforced client-side |
| VM impact | **Zero** if it keeps calling the same repository methods | Zero if repository signatures preserved, but flow semantics/timing change; VM `observeAllInspections` (pre-login all rows) must become auth-scoped |
| 42 tests | Green: boundary is mocked; only an import line moves if the interface relocates to `domain` | Green for the same reason, but new sync tests cannot use the Firestore SDK in JVM unit tests |
| Conflict resolution | Explicit: `updatedAt` + `syncStatus` columns; remote applies only if not locally dirty and `remoteUpdatedAt >= local`; status transitions are scalar LWW | Firestore LWW at document level; a stale offline device can clobber a whole document (findings included) unless status updates use field-level `update()` and the rest is immutable |
| Local data migration | Room v2 -> v3 needs a real `Migration(2,3)` (or destructive fallback wipes); backfill job optional | Existing Room rows are abandoned unless backfilled; users lose history |
| Offline field use | Best: Room is the source of truth, sync is best-effort; writes already succeed offline | Good, but the offline queue lives in the SDK; app has less control over retries/diagnostics |
| Query cost | Room queries free; Firestore used for sync only | Every screen change hits Firestore (and offline queries scan cache; auto-indexing is off by default) |
| Effort | Medium: interface extraction, sync columns, mapping, mirror logic | Medium-High: reimplement all queries/listeners, indexes, rules, test fakes, and data backfill; still needs Room for WorkManager-style pending state if Storage retries are required |

Test impact detail (A and B share most of it):
- 41 VM tests keep passing because they mock the repository; the only compile change is the import of the
  interface if it moves to `domain/repository` (assertions unchanged).
- New unit tests can cover: remote DTO <-> domain mappers (pure Kotlin `Map<String, Any?>`; no `org.json`,
  no Robolectric), auth error translation, sync merge rules (if A), folio/doc-id generation.
- The Firestore/Storage SDKs cannot run in JVM unit tests. Any remote behavior test is either a fake data
  source test or an instrumented emulator test (`androidTest`, Firebase Emulator Suite, `demo-` project).

Recommendation: **A (Room as cache + Firestore sync)** for the first slice, because it preserves the
existing read path, the ViewModel, and the 42 tests, and it is the only option where "offline-first" is a
property of the app rather than a property of the SDK's cache. Firestore-only is a legitimate later
simplification (removing Room) if the sync layer proves to be pure overhead.

## Blockers and Prerequisites

| Item | Status | Evidence / action |
|---|---|---|
| `google-services.json` | **Missing** | No file found anywhere in the project. Required by the plugin; generate from a Firebase project with the exact `applicationId` `com.example.dam_t1_evaluacion_practica` (`app/build.gradle.kts:14`) |
| Firebase project | **Unknown / blocker** | Not discoverable from the repo; must be created by the user |
| Storage billing | **Blaze required** | Cloud Storage for Firebase requires the pay-as-you-go Blaze plan; requirement effective 2026-02-03 (Firebase Storage FAQ, fetched 2026-11-06). Spark projects get 402/403 on Storage calls |
| Firebase Android BoM | **35.1.0 verified as latest + cached locally** | firebase.google.com/support/releases (2026-11-03) lists BoM 35.1.0 and google-services 4.5.0; local cache `~/.gradle/caches/modules-2/files-2.1/com.google.firebase/firebase-bom/35.1.0/...pom` |
| BOM-managed versions | **Verified from the cached BOM POM** | auth 25.0.0, firestore 27.1.0, storage 23.0.0, common 23.0.0 |
| `firebase-storage` artifact | **Not in local cache** | First build will download it; offline builds will fail until then |
| google-services plugin 4.5.0 vs AGP 9.4.1 | **Unverified** | 4.5.0 is the documented latest; no explicit AGP 9.4 compatibility statement found. Proving step: apply the plugin, run `assembleDebug` + `test` |
| Gradle requirement | **Verified** | AGP 9.4 requires Gradle 9.6.0 (developer.android.com AGP table); project already uses 9.6.0 (`libs.versions.toml:2`, wrapper) |
| `INTERNET` permission | **Transitive only** | Source manifest lacks it; merged manifest gets it from `play-services-maps:18.2.0`. Declare it explicitly |
| Firestore composite indexes | **Required** | `whereEqualTo(ownerUid) + orderBy(timestamp DESC)` needs a composite index; deploy via console/`firestore.indexes.json` |
| Security rules | **Required, none exist** | Rules must scope reads/writes to `request.auth.uid`; create/update must validate `ownerUid` (use `request.resource.data`) |
| DI / background work / image loader | **Absent** | No Hilt/Koin, no WorkManager, no Coil/Glide. Decide whether adding them is in scope (they are the honest cost of durable uploads/remote images) |
| Emulator/testing infra | **Absent** | No Firebase Emulator config, no `androidTest` beyond the template |
| Repo hygiene | **Decision** | `.gitignore` does not exclude `google-services.json`; Firebase treats it as non-secret identifiers, but committing it alongside a public API key needs an explicit call |

## Approaches (consolidated)

1. **Thin cloud adapter (recommended first slice)** - extract domain repository interfaces, keep Room as
   the read model, add Firestore write-through + a snapshot mirror, add Firebase Auth behind the same
   repository methods, upload evidence bytes to Storage while keeping local `photoUri`.
   - Pros: minimal VM/test churn; offline-first preserved; strict Entity<->Domain separation is
     enforceable in new packages; each layer can ship independently.
   - Cons: two stores to keep consistent; needs an explicit merge policy; still needs a durable upload
     queue decision.
   - Effort: Medium.
2. **Firestore-only rewrite** - drop Room for inspections/incidences, enable the default persistent cache,
   drive the UI from snapshot listeners.
   - Pros: single source of truth; no dual-write merge; smaller data layer long-term.
   - Cons: rewrites every read path and its tests; needs indexes/rules early; destructive for existing
     local data; loses Room's deterministic ordering/instant reads; offline queue semantics owned by the
     SDK.
   - Effort: High.
3. **Metadata-only cloud sync** - upload inspection JSON to Firestore, leave photos local (or base64 in
   Firestore).
   - Pros: cheapest; no Storage/Blaze requirement.
   - Cons: fails the user's explicit requirement ("almacenamiento ... de evidencias"); base64 violates
     the 1 MiB document limit and inflates cost; evidence remains device-bound.
   - Effort: Low, but does not satisfy the request. **Rejected.**
4. **Full Clean Architecture rewrite first** - introduce use cases, split the 568-line VM, add DI, then add
   Firebase.
   - Pros: architecturally clean.
   - Cons: huge diff, likely breaks/rewrites the 41 tests, delays the actual ask, gold-plating.
   - Effort: High. **Not recommended in this change.**

## Recommendation

Adopt **Approach 1** on top of **Room-as-cache (option A offline)** and **Email/Password** auth, with:
- domain repository interfaces preserving the current method signatures (so the VM and its 41 tests need
  no behavioral change; only the interface import moves);
- `data/remote/entity` DTOs + explicit `data/remote/mapper` converters using `Map<String, Any?>` (no
  reflection, no `org.json`), keeping Firestore shapes out of `domain/` and `ui/`;
- inspections as one document with embedded `modules`/`findings`/`normCodes` arrays, document id =
  generated id, `folio` as a display field;
- evidence bytes in Storage at `evidence/{uid}/{inspectionId}/{itemId}.jpg`, local path retained in
  `photoUri`, remote path/URL cached for other devices;
- scoring / Critical Stop / 85%-24h rules **left in the ViewModel**.

This satisfies the user's strict separation and validation-in-domain requirements for the *new* concerns
while avoiding a rewrite of rules that are already specified and tested.

## Risks

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| R1 | Room v2 has `fallbackToDestructiveMigration` and no migration object (`AppDatabase.kt:22-24`, `:41`). Any schema change silently wipes inspections/users | High - data loss on upgrade | Decide destructive-vs-migration before schema changes; a `Migration(2,3)` with `ALTER TABLE ADD COLUMN` is small |
| R2 | Plaintext passwords in `users` (`UserEntity.kt:10`) and SQL comparison (`UserDao.kt:15-16`) | High - security finding | Remove/zero the column, delete the seed password default, migrate or recreate accounts |
| R3 | `photoUri` is a device-local MediaStore URI (`GalleryUtils.kt:26`); PDF export only reads local paths/URIs (`InspectionPdfGenerator.kt:203-212`) | High - evidence lost cross-device; silent PDF gaps | Keep local reference as the domain value, add remote path/url in the DTO, local-first rendering |
| R4 | Folio is generated from the local list (`InspectionViewModel.kt:550-554`) | High - duplicate ids -> document clobbering across devices | Use a generated Firestore id / UUID; keep folio as display data |
| R5 | Storage uploads are not durable across process death; no WorkManager | Medium - evidence may never reach the cloud | `pendingUpload` flag + retry at app start (no new dependency) or add WorkManager |
| R6 | `org.json`-based mappers are not JVM testable; remote mappers could inherit the pattern | Medium - untestable sync layer, TDD blocked | Use plain `Map<String, Any?>` DTO converters; keep Room's `org.json` mapper as-is |
| R7 | 42 tests depend on `InspectionRepository` being mockable at the current boundary | Medium - regression pressure | Keep signatures; move only the import; add new tests below the boundary |
| R8 | No DI: `MainActivity` builds the graph by hand (`:62-65`); adding Firebase auth/storage/firestore constructors inflates it | Medium - maintainability | Keep a manual composition root (a small factory/container) rather than introducing Hilt in this change |
| R9 | Firestore rules/indexes are console/CLI artifacts, outside the app build | Medium - local build green, real devices broken | Treat rules + `firestore.indexes.json` as deliverables of the change, verified in the emulator |
| R10 | Pre-login `observeAllInspections` exposes every local record (`InspectionViewModel.kt:110-120`) | Medium - privacy on shared devices | Scope all reads to the signed-in uid; rules enforce it server-side |
| R11 | Photos are captured via `TakePicturePreview` (thumbnail quality) | Low-Medium - weak evidence | Keep for the first slice (upload existing bytes); full-res `TakePicture` + `FileProvider` as a separate decision |
| R12 | Existing incomplete change `compliance-score-reinspection` also touches persistence/spec wording | Medium - conflicting deltas | Keep this change's specs additive about cloud sync; do not rewrite scoring/reinspection semantics |
| R13 | `google-services.json` / Blaze account may not be ready | High - nothing can be verified end-to-end | Make it the first blocking prerequisite; a manual `FirebaseOptions` bootstrap is a fallback if the JSON cannot be added |

## Smallest Viable Slice Ordering

- S0 - Build plumbing + seams: add google-services plugin, Firebase BOM 35.1.0, auth/firestore/storage
  artifacts, explicit `INTERNET`, `google-services.json`; extract `domain/repository` interfaces with the
  existing implementations renamed; `assembleDebug` + all 42 tests green.
- S1 - Auth: `AuthDataSource` + `FirebaseAuthRepository`; VM keeps `login/register/logout` signatures;
  remove plaintext storage; map `FirebaseAuthException` to domain `AuthError`; add auth error tests.
- S2 - Inspections in Firestore: `RemoteInspectionDto` + explicit mappers; Firestore data source;
  write-through from `saveInspection`/`updateInspection`; snapshot mirror into Room (or read-through cache);
  merge rule via `updatedAt`/`syncStatus`; mapper + merge unit tests.
- S3 - Evidence: Storage data source, path convention, local-first resolution, upload retry policy,
  `downloadUrl` cache; PDF/photo consumers unchanged.
- S4 - Rules/indexes/verification: security rules, `firestore.indexes.json`, emulator smoke test,
  manual two-device check.
- S5 - Optional: backfill of existing Room rows/users, or clean-start announcement.

S1 and S2 are independent after S0; S3 depends on S2's document id decision.

## Open Product Decisions (blocking the proposal)

1. **Auth methods** - Email/Password only, Email/Password + Google, or anonymous? (Recommendation:
   Email/Password only.)
2. **Offline-first architecture** - Room as cache + Firestore sync, or Firestore-only? (Recommendation:
   Room as cache.)
3. **Data policy** - clean start (destructive migration, recreate users) or preserve/backfill existing
   Room inspections and users? (Recommendation: clean start for this course change.)
4. **Firebase project readiness** - does a project exist, who owns it, is Blaze enabled, and when can
   `google-services.json` be provided? (Hard blocker for Storage.)
5. **Evidence storage** - upload the existing gallery copy, or switch capture to app-private
   `FileProvider` + full-resolution `TakePicture`? Is a durable upload queue (WorkManager) in scope?
6. **Ownership/visibility model** - per-inspector private records, or a shared company collection where
   any authenticated inspector can read all? (Drives Firestore rules and queries.)
7. **Document identity + incidence modeling** - use generated ids with `folio` as display data? Do
   incidences become a subcollection of the inspection (requires linking an inspection id at session
   start), or stay top-level?
8. **Test strategy** - approve the repository-interface + fake-data-source approach for new tests, and
   decide whether the Firebase Emulator (instrumented tests) is in scope. Without that, the cloud layer is
   only verified manually.
9. **Repo hygiene** - commit `google-services.json` or keep it ignored/injected?

## Ready for Proposal

**No, pending decisions 1-4 (blocking) and 5-9 (scope-shaping).** All code paths and constraints are
mapped and evidence-based. Once the user answers the auth method, offline-first stance, data policy, and
Firebase project readiness, `sdd-propose` can define scope, rollback, and the slice boundary.
