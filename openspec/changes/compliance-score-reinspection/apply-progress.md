# Apply Progress: Compliance Score & Reinspection Scheduling — Slice 1

**Change**: compliance-score-reinspection
**Slice**: 1 of 3 (chained PRs) — "Test Foundation" + "Score Rule, Reset, Derived Status"
**Phase executor**: sdd-apply (Strict TDD Mode, `./gradlew test`)
**Date**: 2026-09-19
**Status**: SLICE 1 COMPLETE — tasks 1.1, 1.2, 2.1–2.6 done; focused and full unit suites green; `:app:assembleDebug` compiles

## Completed Tasks (this batch)

- [x] 1.1 Add `kotlinx-coroutines-test` 1.11.0 (fallback 1.10.2) and `mockk` 1.14.11 (fallback 1.13.17) to `gradle/libs.versions.toml`
- [x] 1.2 Add `testImplementation(libs.kotlinxCoroutinesTest)` and `testImplementation(libs.mockk)` to `app/build.gradle.kts`
- [x] 2.1 RED: create `app/src/test/.../ui/viewmodel/InspectionViewModelTest.kt`: 9/13→69 floor, only-true counts, all→100, none→0, empty→100
- [x] 2.2 RED: same file: 84→due, 85→not due; 7 HIGH/1 unanswered→score 85, stop=true, due=false; `initChecklist` clears answers/stop/score
- [x] 2.3 GREEN: `InspectionViewModel.kt` — `calculateScore` internal; `isReinspectionDue(score)` internal; consts `REINSPECTION_THRESHOLD=85`, `REINSPECTION_WINDOW_MS=86_400_000L`; `reinspectionDue` in `InspectionUiState`, set in `submitAnswer`; stop derived from HIGH items with `answers[id] != true`
- [x] 2.4 GREEN: `initChecklist` resets `_uiState.value = InspectionUiState(currentChecklist = items)`
- [x] 2.5 `ChecklistScreen.kt` — `InspectionScoreCard` reads `uiState.reinspectionDue`; delete literal `< 85` strings (lines 176, 182)
- [x] 2.6 `SummaryScreen.kt` — status color/title from `reinspectionDue`; delete dead `isApproved` (line 62) and literal `< 85`

## TDD Cycle Evidence (Strict TDD hard gate)

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1, 1.2 | N/A (config deps) | N/A | ✅ 1/1 baseline `gradlew test` | N/A (structural) | ✅ `:app:compileDebugUnitTestKotlin` resolved deps | ➖ Single (Triangulation skipped: purely structural config) | ➖ None needed |
| 2.1, 2.2, 2.3 | `InspectionViewModelTest.kt` | Unit | ✅ 1/1 pre-existing suite | ✅ Compile failure: `calculateScore` private (5×), `isReinspectionDue`/`scheduleReinspection`/`REINSPECTION_WINDOW_MS`/`reinspectionDue` unresolved | ✅ 12/12 `:app:testDebugUnitTest --tests "*InspectionViewModelTest*"` | ✅ 5 score cases, 2 threshold cases, 2 schedule cases, 2 state-level, 1 reset | ✅ Single-snapshot refactor in `submitAnswer`, suite still green |
| 2.4 | `InspectionViewModelTest.kt` (reset test) | Unit | (same suite as above) | ✅ Same compile-failure RED | ✅ 12/12 | ✅ Precondition leaves answers+stop+score, asserts ∅/false/100/false | ✅ Included in refactor pass |
| 2.5, 2.6 | `InspectionViewModelTest.kt` (state derivation covered by 2.2/2.3 tests) | Unit (UI wiring) | ✅ 13/13 full suite | ✅ Derived behavior already RED→GREEN via VM state tests (reinspectionDue in state) | ✅ `:app:testDebugUnitTest` 13/13 + `:app:assembleDebug` compiles | ✅ Threshold both sides (84 → alarm state, 85 → regular) through `reinspectionDue` | ➖ None needed |

Note on UI layer: Compose composables have no JVM unit harness in this project (no Robolectric; instrumented tests are device-only and out of SLA for this slice). The behavioral logic consumed by the UI (`reinspectionDue`, `criticalStopTriggered`) lives in `InspectionUiState` and is fully unit-tested at the ViewModel layer; the UI change is wiring + magic-number removal, proven by compilation and by the state tests.

## Work Unit Evidence (work-unit-commits hard gate)

| Unit | Focused test command and exact result | Runtime harness command/scenario and exact result | Rollback boundary |
|------|--------------------------------------|---------------------------------------------------|-------------------|
| WU-1 Test deps (1.1, 1.2) | `gradlew :app:compileDebugUnitTestKotlin` → BUILD SUCCESSFUL (deps resolved) | N/A — config/dependency change, no runtime boundary | Revert the two added lines in `libs.versions.toml` + `app/build.gradle.kts` |
| WU-2 Rule + reset (2.1–2.4) | `gradlew :app:testDebugUnitTest --tests "*InspectionViewModelTest*"` → BUILD SUCCESSFUL, 12 tests, 0 failures | N/A — pure JVM rule/state tests, no device state required | Revert `InspectionViewModel.kt` to pre-slice state + delete `InspectionViewModelTest.kt` |
| WU-3 UI wiring (2.5, 2.6) | `gradlew :app:testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL, 13 tests, 0 failures | N/A automated — Compose UI needs device; MANUAL on physical device: see "PARA PROBAR EN DISPOSITIVO" in delivery (user runs device tests outside this phase per workflow) | Revert `ChecklistScreen.kt`, `SummaryScreen.kt` score-card/status blocks only |

## Full Suite Result

- `gradlew test` → BUILD SUCCESSFUL
- `ExampleUnitTest`: 1/1 ✓ (pre-existing)
- `InspectionViewModelTest`: 12/12 ✓ (new)
- **Total: 13 tests, 0 failures, 0 errors, 0 skipped**
- `:app:assembleDebug`: BUILD SUCCESSFUL (app compiles)

## Files Changed (Slice 1)

| File | Action | What Was Done |
|------|--------|---------------|
| `gradle/libs.versions.toml` | Modified | Added `kotlinxCoroutinesTest = 1.11.0`, `mockk = 1.14.11` versions + libraries |
| `app/build.gradle.kts` | Modified | Added `testImplementation(libs.kotlinx.coroutines.test)` + `testImplementation(libs.mockk)` |
| `app/src/main/.../ui/viewmodel/InspectionViewModel.kt` | Modified | `calculateScore` internal; new internal `isReinspectionDue`, `scheduleReinspection`; consts `REINSPECTION_THRESHOLD=85`, `REINSPECTION_WINDOW_MS=86_400_000L`; `reinspectionDue` in `InspectionUiState` set in `submitAnswer`; Critical Stop derived from HIGH items with `answers[id] != true`; `initChecklist` full state reset |
| `app/src/main/.../ui/screens/ChecklistScreen.kt` | Modified | `InspectionScoreCard` takes and renders `reinspectionDue`; removed literal `score < 85` conditions and "(< 85%)" string |
| `app/src/main/.../ui/screens/SummaryScreen.kt` | Modified | Status color/title driven by `reinspectionDue`; removed dead `isApproved` and literal `< 85` / "< 85%" |
| `app/src/test/.../ui/viewmodel/InspectionViewModelTest.kt` | Created | 12 unit tests: floor 9/13→69, only-true, all→100, none→0, empty→100, 84→due, 85→not due, 24h schedule + determinism, critical-stop independence (7 HIGH/6 answered → 85/stop/due=false), due=true state-level (84), sticky reset |

## Behavior Change Note (intentional, spec-aligned)

Critical Stop is now DERIVED: `checklist.any { it.criticalityLevel == HIGH && answers[it.id] != true }` — an unanswered HIGH item triggers the stop, not only an answered-false one. Impact: in a checklist with HIGH items, the Critical Stop dialog may appear as soon as answers are incomplete (per approved spec scenario "unanswered HIGH → stop"). `initChecklist` resets the flag so a fresh inspection starts without stop.

## Deviations from Design

None — implementation matches design.md (D1, D2, reset semantics). `scheduleReinspection` was implemented in this slice per the apply handoff (it is part of the pure rule set); the deterministic-schedule scenario test was included here, while the confirm-flow scheduling tests remain slice 2 (task 3.7).

## Out of Scope (deferred to slices 2/3 — NOT implemented)

- Room `inspections` entity, DAO, DB v2 migration, repository extension, `confirmInspection`, `Screen.Summary(module)` — slice 2 (tasks 3.1–3.10)
- Dashboard data-driven chip — slice 3 (tasks 4.1–4.2)
- Verification & cleanup 5.1–5.3 run at change level after slice 3

## PR Boundary

- Mode: chained PR slice 1 of 3 (~230 authored lines, within 400 budget)
- Start: baseline with test deps absent, rules private, UI literals
- Finish: rules internal + reset + derived state + UI reads state; unit suite 13/13 + app compiles
- Dependency: PR 2 (persistence) builds on this slice's internal rule functions