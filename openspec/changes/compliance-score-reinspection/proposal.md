# Proposal: Compliance Score & Reinspection Scheduling

## Intent

Enforce the real risk rule: when a zone's compliance score falls below 85%, the app MUST automatically schedule a reinspection 24 hours later. Today the rule is only duplicated UI strings (ChecklistScreen 176/182, SummaryScreen 62-72), the Dashboard "Reinspeccion 24h" chip is hardcoded mock, inspections are never persisted, and ViewModel state leaks across inspections. This change makes the rule a testable ViewModel-layer behavior with persisted scheduling.

## Scope

### In Scope
- Pin score semantics; extract rule to pure internal functions in the ViewModel layer (config rule).
- Strict `<85` threshold: `confirmInspection()` schedules reinspection at now+24h, persisted in a new Room `inspections` entity; DB v1→v2 with explicit `Migration(1,2)`.
- Reset sticky state in `initChecklist` (prerequisite; required for reinspection correctness).
- Dashboard: ONLY the "Reinspeccion 24h" chip becomes data-driven; rest stays mock.
- Remove duplicated 85% UI strings and dead `isApproved`; read status from state.
- Test stack: `kotlinx-coroutines-test` + MockK; unit tests for score, critical stop, threshold, scheduling, reset.

### Out of Scope
- Full data-driven Dashboard (follow-up `dashboard-real-data`).
- `inspectionId` FK on incidences / per-inspection grouping (follow-up, needs scoping decision).
- Multi-zone aggregation; changing rounding (flooring kept); per-zone model beyond the single active checklist.

## Capabilities

No existing specs under `openspec/specs/`. All capabilities NEW (full specs in spec phase):
- `compliance-scoring`: weighted score (HIGH=5/MEDIUM=3/LOW=1), floor division, unanswered=non-compliant, empty→100, status derivation, strict `<85`.
- `reinspection-scheduling`: confirm flow, 24h rule, `inspections` entity + v2 migration, DAO/repo queries, data-driven chip, state reset at start.

## Approach

**A vs B decision: B.** Rule as pure functions in ViewModel layer + persisted `inspections` entity written by `confirmInspection()`, invoked from SummaryScreen exit. A (VM-only) rejected: chip stays mock, feature invisible, lost on process death. C (column on incidences) rejected: wrong modeling, N ambiguous schedules. D (repository) rejected: violates config "rules in ViewModel". Scheduling triggers on `score < 85` only; critical stop stays an independent gate. `scheduleReinspection(nowMillis)` is pure → deterministic tests.

## Affected Areas

| Area | Impact | Change |
|---|---|---|
| `ui/viewmodel/InspectionViewModel.kt` | Modified | Pure rule functions, `confirmInspection`, reset |
| `data/local/Entities.kt` | Modified | New `InspectionEntity` |
| `data/local/Daos.kt` | Modified | `InspectionDao` + queries |
| `data/local/AppDatabase.kt` | Modified | v2 + `Migration(1,2)` |
| `data/repository/InspectionRepository.kt` | Modified | Inspection persist/query |
| `ui/screens/ChecklistScreen.kt` | Modified | Read status from state |
| `ui/screens/SummaryScreen.kt` | Modified | Read status; confirm on exit; remove dead `isApproved` |
| `ui/screens/DashboardScreen.kt` | Modified | Real reinspection chip only |
| `MainActivity.kt` | Modified | Wire confirm before navigating home |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Modified | Test deps |
| `app/src/test/` | New | Unit tests |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Room v1→v2 migration (no schema export) | Med | Explicit `Migration(1,2)`; never destructive fallback (user data) |
| Budget > 400 review lines | Med | Chain PRs: 1) rule+reset+tests, 2) persistence, 3) chip |
| Score floor/rounding semantic drift vs current UI | Low | Keep flooring; pin 9/13→69 in tests |
| SummaryScreen shows global incidences | Low | Out of scope; note follow-up |

## Rollback Plan

Revert the change commit. DB stays v2 (dev stage: uninstall/reinstall to downgrade); no destructive migration shipped.

## Dependencies

- Room 2.6.1 (present). Add `kotlinx-coroutines-test` + MockK (test-only).

## Success Criteria

- [ ] Unit tests pass: floor score (9/13→69), threshold (84→due, 85→not due), critical stop, reset, 24h scheduling.
- [ ] `confirmInspection()` with score<85 persists `InspectionEntity` with `scheduledReinspectionAt ≈ now+24h`; score≥85 persists without schedule.
- [ ] Dashboard chip renders persisted reinspection; hidden when none pending.
- [ ] Existing `submitAnswer` incidence persistence unchanged (covered by test).