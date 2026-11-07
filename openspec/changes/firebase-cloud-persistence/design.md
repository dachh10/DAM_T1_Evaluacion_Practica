# Design: Firebase Cloud Persistence

Change: `firebase-cloud-persistence` · Store: `openspec` · Language: English (UI copy stays Spanish)

## Technical Approach

Room remains the UI's source of truth; Firestore is a per-inspector replica written by a
WorkManager-driven coordinator; Cloudinary holds evidence bytes. Firebase Auth replaces the local
`users` credential table. The ViewModel keeps its single constructor parameter and its scoring,
Critical Stop, and 85%/24 h rules untouched, so the 41 `InspectionViewModelTest` cases plus the
template stay green with only an import line moving.

The change introduces a domain contract surface (repository interfaces + errors + validators), a
Firebase/Cloudinary adapter layer with pure `Map<String, Any?>` mappers, and a durable outbox-by-state
model expressed in Room columns. Firestore is never read by the UI directly.

Versions: Firebase BoM `34.12.0` (pinned), `google-services` `4.5.0`, `cloudinary-android` `3.1.3`
are orchestrator notes, **not research evidence** (`research.md` is `blocked`). BoM `35.x` and `34.13.0+`
are rejected: their `firebase-auth` ships Kotlin metadata `2.4.0` / `2.3.0`, unreadable by this
project's Kotlin `2.2.10`. BoM `34.12.0` is the highest release whose `firebase-auth` `24.0.1` is
compiled with metadata `2.1.0`, matching `firebase-firestore` and `firebase-common`. Verified on
2026-11-06 by AAR inspection plus `javap` on the Kotlin classes. `androidx.work:work-runtime` resolved
to `2.12.0`.

## 1. Layer and Package Layout

```
com.example.dam_t1_evaluacion_practica/
├── SecurityInspectionsApp.kt        NEW  Application: owns AppContainer, Cloudinary init
├── AppContainer.kt                  NEW  manual composition root
├── MainActivity.kt                  MOD  reads AppContainer, auth-aware navigation
├── domain/
│   ├── model/                            existing + Evidence.kt, SyncState.kt, AuthSession.kt
│   ├── repository/                  NEW  InspectionRepository, AuthRepository, EvidenceRepository
│   ├── validation/                  NEW  AuthValidator, SyncConflictPolicy, EvidenceBudget
│   └── error/                       NEW  AuthError, SyncError, EvidenceError, AuthException
├── data/
│   ├── local/                            Room v3: entities, DAOs, mappers
│   ├── remote/
│   │   ├── mapper/                  NEW  Inspection/Incidence/Profile RemoteMapper (Map only)
│   │   ├── firestore/               NEW  RemoteInspectionDataSource + Firestore impl + CloudMirror
│   │   ├── auth/                    NEW  FirebaseAuthDataSource
│   │   └── cloudinary/              NEW  EvidenceUploader + Cloudinary impl
│   ├── repository/                       InspectionRepositoryImpl, FirebaseAuthRepository,
│   │                                     DefaultEvidenceRepository
│   └── sync/                        NEW  SyncCoordinator, SyncScheduler, InspectionSyncWorker,
│                                          EvidenceUploadWorker, CloudMirror
├── ui/                                   existing screens + components/EvidenceImage.kt
└── util/                                 existing + EvidenceStorage, EvidencePreparer, RemoteImageLoader
```

Enforceable rules: `domain/` imports no `com.google.*`, `androidx.room.*`, `android.*`; `data/remote/`
types are unreachable from `ui/` and `domain/`; only `data/remote/firestore` sees `DocumentSnapshot`.

**`Uri` decision**: keep the domain as `String?` references (`photoUri`), no `android.net.Uri`.
Rationale: the value already crosses Room, JSON, Firestore, and tests as a string; wrapping it adds no
validation and would drag a framework type into `domain/` (today `Incidence.kt:14` and
`InspectionSession.kt:34` are plain `String?`). Semantics are documented as "resolvable reference:
local file, `content://`, or `https` URL".

## 2. Domain Contracts and Validation Placement

```kotlin
interface InspectionRepository {
    fun getAllInspections(): Flow<List<InspectionSessionRecord>>
    fun getInspectionsByUser(userEmail: String): Flow<List<InspectionSessionRecord>>
    fun getInspectionByFolio(folio: String): Flow<InspectionSessionRecord?>
    suspend fun saveInspection(record: InspectionSessionRecord): Long
    suspend fun updateInspection(record: InspectionSessionRecord)
    fun getAllIncidences(): Flow<List<Incidence>>
    suspend fun saveIncidence(incidence: Incidence): Long
    suspend fun getUserByEmail(email: String): User?
    suspend fun login(email: String, password: String): User?
    suspend fun registerUser(user: User, password: String): Long
    suspend fun deleteInspection(inspectionId: String)                 // tombstone + enqueue
    suspend fun retrySync(inspectionId: String)
    suspend fun registerEvidence(inspectionId: String, itemId: String, ref: String)
    fun observeEvidence(inspectionId: String): Flow<List<Evidence>>
}

interface AuthRepository {
    val session: Flow<AuthSession?>
    suspend fun signIn(email: String, password: String): User      // throws AuthException
    suspend fun register(name: String, email: String, cedula: String, password: String): User
    suspend fun signOut()
    fun observeProfile(uid: String): Flow<User?>
    suspend fun cachedProfile(uid: String): User?
}

interface EvidenceRepository {
    fun observe(inspectionId: String): Flow<List<Evidence>>
    suspend fun register(inspectionId: String, itemId: String, localRef: String): Evidence
    suspend fun retry(evidenceId: String)
}
```

`InspectionRepository` also exposes the evidence subset (`registerEvidence`, `observeEvidence`) because
the ViewModel constructor must stay single-parameter; `EvidenceRepository` is the worker-facing contract
and `InspectionRepositoryImpl` delegates to it. This is a deliberate test-seam concession, not a second
source of truth.

Errors: `AuthError { INVALID_CREDENTIALS, EMAIL_ALREADY_IN_USE, WEAK_PASSWORD, NETWORK_UNAVAILABLE, UNKNOWN }`
as an enum; `AuthException(val error: AuthError)` carries it without changing `login(...): User?`.
`SyncError { NETWORK, PERMISSION_DENIED, NOT_FOUND, INVALID_DATA, EXHAUSTED, UNKNOWN }` and
`EvidenceError { NETWORK, REJECTED, TOO_LARGE, STORAGE_IO, EXHAUSTED, UNKNOWN }` are mapped in the
adapters and only their names are persisted (`lastError`).

Validation placement:

| Rule | Owner | Why |
|---|---|---|
| Email format, password ≥ 6 | `domain/validation/AuthValidator` | Pure, testable, no request on invalid input |
| Ownership / authorization | Firestore rules + `ownerUid` stamped by the data source from `auth.currentUser` | Never trust the caller's record |
| Remote-vs-local merge | `domain/validation/SyncConflictPolicy` | Pure predicate, unit-testable |
| Compression ceiling (2048 px / 2 MiB / q≥70) | `domain/validation/EvidenceBudget` | Pure constants + predicate; Android code only applies it |
| Scoring HIGH=5/MEDIUM=3/LOW=1, Critical Stop, 85%/24 h | **stays in `InspectionViewModel`** (`calculateScore`, `submitAnswer`, `finishModule`, `isReinspectionDue`, `scheduleReinspection`) | 41 tests and `compliance-score-reinspection` own these semantics |

No duplication: the data layer persists `overallScore`, `criticalStop`, `reinspectionDue`, and
`scheduledReinspectionAt` verbatim; neither the mapper nor Firestore recomputes them.
Password length applies to **registration only** — the login test calls `login("test@seguridad.mx", "pass")`
(`InspectionViewModelTest.kt:729`) and must keep succeeding; Firebase validates sign-in.

## 3. Test Seam: How the 42 Tests Survive

The seam is `InspectionRepository` as a domain interface, with the existing class renamed to
`data/repository/InspectionRepositoryImpl`.

```kotlin
// UNCHANGED arity and behavior; only the imported type changes
class InspectionViewModel(private val repository: InspectionRepository) : ViewModel()
```

| Element | Change |
|---|---|
| `InspectionViewModelTest.kt:3` import | `data.repository.InspectionRepository` → `domain.repository.InspectionRepository` |
| `mockk<InspectionRepository>(relaxed = true)` (`:32`) | none — now a mock of the interface |
| `coEvery { repository.login/getUserByEmail/registerUser/getInspectionsByUser }` | none; signatures preserved verbatim, including `registerUser(any(), any()) returns 1L` |
| 41 test bodies | none |
| `InspectionViewModelFactory` | parameter type becomes the interface; `MainActivity.kt:63-65` wiring only |
| New interface members (`deleteInspection`, `retrySync`, `registerEvidence`, `observeEvidence`) | relaxed mock returns defaults; no test stubs or asserts them. `registerEvidence` is invoked only inside the existing `viewModelScope.launch` alongside `saveIncidence` (same coroutine that already hits the mocked repository); `observeEvidence` is collected only after `startNewSession`, which no test calls |
| `startNewSession` | rotates an in-memory UUID only, no I/O, not called by any test |

`saveDictamen()` stays synchronous-returning; persistence still happens in `viewModelScope.launch`.
New tests below the boundary use hand-written fakes (`FakeInspectionRepository`,
`FakeRemoteDataSource`, `InMemoryLocalStore`), not `mockk`.

## 4. Firestore Data Model

```
inspections/{inspectionId}                      // inspectionId = UUID, created at session start
  ownerUid: string        ownerEmail: string     folio: string      // folio = display only
  companyName, zoneName, auditType, inspectorName, inspectorCedula: string
  timestamp: Timestamp   updatedAt: Timestamp (serverTimestamp, write-once set at first push)
  overallScore: number    criticalStop, reinspectionDue: bool
  scheduledReinspectionAt: Timestamp|null   status: string   latitude, longitude: number|null
  modules: [ { moduleTitle, normCodes:[string], score, criticalStop, reinspectionDue,
               completed, answers:{itemId:bool}, naAnswers:[string],
               items:[ {id, description, normCode, normTitle, criticalityLevel,
                        requiresPhotoEvidence} ] } ]
  findings: [ { itemId, itemDescription, normCode, criticalityLevel, isNA, notes|null,
                photoUri: https|null, evidencePending: bool, timestamp: Timestamp,
                latitude|null, longitude|null } ]
  normCodes: [string]     schemaVersion: 1

inspections/{inspectionId}/incidences/{incidenceId}
  ownerUid, checklistItemId, status, timestamp, latitude|null, longitude|null,
  photoUri: https|null, notes|null, updatedAt: Timestamp

users/{uid}
  email, displayName, cedula, updatedAt: Timestamp
```

**Blobs → maps/arrays, not strings.** `modulesJson`, `findingsJson`, `normCodesJson`
(`InspectionEntity.kt:23-25`) become real arrays/maps: the doc stays queryable/inspectable, needs no
`org.json` on the remote path, and a single ~10–30 KB document is far below the 1 MiB limit
(exploration sizing). `answers` is a map because keys are `epp_1`, `mach_3` (verified in
`SampleData.kt`) — Firestore map keys cannot contain `.`, `/`, `~`, `[`, `]`; the mapper rejects and
drops those keys defensively. `normCodes` is a string array, not a JSON string.

Timestamps: business times (`timestamp`, incidence `timestamp`) are milliseconds in the
mapper and converted to `com.google.firebase.Timestamp` by the data source; `updatedAt` is always
`FieldValue.serverTimestamp()` — never client-supplied (that is what makes the conflict rule
monotonic).

## 5. DTO ↔ Domain Mappers

One object per entity, all in `data/remote/mapper`, all pure Kotlin:

```kotlin
object InspectionRemoteMapper {
    fun toMap(record: InspectionSessionRecord,
              evidenceUrls: Map<String, String>): Map<String, Any?>          // millis, plain types
    fun fromMap(inspectionId: String, map: Map<String, Any?>): InspectionSessionRecord
}
object IncidenceRemoteMapper { fun toMap(i: Incidence): Map<String, Any?>
                               fun fromMap(incidenceId: String, m: Map<String, Any?>): Incidence }
object ProfileRemoteMapper   { fun toMap(u: User): Map<String, Any?>
                               fun fromMap(uid: String, m: Map<String, Any?>): User }
```

No `org.json`, no Firebase types, no reflection, no `DocumentSnapshot.toObject()` (Kotlin data classes
lack no-arg constructors and reliable defaults).

Round-trip contract: `fromMap(id, toMap(record, urls))` equals `record` after applying the same
evidence substitution, with four documented deviations: (a) `inspectionId`/`incidenceId` come from the
document id argument; (b) `photoUri` is the `secure_url` from `urls`, else `null` with
`evidencePending = true`; (c) `updatedAt` is server-owned; (d) `status`/`criticalityLevel` fall back on
unknown values.

Null/unknown policy: optional scalars (`latitude`, `longitude`, `notes`, `scheduledReinspectionAt`)
are **omitted** when null; `findings[].photoUri` is **written as null** when unresolved so a stale URL
is cleared. Reads treat missing keys as null, missing strings as `""`, `Number`→`Long`/`Double`,
unknown keys ignored, unknown enums → `OPEN`/`LOW`, `schemaVersion > 1` still parsed best-effort and
never throws.

## 6. Sync Architecture

State lives in Room: `inspections.syncState`, `lastSyncedAt`, `updatedAt`, `ownerUid`; and
`incidences.syncState`; `evidence.status/attempts/lastError`.

```
LOCAL_ONLY ──save/update/delete──► PENDING ──worker ok──► SYNCED
                                      │                     │
                                      └─attempts exhausted──► FAILED ──retrySync──► PENDING
```

Write path: `saveInspection` inserts `LOCAL_ONLY` in Room, then the `SyncCoordinator` owns the state
machine — it sets `PENDING`, enqueues unique work `inspection-sync-{id}` through `SyncScheduler`
(`ExistingWorkPolicy.REPLACE`, `NetworkType.CONNECTED`, exponential backoff 30 s, max 5 attempts), and
records `FAILED`/`SYNCED` plus `lastSyncedAt` when the worker reports back. The worker re-reads the
current Room state, so a later edit is always what gets pushed.

Push order (Firestore gotcha): rules see the **pre-batch** state, so `exists(parent)` cannot be
satisfied by a parent written in the same batch. The worker therefore (1) `set` the parent and await,
(2) batch `set` each incidence under it, (3) mark `SYNCED`. Every step is idempotent because ids are
stable. Non-retryable errors (`PERMISSION_DENIED`, `INVALID_ARGUMENT`, unknown parent) → immediate
`FAILED`; transient errors → `Result.retry()`, and `runAttemptCount >= 5` → `FAILED`. `REPLACE` resets
the attempt budget on a genuine new edit, which is intended; it never resets it on a retry.

Conflict rule (exact): `CloudMirror` applies a remote snapshot **only if**
`local.syncState == SYNCED && remote.updatedAt > local.updatedAt`; then it upserts and sets
`local.updatedAt = remote.updatedAt`. A local record in `LOCAL_ONLY/PENDING/FAILED` ignores remote
snapshots entirely until its push is acknowledged (local dirty wins), after which the next server
snapshot is newer by construction and wins normally. `DocumentChange.Type.REMOVED` deletes locally
only when `syncState == SYNCED`; dirty records ignore it. Deletes are local tombstones
(`deleted = 1, syncState = PENDING`) replayed by the worker (incidences first, then the parent, then a
local hard delete) — deleting a Firestore document does not delete its subcollections.

UI: `InspectionSessionRecord.syncState` drives a chip in `HistorialScreen` and
`InspectionDetailScreen` — "Pendiente" (`LOCAL_ONLY`/`PENDING`), "Sincronizado" (`SYNCED`),
"Error · Reintentar" (`FAILED`, button calls `viewModel.retrySync(id)`).

## 7. Inspection Identity at Session Start

```kotlin
private var activeInspectionId: String = UUID.randomUUID().toString()

fun startNewSession() {
    activeInspectionId = UUID.randomUUID().toString()
    // ... existing reset block unchanged
    observeActiveEvidence()          // NOT reached by any of the 42 tests
}
```

`saveDictamen()` passes `inspectionId = activeInspectionId` into `InspectionSessionRecord`, and
`submitAnswer`/`attachFindingEvidence` stamp `Incidence.inspectionId = activeInspectionId`. Because the
field is initialized at construction, `saveDictamen` works in every test that never calls
`startNewSession`. Folio generation (`:550-554`) is untouched and remains display data.
`InspectionSessionRecord` gains `inspectionId: String`, `ownerUid: String = ""`,
`syncState: SyncState = LOCAL_ONLY`, `updatedAt: Long = timestamp`; `Incidence` gains
`inspectionId: String`. Only `InspectionMapper`, the VM, and the remote mappers construct these, so the
tests are unaffected.

## 8. Evidence Pipeline

```
TakePicture → FileProvider URI → EvidenceStorage filesDir/evidence/{inspectionId}/{evidenceId}.jpg
   → EvidencePreparer (≤2048 px, q80→q70, ≤2 MiB) → WorkManager → Cloudinary unsigned → secure_url
```

- Capture: replace `TakePicturePreview()` at `ChecklistModuleScreen.kt:563`, `ChecklistScreen.kt:260`,
  `CriticalStopDialog.kt:93` with `TakePicture()`, launched with a FileProvider URI from
  `EvidenceStorage`. The camera writes the full-resolution still directly to the app file; no bitmap
  round trip, no gallery pollution (`GalleryUtils.saveBitmapToGallery` stays only for legacy reads).
- Compression: longest edge **2048 px**; JPEG quality **80**, floor **70**; hard cap **2 MiB**. If over
  the cap, reduce the edge to 1600 → 1280 → 1024 at q70; never upscale. Full-resolution original is
  kept locally. No `MediaStore` dependency for the uploaded copy.
- Evidence id is deterministic: `UUID.nameUUIDFromBytes("$inspectionId|$itemId")`, so retries and
  re-captures address the same asset. `evidenceId` is the Cloudinary public id suffix.
- Upload wrapper: `MediaManager.get().upload(file).unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
  .option("public_id", "inspections/$inspectionId/$evidenceId")`, bridged to a `suspend` via
  `suspendCancellableCoroutine`. `MediaManager.init(...)` runs once in
  `SecurityInspectionsApp.onCreate` (workers can start cold in a new process).
- Durable queue: `EvidenceUploadWorker`, unique name `evidence-upload-{evidenceId}`,
  `NetworkType.CONNECTED`, exponential backoff 30 s, max 5 attempts, terminal `FAILED` with manual
  retry. `status` transitions `PENDING → UPLOADING → UPLOADED|FAILED`; on success persist `secureUrl`
  and enqueue the parent inspection sync so the document picks up the https URL.
- Persisted reference: `Evidence.remoteUrl` is the reference of record (the `secure_url` persisted on
  upload and replicated into `findings[].photoUri` by the mapper). Room keeps the local FileProvider
  `content://` in `SessionFinding.photoUri` as a display cache only, so previews and offline PDF keep
  working; a `content://` value never reaches Firestore. Unuploaded evidence is written as
  `photoUri = null` with `evidencePending = true`.
- PDF: `InspectionPdfGenerator.generate` becomes `suspend` and runs on `Dispatchers.IO`
  (`InspectionDetailScreen.kt:404-411` already owns a `rememberCoroutineScope`). It resolves
  `content://`, file paths, and `https://` (HttpURLConnection, 10 s timeouts, 2 MiB read cap); an
  unresolvable item draws "Evidencia no disponible" instead of being skipped. UI previews of remote
  evidence use a small `EvidenceImage` composable (`produceState` on IO, LRU cache).

## 9. Auth Flow

`FirebaseAuthDataSource` wraps `FirebaseAuth.getInstance()`; `session` is a `callbackFlow` over
`addAuthStateListener`, emitting `AuthSession(uid, email)`. Task bridging uses a local
`awaitTask()` (`suspendCancellableCoroutine` + `addOnCompleteListener`) — no
`kotlinx-coroutines-play-services` dependency. Session persistence is Firebase's built-in disk cache
(`UNVERIFIED` SDK behavior: research was blocked; S2 validates it with an offline relaunch), so a cold
offline start restores `currentUser` without network; the profile is read from the Room `profiles`
cache, so restoration never needs Firestore.

Registration: create the Auth user → write `users/{uid}` → cache the profile locally. A failed profile
write is non-fatal (account survives); `ensureProfile(uid)` retries on the next sign-in.
`getUserByEmail` reads the local `profiles` cache only (superseded pre-check; Firebase
`EMAIL_ALREADY_IN_USE` is authoritative).

| Firebase `errorCode` | Domain error |
|---|---|
| `ERROR_INVALID_CREDENTIALS`, `ERROR_WRONG_PASSWORD`, `ERROR_USER_NOT_FOUND`, `ERROR_INVALID_EMAIL` | `INVALID_CREDENTIALS` |
| `ERROR_EMAIL_ALREADY_IN_USE` | `EMAIL_ALREADY_IN_USE` |
| `ERROR_WEAK_PASSWORD` | `WEAK_PASSWORD` |
| `ERROR_NETWORK_REQUEST_FAILED` | `NETWORK_UNAVAILABLE` |
| `ERROR_TOO_MANY_REQUESTS`, anything else | `UNKNOWN` |

The VM catches `AuthException`, maps the enum to the existing Spanish strings, and keeps
`authError: String?`. Raw SDK types/codes never leave `data/remote/auth`. Exact Firebase Auth and
Cloudinary API shapes (`addAuthStateListener`, `MediaManager.init`) are `UNVERIFIED`; S2/S4 validate
them against the resolved artifacts.

Navigation: `MainActivity` collects `authRepository.session` (via `AppContainer`) and
`InspectionApp` reacts — restored session → `viewModel.restoreSession(user)` → `Screen.Dashboard`
instead of `Screen.Login` after splash; `null` while on a protected screen → `Screen.Login`.
`restoreSession` mirrors the success branch of `login` (cancel `observeAllInspections`, set
`currentUser`, `observeUserInspections(user.email)`). The `User` comes from
`AuthRepository.cachedProfile(uid)` (Room `profiles`); if the cache is empty, a minimal
`User(email = session.email, name = "", cedula = "")` is used so the app still opens offline and the
profile is repaired on the next online launch.

Offline sign-in with no persisted session fails with `NETWORK_UNAVAILABLE`; no local credential check
exists once the credential table is gone (`users` is replaced by `profiles`, which stores no secret).

## 10. Dependency Injection and Configuration

`AppContainer(context)` is built once by `SecurityInspectionsApp.onCreate` (the Application, not the
Activity, so WorkManager workers can resolve it after process death) and is read by `MainActivity` as
`(application as SecurityInspectionsApp).container`:

```kotlin
class AppContainer(context: Context) {
    val db = AppDatabase.getDatabase(context)
    private val auth: AuthRepository = FirebaseAuthRepository(FirebaseAuthDataSource(), ProfileDao…)
    private val remote: RemoteInspectionDataSource = FirestoreInspectionDataSource(auth)
    private val evidence: EvidenceRepository = DefaultEvidenceRepository(…, CloudinaryEvidenceUploader())
    val inspection: InspectionRepository =
        InspectionRepositoryImpl(db, remote, auth, SyncCoordinator(db, remote, SyncScheduler(context)))
}
```

No Hilt/Koin: the graph is 6 objects and the project already composes by hand
(`MainActivity.kt:62-65`). Workers obtain `container` from `applicationContext` as
`SecurityInspectionsApp`; if a worker ever needs a non-Room collaborator, a small `WorkerFactory` can be
passed to `WorkManager.initialize`, but the default initializer is kept for S0.

Config: `CLOUDINARY_CLOUD_NAME=f2ge9jhj` and `CLOUDINARY_UPLOAD_PRESET=ml_default` live in
`local.properties` (untracked) and become `BuildConfig` fields. They are public identifiers, not
secrets; the API key/secret is never requested, never stored, and never shipped. `google-services.json`
is already present for `seguridad-85edc` / `com.example.dam_t1_evaluacion_practica`.

## 11. Build Changes

`gradle/libs.versions.toml`: add `firebaseBom = "34.12.0"`, `googleServices = "4.5.0"`,
`cloudinary = "3.1.3"`, a `workManager` version that **must be resolved at build time (UNVERIFIED)**,
libraries `firebase-bom`, `firebase-auth`, `firebase-firestore`, `cloudinary-android`,
`androidx-work-runtime`, `androidx-work-testing`, and plugin `google-services`.

`build.gradle.kts` (root): `alias(libs.plugins.google.services) apply false`.
`app/build.gradle.kts`: plugin order `android.application` → `kotlin.compose` → `ksp` →
`google.services` **last**; `buildFeatures { compose = true; buildConfig = true }`;
`implementation(platform(libs.firebase.bom))` plus the two artifacts (never `-ktx`);
`implementation(libs.androidx.work.runtime)`; `testImplementation(libs.androidx.work.testing)`.
`local.properties` is read with `providers.fileContents(...).asText` (configuration-cache aware) and
injected through `buildConfigField`.

`AndroidManifest.xml`: add `<uses-permission android:name="android.permission.INTERNET" />` (today it is
only transitive from `play-services-maps:18.2.0`), `android:name=".SecurityInspectionsApp"`, and the
`FileProvider` with `authorities="${applicationId}.fileprovider"`, `exported="false"`,
`grantUriPermissions="true"`, `@xml/file_paths` → `<files-path name="evidence" path="evidence/" />`.

Configuration-cache risk: the google-services task and WorkManager's startup initializer both run in
configuration/execution of every build. Concrete validation: run
`java -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain :app:assembleDebug`
twice; the second must log `Reusing configuration cache` and produce an APK. If the plugin breaks CC,
the fallback is `org.gradle.configuration-cache=false` scoped in `gradle.properties` with a note — not a
silent `--no-configuration-cache` in CI.

## 12. Security Rules and Indexes

`firestore.rules` (deployed artifact; per-inspector private, the recommendation that resolves the
proposal's open question):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() { return request.auth != null; }
    function parentOwner(id) {
      return get(/databases/$(database)/documents/inspections/$(id)).data.ownerUid;
    }
    match /users/{uid} {
      allow read, write: if signedIn() && request.auth.uid == uid;
    }
    match /inspections/{inspectionId} {
      allow read: if signedIn() && resource.data.ownerUid == request.auth.uid;
      allow create: if signedIn()
        && request.resource.data.ownerUid == request.auth.uid
        && request.resource.data.overallScore is int
        && request.resource.data.overallScore >= 0
        && request.resource.data.overallScore <= 100;
      allow update: if signedIn()
        && resource.data.ownerUid == request.auth.uid
        && request.resource.data.ownerUid == resource.data.ownerUid;
      allow delete: if signedIn() && resource.data.ownerUid == request.auth.uid;
      match /incidences/{incidenceId} {
        allow read: if signedIn() && parentOwner(inspectionId) == request.auth.uid;
        allow create: if signedIn()
          && parentOwner(inspectionId) == request.auth.uid
          && request.resource.data.ownerUid == request.auth.uid;
        allow update: if signedIn()
          && parentOwner(inspectionId) == request.auth.uid
          && resource.data.ownerUid == request.auth.uid
          && request.resource.data.ownerUid == request.auth.uid;
        allow delete: if signedIn()
          && parentOwner(inspectionId) == request.auth.uid
          && resource.data.ownerUid == request.auth.uid;
      }
    }
  }
}
```

A missing parent makes `get()` error, which denies the write — the spec's "unknown parent" scenario.
Rules are not hierarchical: the `incidences` match is mandatory, not inherited.

`firestore.indexes.json` (one composite index for the mirror query
`whereEqualTo(ownerUid).orderBy(updatedAt, DESC)`):

```json
{
  "indexes": [{
    "collectionGroup": "inspections",
    "queryScope": "COLLECTION",
    "fields": [
      { "fieldPath": "ownerUid", "order": "ASCENDING" },
      { "fieldPath": "updatedAt", "order": "DESCENDING" }
    ]
  }],
  "fieldOverrides": []
}
```

`get()` inside rules is capped (10 calls per single-document request, 20 per multi-document request), so
the incidence batch is chunked to at most 8 writes; a larger checklist would otherwise exceed the limit.
No extra index is needed for `users`, which is only read by document id. Rules and indexes are
deliverables of this change even though they sit outside Gradle.

## 13. Test Strategy

| Layer | What | Where |
|---|---|---|
| JVM unit | `AuthValidator` (email forms, 6-char boundary, login vs register asymmetry) | `app/src/test/.../domain/validation` |
| JVM unit | `InspectionRemoteMapper` / `IncidenceRemoteMapper` / `ProfileRemoteMapper` round-trip, nulls, unknown keys/enums, evidence substitution, Firestore-illegal map keys | `app/src/test/.../data/remote/mapper` |
| JVM unit | `SyncConflictPolicy`, `EvidenceBudget` | `app/src/test/.../domain/validation` |
| JVM unit | `SyncCoordinator` / `InspectionRepositoryImpl` with `FakeRemoteDataSource` + in-memory implementations of the Room DAO interfaces: LOCAL_ONLY→PENDING→SYNCED, FAILED terminal, dirty-wins, delete tombstone, parent-before-incidence ordering | `app/src/test/.../data/sync` |
| JVM unit | Auth error mapping table (`FirebaseAuthException.errorCode` → `AuthError`) with a fake exception factory | `app/src/test/.../data/remote/auth` |
| Instrumented (emulator) | Firebase Auth + Firestore against the Emulator Suite; manifest `INTERNET`; capture→compress bound | `app/src/androidTest` — **optional**, no infra exists today |
| Manual | two-device visibility, offline photo upload after reboot | acceptance checklist |

No Robolectric dependency is added; anything needing Android graphics (`EvidencePreparer`) is exercised
on an emulator or manually, while its pure budget predicate is unit-tested.

Commands (from repo root):

```
java -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain test
java -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain :app:assembleDebug
java -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain :app:lint
java -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain :app:connectedDebugAndroidTest
```

The first command is the gate on every slice: it must report 42 passing tests before new ones are added.

## 14. Rollout Order

| Step | Content | Verifiable at this point |
|---|---|---|
| S0 | Build plumbing: plugin, BOM, auth/firestore/work/cloudinary deps, `INTERNET`, FileProvider, Application, BuildConfig fields, `AppContainer` skeleton | `assembleDebug` + 42 tests green; second build reuses configuration cache |
| S1 | Interfaces + Room v3 + `InspectionRepositoryImpl` with sync disabled; VM constructor untouched | 42 tests green with only the test import changed; app still works offline-only |
| S2 | Auth: data source, error mapping, validators, profile cache, session restore, login/register/logout | Register → kill → relaunch offline shows Dashboard; wrong password → `INVALID_CREDENTIALS` message |
| S3 | Inspection sync: mappers, data source, mirror, workers, rules + indexes | Save offline shows "Pendiente"; after reconnect "Sincronizado"; a second account/device sees the doc; rules reject the other uid |
| S4 | Evidence: capture, compression, Cloudinary, upload worker, remote preview/PDF, UI states | Airplane-mode capture → reboot → reconnect uploads without action; PDF embeds both local and remote evidence |

S2 and S3 are independent after S1; S4 depends on S3's document id and on the evidence table.

## 15. Risks and Tradeoffs

| Risk | Impact | Mitigation / rejected alternative |
|---|---|---|
| Firebase BoM vs Kotlin `2.2.10` metadata | `compileDebugKotlin` rejects the classpath | Resolved at S0: BoM pinned to `34.12.0` (`firebase-auth` 24.0.1, metadata 2.1.0). BoM `34.13.0+`/`35.x` rejected (auth 24.1.0+/25.0.0 ship 2.3.0/2.4.0); Kotlin bump rejected because KSP has no 2.4.x release |
| `work-runtime` version `UNVERIFIED` | Wrong/incompatible artifact | Resolve latest stable from Google Maven at S0 and record it; no version invented here |
| Destructive Room v2→v3 wipes local inspections and users | Data loss for existing installs | Accepted clean start (proposal); rejected a hand-written `Migration(2,3)` because the PK moves from `folio` to `inspectionId` and every row needs a UUID backfill |
| Firestore rules/indexes live outside the build | Green build, broken device | Deliver `firestore.rules` + `firestore.indexes.json`; verify in the Emulator Suite |
| Unsigned Cloudinary preset is a public upload credential | Abuse of the account | Restrict the preset (max ~5 MiB, jpg/png, no delete token); rejected a signed upload because it requires shipping a secret |
| Local dirty record loses to a remote snapshot | Silent data loss | `SyncConflictPolicy` ignores remote while `LOCAL_ONLY/PENDING/FAILED`; property-tested as a pure function |
| Two-step parent-then-incidences is not atomic | Partial document on crash | Idempotent re-runs; parent always wins on retry. Rejected single-batch because rules cannot see same-batch writes |
| `content://` FileProvider URIs are still device-local | Exported PDF on another device | Remote `https` fallback in PDF and previews; Firestore never stores `content://` |
| PDF now fetches network | Main-thread violation / slow export | `suspend` + `Dispatchers.IO`, timeouts, visible unavailable marker |
| Publishing a Firestore mirror listener adds reads | Cost | Listener only while authenticated; Room remains the read path |
| `restoreSession` is a new public VM method | Could diverge from `login` | Shared private helper; new unit test on session restore |

Rejected overall alternatives: Firestore-only (rewrites every read path and the tests, exploration
§Offline-First), metadata-only sync (fails the evidence requirement), full Clean Architecture rewrite
with use cases (gold-plating, breaks the 42-test contract), and Firebase Storage (Blaze requirement;
Cloudinary was the product decision).

## File Changes

| File | Action | Description |
|---|---|---|
| `app/src/main/java/.../domain/repository/*.kt` | Create | Three domain interfaces with the exact VM-visible signatures |
| `app/src/main/java/.../domain/error/*.kt` | Create | `AuthError`, `AuthException`, `SyncError`, `EvidenceError` |
| `app/src/main/java/.../domain/validation/*.kt` | Create | `AuthValidator`, `SyncConflictPolicy`, `EvidenceBudget` |
| `app/src/main/java/.../domain/model/Evidence.kt` | Create | `Evidence`, `EvidenceStatus`, deterministic id helper |
| `app/src/main/java/.../domain/model/SyncState.kt` | Create | `LOCAL_ONLY`, `PENDING`, `SYNCED`, `FAILED` |
| `app/src/main/java/.../domain/model/AuthSession.kt` | Create | `AuthSession(uid, email)` |
| `app/src/main/java/.../domain/model/InspectionSession.kt` | Modify | `inspectionId`, `ownerUid`, `syncState`, `updatedAt` |
| `app/src/main/java/.../domain/model/Incidence.kt` | Modify | `inspectionId` |
| `app/src/main/java/.../domain/model/User.kt` | Modify | `uid: String = ""` |
| `app/src/main/java/.../data/repository/InspectionRepository.kt` | Delete → Create impl | Becomes `InspectionRepositoryImpl` behind the interface |
| `app/src/main/java/.../data/repository/FirebaseAuthRepository.kt` | Create | `AuthRepository` over `FirebaseAuthDataSource` |
| `app/src/main/java/.../data/repository/DefaultEvidenceRepository.kt` | Create | Evidence rows + upload enqueue |
| `app/src/main/java/.../data/remote/{mapper,firestore,auth,cloudinary}/` | Create | DTO maps, Firestore/Auth data sources, Cloudinary uploader |
| `app/src/main/java/.../data/sync/*.kt` | Create | `SyncCoordinator`, scheduler, two workers, `CloudMirror` |
| `app/src/main/java/.../data/local/entity/{Inspection,Incidence}Entity.kt` | Modify | New PK/columns; `UserEntity`/`UserDao` replaced by `ProfileEntity`/`ProfileDao` |
| `app/src/main/java/.../data/local/dao/{InspectionDao,IncidenceDao}.kt` | Modify | `deleted = 0` filters, lookup by `inspectionId`, owner-scoped queries |
| `app/src/main/java/.../data/local/{entity/EvidenceEntity,dao/EvidenceDao,dao/ProfileDao}.kt` | Create | Evidence queue + profile cache |
| `app/src/main/java/.../data/local/AppDatabase.kt` | Modify | v3, new tables, drop seed user |
| `app/src/main/java/.../data/mapper/*.kt` | Modify | Map new fields; delete `UserMapper` → `ProfileMapper` |
| `app/src/main/java/.../ui/viewmodel/InspectionViewModel.kt` | Modify | Session UUID, evidence registration, sync retry, `restoreSession`, error mapping |
| `app/src/main/java/.../ui/viewmodel/ViewModelFactory.kt` | Modify | Interface parameter type |
| `app/src/main/java/.../MainActivity.kt` | Modify | Reads `AppContainer` from the Application; auth-aware navigation |
| `app/src/main/java/.../SecurityInspectionsApp.kt`, `AppContainer.kt` | Create | Application + composition root |
| `app/src/main/java/.../util/{EvidenceStorage,EvidencePreparer,RemoteImageLoader}.kt` | Create | Capture file, compression, https decode |
| `app/src/main/java/.../util/InspectionPdfGenerator.kt` | Modify | `suspend`, https resolution, unavailable marker |
| `app/src/main/java/.../ui/components/EvidenceImage.kt` | Create | Local/remote evidence preview |
| `app/src/main/java/.../ui/screens/{ChecklistModuleScreen,ChecklistScreen,CriticalStopDialog}.kt` | Modify | `TakePicture` + FileProvider |
| `app/src/main/java/.../ui/screens/{ResultsScreen,InspectionDetailScreen,SummaryScreen,HistorialScreen}.kt` | Modify | `EvidenceImage`, sync chip, retry |
| `app/src/main/java/.../ui/screens/LoginScreen.kt` | Modify | Remove demo prefill; show mapped errors |
| `app/src/main/AndroidManifest.xml` | Modify | `INTERNET`, Application, FileProvider |
| `app/src/main/res/xml/file_paths.xml` | Create | `files-path` for evidence |
| `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `local.properties` | Modify | Versions, plugin, BuildConfig fields, WorkManager |
| `firestore.rules`, `firestore.indexes.json` | Create | Rules + composite index |
| `app/src/test/java/.../ui/viewmodel/InspectionViewModelTest.kt` | Modify (1 line) | Interface import only |

## Data Flow

Save → sync:

```
Results ──saveDictamen()──► VM (UUID id, score/stop/85% unchanged)
                          │
                          ▼
             InspectionRepositoryImpl.saveInspection
                │ Room insert (LOCAL_ONLY)
                ▼
             SyncCoordinator ──► SyncScheduler ──► WorkManager (unique, CONNECTED, backoff ×5)
                                    │
                          set parent → batch incidences → ack
                                    │
                                    ▼
                          Room syncState = SYNCED ──► UI chip
```

Evidence → upload → document:

```
Capture ──► EvidenceStorage file ──► Evidence row (PENDING)
                                        │  EvidenceUploadWorker
                                        ▼
                              Cloudinary unsigned upload
                                        │ secure_url
                              ┌─────────┴──────────┐
                              ▼                    ▼
                    evidence.remoteUrl      enqueue inspection sync
                              │                    │
                              ▼                    ▼
                    PDF / preview https   Firestore findings[].photoUri
```

Auth → navigation:

```
Splash ──► AuthRepository.session (callbackFlow)
             │ non-null                  │ null
             ▼                           ▼
   restoreSession(profile cache)      Login
             ▼
         Dashboard
```

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or
process-integration boundary. The only external boundary is Firebase/Cloudinary HTTP, covered by the
rules file and the unsigned-preset constraint above.

## Open Questions

- [ ] `androidx.work:work-runtime` version is `UNVERIFIED`; resolve at S0 and pin it.
- [ ] `cloudinary-android:3.1.3` API shape (`MediaManager.init` overloads, whether an unsigned preset
      accepts a slash-bearing `public_id`) is `UNVERIFIED`; validate on the first upload, with preset
      `folder` + bare `public_id` as the fallback.
- [ ] `google-services` 4.5.0 against AGP 9.4.1 / Gradle 9.6.0 is `UNVERIFIED`; S0 is the proof.
- [ ] Firestore persistent-cache defaults are `UNVERIFIED` (research was blocked); the design does not
      depend on them because Room is the source of truth.
- [ ] Is a Firebase Emulator smoke test in scope, or is two-device manual verification the acceptance
      path? Rules/indexes can only be trusted with one of the two.
