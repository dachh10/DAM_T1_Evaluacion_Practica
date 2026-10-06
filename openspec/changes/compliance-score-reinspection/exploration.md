# Exploration: Compliance Score & Reinspection Scheduling

Change: `compliance-score-reinspection`
Scope: read-only codebase mapping. No implementation decisions finalized — options with tradeoffs for the proposal phase.

## Current State

### 1. Compliance score computation
- **Only computation site**: `InspectionViewModel.calculateScore(items, answers)` (`app/src/main/java/.../ui/viewmodel/InspectionViewModel.kt`, lines 75-81).
  - Weighted percent: `HIGH=5, MEDIUM=3, LOW=1` (`weightFor`, lines 83-87).
  - `(obtainedWeights * 100) / totalWeights` — **integer division, floors** (e.g. 9/13 → 69, not 69.2).
  - Unanswered items count as NON-compliant (only `answers[it.id] == true` contributes).
  - Empty checklist → returns 100.
- Stored in `InspectionUiState.overallComplianceScore: Int = 100` (line 20), recomputed on every `submitAnswer` (line 49).
- **No per-zone/per-module scoring exists** — the score is always computed over the single active checklist (`currentChecklist`), which holds one module's items. There is no multi-zone aggregation anywhere.

### 2. The 85% threshold (duplicated magic numbers, UI-only)
| File | Line(s) | Usage |
|---|---|---|
| `ui/screens/ChecklistScreen.kt` | 176, 182 | `InspectionScoreCard`: color + label `"ESTADO: ALERTA DE PONDERACIÓN (< 85%)"` |
| `ui/screens/SummaryScreen.kt` | 62 | `isApproved = !criticalStopTriggered && score >= 85` — **computed but NEVER used (dead code)** |
| `ui/screens/SummaryScreen.kt` | 66, 72 | statusColor + statusTitle `"OBSERVADA - PONDERACIÓN < 85%"` |

The rule is not enforced anywhere, not persisted, and not unit-testable (lives in composables). `openspec/config.yaml` apply guideline states business rules must live in the ViewModel layer — the threshold currently violates that convention.

### 3. The "Reinspeccion 24h" placeholder
- `ui/screens/DashboardScreen.kt` line 291: a **hardcoded** `RecentItem` chip in `RecentInspectionsSection` — "Area de Pintura y Quimicos", `chipText = "Reinspeccion 24h"`, `norm = "NOM-005-STPS Manejo de quimicos (82%)"`. Purely cosmetic; no wiring to any state, DAO, or ViewModel. The whole Dashboard is static mock UI (hardcoded dates, "91.4%", "68%", etc.).

### 4. Data model (Room)
- **Single entity**: `IncidenceEntity` (`data/local/Entities.kt`, table `incidences`): `id`, `checklistItemId`, `status: String`, `timestamp`, `latitude/longitude?`, `photoUri?`, `notes?`.
- **Single DAO**: `IncidenceDao` (`data/local/Daos.kt`): `getAllIncidences(): Flow<List<IncidenceEntity>>`, `insertIncidence(...)`.
- **DB**: `AppDatabase` version 1, `exportSchema = false`, single `incidences` table.
- **There is NO inspection entity/table, NO `scheduledReinspectionAt` column, NO `inspectionId` on incidences** — inspections themselves are never persisted; only per-answer incidences are saved (in `submitAnswer`). Incidences are not linked to the inspection that produced them.
- **Repository**: `InspectionRepository(incidenceDao)` (`data/repository/InspectionRepository.kt`) — thin mapper wrapper: `getAllIncidences()` (maps entity→domain) and `saveIncidence()` (`withContext(Dispatchers.IO)`). Concrete final class, no interface.

### 5. Navigation & save/confirm flow
- `MainActivity.kt` `Screen` sealed class: Splash → Login → Dashboard → NewInspection → ModulesSummary → Checklist(module) → Summary.
- **There is no "confirm/save inspection" action anywhere**. `ChecklistScreen.onFinishInspection` (MainActivity line 142-144) just navigates `currentScreen = Screen.Summary`. `SummaryScreen.onReturnHome` (lines 154-156) just navigates back to Dashboard. Persistence happens only per-answer via `submitAnswer → repository.saveIncidence`.
- **ViewModel state leaks across inspections**: `initChecklist(items)` (lines 33-35) only replaces `currentChecklist` — it does NOT clear `answers`, `criticalStopTriggered` (sticky), or `overallComplianceScore`. Re-running the same module starts with the previous answers, a sticky critical stop, and a stale score until the first new answer. Any reinspection feature MUST reset state at start.
- `SummaryScreen` receives ALL incidences from the repository flow (line 79 of MainActivity: `repository.getAllIncidences().collectAsState`), not just the current inspection's — counts and list are global.

### 6. Testing infrastructure
- `app/build.gradle.kts` test deps: **only** `testImplementation(libs.junit)` (JUnit 4.13.2). No `kotlinx-coroutines-test`, no MockK/Mockito.
- Test dirs exist with placeholders only: `app/src/test/.../ExampleUnitTest.kt` (`addition_isCorrect`), `app/src/androidTest/.../ExampleInstrumentedTest.kt` (package check). Zero real tests.
- ViewModel testability: `InspectionViewModel(repository)` constructor takes the concrete `InspectionRepository` — already injectable. No Android framework types in the VM (pure coroutines + lifecycle). `viewModelScope.launch` requires `Dispatchers.Main` → tests need `Dispatchers.setMain(StandardTestDispatcher)` + `runTest`/`advanceUntilIdle` from `kotlinx-coroutines-test`.
- `InspectionRepository` is final but thin; it takes the Room `IncidenceDao` interface — MockK can mock the DAO and exercise real mapper logic. No interface extraction strictly required if MockK is adopted.

## Affected Areas

| File | Impact | Why |
|---|---|---|
| `app/src/main/java/.../ui/viewmodel/InspectionViewModel.kt` | Modified | Score rule source of truth; add confirm/reset; reinspection scheduling state |
| `app/src/main/java/.../ui/viewmodel/ViewModelFactory.kt` | Unchanged | Already injects repository |
| `app/src/main/java/.../ui/screens/ChecklistScreen.kt` | Modified | Remove 85% magic numbers (read state instead) |
| `app/src/main/java/.../ui/screens/SummaryScreen.kt` | Modified | Remove 85% magic; confirm action hook; dead `isApproved` |
| `app/src/main/java/.../ui/screens/DashboardScreen.kt` | Modified | Replace hardcoded "Reinspeccion 24h" chip with real data (or keep mock if scope says out-of-scope) |
| `app/src/main/java/.../MainActivity.kt` | Modified | Wire confirm action / summary exit to ViewModel call |
| `app/src/main/java/.../data/local/Entities.kt` | Modified | New inspection entity and/or column (Room v2) |
| `app/src/main/java/.../data/local/Daos.kt` | Modified | New DAO queries for inspections / scheduled reinspections |
| `app/src/main/java/.../data/local/AppDatabase.kt` | Modified | DB version 1→2 + migration |
| `app/src/main/java/.../data/repository/InspectionRepository.kt` | Modified | Persist/query inspection + reinspection schedule |
| `app/build.gradle.kts` + `gradle/libs.versions.toml` | Modified | Add `kotlinx-coroutines-test`, MockK |
| `app/src/test/...` | New | ViewModel unit tests (score, critical stop, reinspection scheduling) |

## Approaches — Where should reinspection scheduling live?

### A. ViewModel-only (in-memory state, no persistence)
Schedule computed at confirm time, held in `InspectionUiState.scheduledReinspectionAt` (or `reinspectionDue: Boolean`).
- Pros: Minimal surface; pure unit-testable with coroutines-test; business rule (85% + 24h) stays in the ViewModel per config convention; no Room migration.
- Cons: Lost on process death/restart; Dashboard chip would stay hardcoded mock (feature invisible); does not satisfy "make the chip real".
- Effort: **Low**

### B. ViewModel rule + new Room `inspections` entity (persisted)
New entity: `id`, `module`, `score`, `complianceStatus`, `scheduledReinspectionAt?`, `createdAt` (and optionally `inspectionId` FK on incidences). DB v1→v2 migration, DAO + repository methods; ViewModel computes and persists on a new `confirmInspection()`; Dashboard reads real scheduled reinspections.
- Pros: Full feature — real persistence, real Dashboard chip, testable VM + DAO; aligns with config (rules in VM, persistence in data layer).
- Cons: Room migration (no schema export → explicit `Migration(1,2)` or `fallbackToDestructiveMigration`, which would wipe data); more files; biggest surface.
- Effort: **Medium-High**

### C. Column on existing `incidences` table
Add `scheduledReinspectionAt` to `IncidenceEntity`.
- Pros: Cheapest persistence (one column, v2 migration).
- Cons: Wrong modeling — a reinspection belongs to the inspection, not to each incidence; N incidences → N ambiguous schedules; conflicts with any future multi-zone aggregation.
- Effort: **Low-Medium** — flagged as anti-pattern; only reasonable if the change deliberately stays minimal.

### D. Rule computed in the repository
Repository derives "needs reinspection" and stores it.
- Pros: (none over B for this codebase) single call site.
- Cons: Explicitly violates `openspec/config.yaml` apply guideline "Business rules live in the ViewModel layer (do not move to UI composables / data layer)"; harder to unit test (repo→DAO mocks needed for rule logic).
- Effort: **Low** — flagged as anti-option.

### Testing approach tradeoffs (independent axis)
- **MockK + kotlinx-coroutines-test**: mock final classes and the Room DAO interface; real repository mapper logic; standard Android practice. Requires 2 dependency additions. — **Recommended**
- **Manual fakes** (no new deps): fake repository/DAO implementations; more boilerplate; avoids mocking final classes; fine but verbose.
- **Robolectric + in-memory Room**: real DB in JVM tests; heavier setup, slower; overkill for pure VM rule tests.

## Recommendation (leaning for the proposal, not a final decision)
- Move the 85% rule and compliance status into the ViewModel as state (single source of truth, unit-testable, config-compliant): e.g. `complianceStatus` enum or `reinspectionDue`/`scheduledReinspectionAt` fields derived by a pure function.
- Persist the scheduled reinspection via a **new `inspections` entity with `scheduledReinspectionAt`** (Approach B), driven from a new `confirmInspection()`/`finishInspection()` ViewModel method invoked from SummaryScreen before returning home — because the Dashboard chip (the visible consumer) can only become real with persistence.
- Add `kotlinx-coroutines-test` + MockK first (TDD is enabled in config: `strict_tdd: true`).
- Include state reset (clear `answers`, `criticalStopTriggered`, score) at inspection start — required correctness for any reinspection flow, not a nice-to-have.

## Risks
- **Room migration v1→v2**: `exportSchema = false` means no auto-migration; must ship an explicit `Migration(1,2)` or accept data loss with `fallbackToDestructiveMigration`. Decide in design phase.
- **Sticky ViewModel state**: answers/`criticalStopTriggered`/score leak across inspections; a reinspection of the same module would start dirty. Tests would encode this bug unless reset is added.
- **Integer division in `calculateScore`** floors the percent; spec must pin exact expectations (e.g. 9/13 → 69 vs 69.2 rounded) before tests are written.
- **SummaryScreen shows global incidences** (no inspection grouping): reinspection/summary counts can be wrong once multiple inspections exist; needs a scoping decision (add `inspectionId`, or keep out of scope).
- **Dashboard is 100% hardcoded mock**: replacing the chip pulls in real DB reads; scope can balloon unless the proposal explicitly bounds it (e.g. only the reinspection chip becomes data-driven).
- **Score semantics for unanswered items** (count as non-compliant) and "empty checklist → 100" are implicit today; make them explicit requirements.
- Dead code `isApproved` in SummaryScreen suggests the approval logic was started but never finished — don't build on top of it without deciding its fate.

## Ready for Proposal
**Yes.** All mapping is evidence-based (files/lines above). The proposal should pick one of A vs B (or explicit scope exclusion) for scheduling location, and the testing stack (MockK recommended).