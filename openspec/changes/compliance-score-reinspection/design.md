# Design: Compliance Score & Reinspection Scheduling

## Technical Approach

Option B (proposal): score/threshold/24h rules as pure functions in `InspectionViewModel`; reinspection persisted to a new Room `inspections` entity via `confirmInspection()`, fired once on Summary exit; Dashboard chip data-driven from one DAO query; `initChecklist` resets sticky state. Persisted score is recomputed from source data (`currentChecklist`+`answers`) at confirm time so "no answers → 0" holds despite the state default of 100.

## Architecture Decisions

| # | Options considered | Tradeoff | Decision |
|---|---|---|---|
| D1 | private / internal rules | internal = testable from same-module JVM tests, zero public API | `calculateScore` → internal; new internal `isReinspectionDue(score)`, `scheduleReinspection(nowMillis)`, consts `REINSPECTION_THRESHOLD=85`, `REINSPECTION_WINDOW_MS=86_400_000L` |
| D2 | `score<85` in composables / derived state | config bans rules in composables | `reinspectionDue: Boolean` in `InspectionUiState`, set in `submitAnswer`; screens read it; delete literal-85 strings and dead `isApproved` |
| D3 | VM imports `InspectionModule` / takes strings | strings decouple VM from screens pkg | `confirmInspection(moduleCode: String, moduleTitle: String)` |
| D4 | UI guard / `isSaving` VM guard | VM guard testable, survives recomposition | early-return if `isSaving`; navigate regardless of write result (error → `errorMessage`) |
| D5 | trust state score / recompute | state stale until first answer | recompute `calculateScore(checklist, answers)` inside `confirmInspection` |
| D6 | schema export off / on | on = drift detection + future auto-migrate; +1 generated JSON, +ksp arg | `exportSchema=true`, `room.schemaLocation=$projectDir/schemas`; commit `2.json` (generated, excluded from authored line budget) |
| D7 | latest overall / latest with schedule | handoff pins "most recent with non-null schedule" | `SELECT * FROM inspections WHERE scheduledReinspectionAt IS NOT NULL ORDER BY createdAt DESC LIMIT 1` → `Flow<Inspection?>`; null → chip hidden |
| D8 | interface+impl / concrete 2-DAO class | repo final+thin; MockK mocks DAOs directly | `InspectionRepository(incidenceDao, inspectionDao)` + entity↔domain mappers (existing pattern) |
| D9 | `Screen.Summary` data object / carries module | exit handler needs module to persist | `Screen.Summary(val module: InspectionModule)` |

## Data Flow

```
Checklist → submitAnswer → [answers, score, reinspectionDue, criticalStop] in state
Summary exit → confirmInspection(moduleCode, moduleTitle)
  score=calculateScore(...); scheduledAt = if (isReinspectionDue(score)) scheduleReinspection(now) else null
  Inspection → repository.saveInspection → InspectionDao → Room `inspections` (v2)
Dashboard ← repository.getLatestPendingReinspection(): Flow<Inspection?> → chip RecentItem | hidden
```

## File Changes

| File | Action | Description |
|---|---|---|
| `ui/viewmodel/InspectionViewModel.kt` | Modify | D1+D2+D4+D5: pure funs, reset, `confirmInspection` |
| `ui/screens/ChecklistScreen.kt` | Modify | score card reads `reinspectionDue` |
| `ui/screens/SummaryScreen.kt` | Modify | status from `reinspectionDue`; remove `isApproved` |
| `MainActivity.kt` | Modify | D9, 2nd DAO, confirm-on-exit, collect pending flow |
| `data/local/Entities.kt` | Modify | `InspectionEntity` |
| `data/local/Daos.kt` | Modify | `InspectionDao` |
| `data/local/AppDatabase.kt` | Modify | v2, exportSchema, `MIGRATION_1_2`, `addMigrations` |
| `domain/models/Inspection.kt` | Create | domain model (mirrors `Incidence`) |
| `data/repository/InspectionRepository.kt` | Modify | persist/query + mappers |
| `ui/screens/DashboardScreen.kt` | Modify | D7 conditional chip |
| `gradle/libs.versions.toml` + `app/build.gradle.kts` | Modify | test deps + schemaLocation |
| `app/src/test/.../InspectionViewModelTest.kt` | Create | rule/reset/confirm tests |
| `app/src/test/.../InspectionRepositoryTest.kt` | Create | mapper + mocked-DAO tests |

## Interfaces / Contracts

```kotlin
@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey val id: String,
    val moduleCode: String,
    val moduleTitle: String,
    val score: Int,
    val criticalStopTriggered: Boolean,
    val scheduledReinspectionAt: Long?, // null ⇒ no reinspection
    val createdAt: Long
)
```
```sql
-- MIGRATION_1_2: additive only; `incidences` untouched ⇒ data preserved.
CREATE TABLE IF NOT EXISTS `inspections` (`id` TEXT NOT NULL, `moduleCode` TEXT NOT NULL, `moduleTitle` TEXT NOT NULL, `score` INTEGER NOT NULL, `criticalStopTriggered` INTEGER NOT NULL, `scheduledReinspectionAt` INTEGER, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))
```
```kotlin
@Dao
interface InspectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: InspectionEntity): Long
    @Query("SELECT * FROM inspections WHERE scheduledReinspectionAt IS NOT NULL ORDER BY createdAt DESC LIMIT 1")
    fun getLatestPendingReinspection(): Flow<InspectionEntity?>
}
```
```kotlin
fun initChecklist(items: List<ChecklistItem>) { _uiState.value = InspectionUiState(currentChecklist = items) }
// → answers=∅, criticalStop=false, score=100, reinspectionDue=false, isSaving=false
```

## Testing Strategy

| Layer | What | How |
|---|---|---|
| Unit | floor 9/13→69; empty→100; none→0; false/absent→0 | pure `calculateScore` (VM + mock repo) |
| Unit | 84→due, 85→not; schedule +86,400,000 deterministic | `isReinspectionDue` / `scheduleReinspection` fixed nowMillis |
| Unit | critical-stop independent: 7 HIGH, 1 unanswered → score 85 (floor), stop=true, due=false | `submitAnswer` path |
| Unit | confirm low→schedule, high→null; `scheduledReinspectionAt == createdAt+24h` (clock-free); double confirm → 1 save | MockK `coEvery`/`coVerify` |
| Unit | sticky reset: answers+stop+score → `initChecklist` → ∅/false/100/false | state assertions |
| Unit | mappers + null flow | real repo, mocked DAOs |

Harness: `Dispatchers.setMain(StandardTestDispatcher())` + `runTest`. Deps (Kotlin 2.2.10/JUnit4): `kotlinx-coroutines-test` 1.11.0 (Kotlin 2.2.20 metadata — readable by 2.2.10; fallback 1.10.2), `mockk` 1.14.11 (fallback 1.13.17).

## Threat Matrix

`N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.`

## Migration / Rollout

Additive-only v1→v2, no destructive fallback; verify manually on a device with existing incidences (integration disabled in config → no `MigrationTestHelper` here; possible follow-up once `2.json` committed). Rollback: revert PR; DB stays v2 (dev reinstall to downgrade).

## PR Chain (3 slices, each < 400 authored lines)

1. `feat(compliance): score rule + state reset + derived status` — D1/D2: pure funs, reset, UI magic-number removal, rule tests (~230).
2. `feat(compliance): persist inspections + room v1→v2` — D3-D6/D8/D9: entity/DAO/migration/repo, `confirmInspection`, `Screen.Summary(module)`, persistence tests + schema JSON (~290).
3. `feat(dashboard): data-driven reinspection chip` — D7: conditional chip from `Flow<Inspection?>`, MainActivity collection (~80).

## Open Questions

- [ ] Overdue schedule (past date): pinned = visible whenever non-null; time-window "pending" semantics deferred.
- [ ] System back on Summary bypasses confirm (button is sole exit today); accepted, noted for verify.