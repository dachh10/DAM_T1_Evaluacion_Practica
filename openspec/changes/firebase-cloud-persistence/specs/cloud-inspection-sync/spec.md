# cloud-inspection-sync Specification

## Purpose

Local-first persistence with Firestore as the per-inspector replica.

## Requirements

### Requirement: Local-First Save

Saving MUST persist to Room and enqueue a cloud sync regardless of network state. The UI MUST read saved records from Room in every network state.

#### Scenario: Save offline then sync

- GIVEN no connectivity
- WHEN a dictamen is saved
- THEN it is readable as PENDING
- AND becomes SYNCED after the cloud write is acknowledged

### Requirement: Stable Cloud Identity

The document id MUST be a UUID generated at record creation and reused by every retry. The `folio` MUST remain display data only.

#### Scenario: Duplicate folios

- GIVEN two offline devices generate folio AUD-2026-0001
- WHEN both sync
- THEN two distinct owner-scoped documents exist

#### Scenario: Retry identity

- GIVEN a queued sync for UUID X
- WHEN the retry runs
- THEN it updates X and creates no second document

### Requirement: Bounded Retry and Idempotency

Sync MUST retry transient failures with increasing delay, stop after a bounded attempt count, and never duplicate documents. Failed records MUST stay local and readable.

#### Scenario: Transient failure recovers

- GIVEN a transient failure
- WHEN a later retry succeeds
- THEN one document exists with state SYNCED

#### Scenario: Terminal failure

- GIVEN every attempt fails
- WHEN the budget is exhausted
- THEN state is FAILED and the local record is intact

### Requirement: Incidence Binding

Each incidence MUST belong to exactly one inspection, stored under that inspection's document. No incidence MAY reach the cloud without an existing parent.

#### Scenario: Evidence before the dictamen

- GIVEN evidence attached before the inspection exists
- WHEN the inspection syncs
- THEN the incidence is written under it

#### Scenario: Unknown parent

- GIVEN an inspection id that was never created
- WHEN an incidence write targets it
- THEN the write is rejected and the record is FAILED locally

### Requirement: Per-Inspector Visibility

Every document MUST record the authenticated uid as owner; rules MUST reject any other uid. Local reads MUST NOT expose another inspector's records.

#### Scenario: Cross-user read

- GIVEN uid B is authenticated or no session exists
- WHEN A's document is requested
- THEN Firestore denies the read

#### Scenario: Local scoping after switch

- GIVEN user A saved records
- WHEN user B signs in on the same device
- THEN B sees none of them

### Requirement: Conflict Resolution

A clean record MUST accept a newer server `updatedAt`. A record with unsynced edits MUST NOT be silently overwritten: remote snapshots are ignored until its push is acknowledged.

#### Scenario: Remote wins when clean

- GIVEN a clean synced record on device A
- WHEN device B syncs a newer version
- THEN A displays B's version

#### Scenario: Local edits survive

- GIVEN A holds an unsynced status change
- WHEN B's newer snapshot arrives
- THEN A keeps its value, pushes it as newest, and loses no edit

### Requirement: Sync Status Exposure

Each inspection MUST expose PENDING, SYNCED or FAILED, and the UI MUST display it.

#### Scenario: Status is visible

- GIVEN a queued record or one that exhausted retries
- WHEN the inspector opens it
- THEN it shows PENDING or FAILED respectively
- AND FAILED offers retry

### Requirement: Update and Delete Propagation

Updates and deletes of a synced inspection MUST propagate. Offline, they MUST apply locally at once and replay on reconnect.

#### Scenario: Offline mutation

- GIVEN a synced inspection and no connectivity
- WHEN it is updated or deleted
- THEN the change is local and the remote change follows on reconnect

### Requirement: Explicit Network Permission

The app module MUST declare INTERNET explicitly, not rely on a transitive dependency.

#### Scenario: Manifest declaration

- GIVEN the merged manifest
- WHEN permissions are inspected
- THEN the app module declares INTERNET
