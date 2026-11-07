# Proposal: Firebase Cloud Persistence

## Intent

Inspections are single-device and paper-backed: evidence dies with the phone, with no cross-device audit trail. Persist inspections and evidence in the cloud so every device sees one durable record.

## Scope

**In**
- Firebase Auth Email/Password; delete local `users` and plaintext passwords.
- Domain repository interfaces; Room is the UI source of truth, Firestore the replica.
- Cloudinary upload on capture plus WorkManager retries.
- Domain validation, DTO/mapper separation, rules, explicit `INTERNET`.

**Out**
- Firebase Storage, Google Sign-In, anonymous auth.
- Admin/backoffice, legacy migration, PDF redesign.
- Changes to scoring, Critical Stop, or 85%–24h semantics.

## Capabilities

**New:** `cloud-inspection-sync`, `firebase-email-auth`, `cloud-evidence-upload`.
**Modified:** None (`openspec/specs/` empty; scoring untouched).

## Approach

- **Domain**: `InspectionRepository`, `AuthRepository`, `EvidenceRepository`; framework-free models; validators own field, ownership, and `AuthError` rules.
- **Data**: `data/remote/entity` DTOs, `data/remote/mapper`, `data/remote/datasource`; Room cache; sync coordinator decides pushes.
- **Presentation**: ViewModel depends on interfaces only; UI never imports Firebase or DAOs.
- **42 tests**: signatures unchanged; tests mock `InspectionRepository` (`InspectionViewModelTest.kt:32-38`), so only the interface import moves. New tests use fakes below that boundary.

## Correctness Baselines

| Issue | Required outcome |
|---|---|
| Device-local `photoUri` (`GalleryUtils.kt:26`) | Cloudinary `secure_url` in the model; PDF resolves remote or local |
| Folio from local rows (`InspectionViewModel.kt:550-554`) | Firestore id is UUID/server-generated; `folio` is display |
| No `Incidence.inspectionId` (`Incidence.kt:7-16`) | Stable parent id or subcollection |
| `INTERNET` transitive only (`AndroidManifest.xml:5-11`) | Declared explicitly |

## Affected Areas

| Area | Impact | Note |
|---|---|---|
| Build | Modified | Plugin, BOM, WorkManager |
| `domain/*`, `data/remote/*` | New | Interfaces, rules, DTOs, mappers |
| `data/*`, Room | Modified | Impl, cache, v2→v3 |
| `ui/*`, capture, PDF | Modified | VM uses interfaces; `SampleData` fix |
| Rules/indexes | New | Deployed artifacts |

Deferred honestly: splitting the god-object ViewModel and moving scoring rules.

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Room wipe (`AppDatabase.kt:22,41`) | High | Clean start; destructive migration intentional, not a violation |
| Plaintext passwords | Med | Recreate accounts in Auth |
| JSON-blob Firestore mapping | Med | Explicit `Map<String, Any?>` DTOs |
| `org.json` mappers untestable | Med | Remote mappers stay `org.json`-free |
| WorkManager + configuration cache | Med | Validate build early |
| `compliance-score-reinspection` open | Med | Untouched; additive specs |

## Rollback Plan

Revert build, manifest, and repository wiring; keep Room unchanged; delete or ignore Firestore and Cloudinary artifacts. No migrated data needs restoring.

## Dependencies

- `firebase-bom:35.1.0`, `firebase-auth`, `firebase-firestore` (no `-ktx`), `google-services:4.5.0`, `cloudinary-android:3.1.3` — orchestrator notes, not research evidence.
- `androidx.work:work-runtime` and any image loader: `UNVERIFIED`.
- `google-services.json` exists for `seguridad-85edc`; Cloudinary `f2ge9jhj` + unsigned `ml_default`; no API secret.

## Success Criteria

- [ ] A second device sees an inspection saved on the first.
- [ ] An offline photo reaches Cloudinary after reconnect.
- [ ] A Firebase Auth user can log in.
- [ ] The 42 tests stay green.

## Open Questions

- Firestore visibility: per-inspector private or shared company read?
- Incidences: subcollection or top-level with parent id?
- Full-resolution capture, or keep thumbnails?
- Emulator smoke test, or manual verification?
