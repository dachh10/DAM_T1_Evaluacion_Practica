# Reinspection Scheduling Specification

## Purpose

Defines when and how the app persists inspections, schedules reinspections for non-compliant inspections, reads scheduled reinspections for the Dashboard chip, and resets stale state when a new inspection starts.

## Requirements

### Requirement: Confirmation Trigger Point

The system MUST run the confirmation flow exactly once when the user finishes an inspection and leaves the Summary screen; no extra manual action is required.

#### Scenario: Confirm on summary exit

- GIVEN a completed inspection showing the Summary screen
- WHEN the user returns to the Dashboard
- THEN the confirmation flow has run exactly once

### Requirement: Schedule on Non-Compliant Confirmation

When the confirmed inspection's score is strictly below 85, the system MUST persist an inspection with `scheduledReinspectionAt` equal to the confirmation time plus exactly 24 hours, computed by the pure `scheduleReinspection(nowMillis)` function (`nowMillis + 86,400,000`).

#### Scenario: Low score schedules reinspection

- GIVEN a score of 84 confirmed at a fixed nowMillis
- WHEN the confirmation flow runs
- THEN an inspection row is persisted with `scheduledReinspectionAt == nowMillis + 24h`

#### Scenario: Deterministic schedule

- GIVEN a fixed nowMillis
- WHEN `scheduleReinspection(nowMillis)` runs twice
- THEN both results are identical (no clock dependence)

### Requirement: Compliant Confirmation Does Not Schedule

When the confirmed inspection's score is 85 or higher, the system MUST persist the inspection with a null `scheduledReinspectionAt`; no reinspection is scheduled.

#### Scenario: High score persists without schedule

- GIVEN a score of 85 confirmed
- WHEN the confirmation flow runs
- THEN an inspection row is persisted and `scheduledReinspectionAt` is null

#### Scenario: Each confirmation persists its own row

- GIVEN two separate confirmations of the same module
- WHEN each confirmation flow runs
- THEN two inspection rows exist (no deduplication; conservative default)

### Requirement: Inspection Persistence and v2 Migration

The system MUST persist inspections in a Room `inspections` entity with a nullable `scheduledReinspectionAt` and MUST migrate v1 databases to v2 with an explicit `Migration(1,2)` that preserves existing data; destructive fallback MUST NOT be used.

#### Scenario: Migration preserves incidences

- GIVEN a v1 database containing incidences
- WHEN the upgraded app opens the database
- THEN all incidences remain and the inspections table exists

### Requirement: Dashboard Reinspection Chip

The Dashboard MUST render the "Reinspeccion 24h" chip from persisted data: visible when the latest inspection has a scheduled reinspection pending, hidden otherwise. All remaining Dashboard mock content MUST stay unchanged in this change.

#### Scenario: Chip shows pending reinspection

- GIVEN the latest persisted inspection has a non-null scheduled reinspection
- WHEN the Dashboard loads
- THEN the chip is visible with the scheduled date/time

#### Scenario: Chip hidden when none pending

- GIVEN no inspection has a pending scheduled reinspection
- WHEN the Dashboard loads
- THEN the chip is hidden

#### Scenario: Mock content untouched

- GIVEN the Dashboard shows other mock entries
- WHEN this change is applied
- THEN those entries render exactly as before

### Requirement: State Reset at Inspection Start

The system MUST clear answers, critical stop state, and the compliance score when a new inspection starts (`initChecklist`), so a reinspection never inherits the previous inspection's state.

#### Scenario: Reset clears sticky state

- GIVEN a previous inspection left answers, a triggered critical stop, and a stored score
- WHEN `initChecklist` starts a new inspection
- THEN answers are empty, Critical Stop is not triggered, and the score is the default value