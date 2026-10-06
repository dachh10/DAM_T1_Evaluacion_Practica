# Tasks: Compliance Score & Reinspection Scheduling

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~600 authored (excludes generated schema `2.json`) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 rule+reset → PR 2 persistence → PR 3 chip |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

**Chain strategy decision (user picks; not fixed here):**
- **stacked-to-main**: PR 1→main, PR 2→main (rebase/retarget), PR 3→main. Fast; slices ship independently.
- **feature-branch-chain**: draft tracker `feat/compliance-reinspection`; PR 1 base=tracker, PR 2 base=PR 1 branch, PR 3 base=PR 2 branch; tracker merges last. Rollback control.

Note: `openspec/config.yaml` forbids git init/commit in this repo — `sdd-apply` performs file work only; PR creation is follow-up once remotes exist. Definition of done: `sdd-apply` ticks each `[x]`; a slice is complete when all its tasks are ticked and its focused test passes.

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Score rule, reset, derived status | PR 1 (~230) | `./gradlew test --tests "*InspectionViewModelTest*"` | N/A: pure JVM rule tests, no device state | Revert `InspectionViewModel.kt`, `ChecklistScreen.kt`, `SummaryScreen.kt`, test deps |
| 2 | Persistence, v2 migration, confirm | PR 2 (~300) | `./gradlew test --tests "*Inspection*Test*"` | Manual: confirm score<85 on emulator; verify `inspections` row | Revert entity/DAO/migration/repo/`confirmInspection`; DB stays v2 (reinstall to downgrade) |
| 3 | Data-driven chip | PR 3 (~80) | `./gradlew test` | Manual: score<85 → chip visible; ≥85 → hidden | Revert `DashboardScreen.kt` + MainActivity collection |

## Phase 1: Test Foundation

- [x] 1.1 Add `kotlinx-coroutines-test` 1.11.0 (fallback 1.10.2) and `mockk` 1.14.11 (fallback 1.13.17) to `gradle/libs.versions.toml`
- [x] 1.2 Add `testImplementation(libs.kotlinxCoroutinesTest)` and `testImplementation(libs.mockk)` to `app/build.gradle.kts`

## Phase 2: Slice 1 — Score Rule, Reset, Derived Status

- [x] 2.1 RED: create `app/src/test/java/com/example/dam_t1_evaluacion_practica/ui/viewmodel/InspectionViewModelTest.kt`: 9/13→69 floor, only-true counts, all→100, none→0, empty→100
- [x] 2.2 RED: same file: 84→due, 85→not due; 7 HIGH/1 unanswered→score 85, stop=true, due=false; `initChecklist` clears answers/stop/score
- [x] 2.3 GREEN: `InspectionViewModel.kt` — `calculateScore` internal; `isReinspectionDue(score)` internal; consts `REINSPECTION_THRESHOLD=85`, `REINSPECTION_WINDOW_MS=86_400_000L`; `reinspectionDue` in `InspectionUiState`, set in `submitAnswer`; stop derived from HIGH items with `answers[id] != true`
- [x] 2.4 GREEN: `initChecklist` resets `_uiState.value = InspectionUiState(currentChecklist = items)`
- [x] 2.5 `ChecklistScreen.kt` — `InspectionScoreCard` reads `uiState.reinspectionDue`; delete literal `< 85` strings (lines 176, 182)
- [x] 2.6 `SummaryScreen.kt` — status color/title from `reinspectionDue`; delete dead `isApproved` (line 62) and literal `< 85`

## Phase 3: Slice 2 — Persistence, Migration, Confirm

- [ ] 3.1 Create `domain/models/Inspection.kt` (mirrors `Incidence` pattern)
- [ ] 3.2 `data/local/Entities.kt` — add `InspectionEntity` (table `inspections`, nullable `scheduledReinspectionAt`)
- [ ] 3.3 `data/local/Daos.kt` — `InspectionDao`: `insertInspection` (REPLACE), `getLatestPendingReinspection(): Flow<InspectionEntity?>` (D7 query)
- [ ] 3.4 `data/local/AppDatabase.kt` — version 2, `exportSchema=true`, `inspectionDao()`, `MIGRATION_1_2` (design SQL), `addMigrations`
- [ ] 3.5 `app/build.gradle.kts` — ksp arg `room.schemaLocation=$projectDir/schemas`; commit generated `schemas/.../2.json` (excluded from authored budget)
- [ ] 3.6 `data/repository/InspectionRepository.kt` — 2-DAO constructor, `saveInspection`, `getLatestPendingReinspection`, entity↔domain mappers
- [ ] 3.7 RED: extend `InspectionViewModelTest.kt` — confirm 84→`scheduledReinspectionAt==now+24h`, 85→null, double confirm→1 save, `scheduleReinspection` deterministic
- [ ] 3.8 RED: create `app/src/test/java/com/example/dam_t1_evaluacion_practica/data/repository/InspectionRepositoryTest.kt` — mappers + null flow with MockK DAOs
- [ ] 3.9 GREEN: `InspectionViewModel.kt` — `confirmInspection(moduleCode, moduleTitle)` exactly-once (`isSaving` guard), recompute `calculateScore(checklist, answers)`, persist, error→`errorMessage`
- [ ] 3.10 `MainActivity.kt` — `Screen.Summary(val module)` data class; Checklist branch passes module; Summary exit calls `confirmInspection` then navigates; wire `inspectionDao()`

## Phase 4: Slice 3 — Dashboard Chip

- [ ] 4.1 `DashboardScreen.kt` — `RecentInspectionsSection`/chip parameterized by `pendingReinspection: Inspection?`: data-driven "Reinspeccion 24h" chip when non-null, hidden otherwise; rest mock unchanged
- [ ] 4.2 `MainActivity.kt` — collect `getLatestPendingReinspection()` via `collectAsState(initial = null)`; pass to `DashboardScreen`

## Phase 5: Verification & Cleanup

- [ ] 5.1 `./gradlew test` green; `./gradlew :app:assembleDebug` compiles; `:app:lint` clean
- [ ] 5.2 Manual: score<85 inspection → confirm → chip visible on Dashboard; score≥85 → hidden
- [ ] 5.3 Manual: v1 DB with incidences survives upgrade; `inspections` table exists (no destructive fallback)