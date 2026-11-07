# cloud-evidence-upload Specification

## Purpose

Evidence capture, compression, unsigned Cloudinary upload, and pending or failed evidence in UI and PDF.

## Requirements

### Requirement: Full-Resolution Capture

Capture MUST produce a full-resolution camera image; a preview bitmap MUST NOT be the evidence source.

#### Scenario: Capture resolution

- GIVEN the inspector captures evidence
- WHEN the image is inspected
- THEN its resolution matches the camera still output, not a preview thumbnail

### Requirement: Compression Ceiling

The uploaded copy MUST be a JPEG, longest edge at most 2048 px, quality at least 70, and at most 2 MiB after further downscaling. The full-resolution capture MAY be kept locally.

#### Scenario: Compression bounds

- GIVEN a capture above the ceiling
- WHEN it is prepared for upload
- THEN the payload is at most 2 MiB and 2048 px on its longest edge
- AND a smaller capture is not upscaled

### Requirement: Unsigned Cloudinary Upload

Uploads MUST use the Cloudinary unsigned preset. The API secret MUST NOT exist in the repository, APK or bundled configuration; only cloud name and preset MAY be present.

#### Scenario: Unsigned upload

- GIVEN the built APK, the repository and a compressed image
- WHEN the APK and repository are scanned and the image is uploaded
- THEN no secret exists and Cloudinary accepts the unsigned upload

### Requirement: Durable Upload Queue

Every upload MUST be enqueued durably before transmission and MUST survive process death and reboot. Pending uploads MUST resume automatically when connectivity returns.

#### Scenario: Process death and reconnect

- GIVEN a queued item captured offline
- WHEN the process is killed, the device reboots and connectivity returns
- THEN the upload completes without inspector action

### Requirement: Bounded Retry with Terminal Failure

Retries MUST use increasing backoff and stop after a bounded attempt count. Exhausting the budget MUST produce a terminal FAILED state exposed to the inspector, with manual retry available.

#### Scenario: Exit terminal state

- GIVEN a non-retryable rejection
- WHEN the retry budget is exhausted
- THEN the item is FAILED with no further automatic attempts
- AND manual retry is attempted again if connectivity exists

### Requirement: Cloud Reference Persistence

After a successful upload the persisted evidence reference MUST be the Cloudinary `secure_url` and MUST be the value replicated in the inspection document. A device-local `content://` URI MUST NOT become the persisted final reference.

#### Scenario: Reference after upload

- GIVEN a successful upload
- WHEN the inspection syncs
- THEN its evidence field holds the https `secure_url` and no `content://` URI

### Requirement: PDF Evidence Resolution

PDF generation MUST resolve remote https evidence URLs and local file references. Evidence it cannot resolve MUST appear as a visible unavailable marker and MUST NOT be silently omitted.

#### Scenario: Resolve every evidence item

- GIVEN one reachable and one unreachable evidence reference
- WHEN the PDF is generated
- THEN the reachable photo is embedded
- AND the unreachable one shows a visible unavailable marker

### Requirement: Pending Evidence UX

An inspection MUST be savable while evidence is PENDING, UPLOADING or FAILED. The UI MUST show each item's state and MUST NOT block the dictamen.

#### Scenario: Pending and failed evidence

- GIVEN one item uploading and one item FAILED
- WHEN the inspector saves the dictamen
- THEN the save succeeds and the uploading item shows PENDING from its local reference
- AND the failed item shows FAILED with retry

### Requirement: Public ID Convention

Each upload MUST use the public id `inspections/{inspectionId}/{evidenceId}`. Retries MUST reuse it so a repeated upload replaces the same asset instead of duplicating it.

#### Scenario: Traceable, non-duplicating id

- GIVEN a timed-out upload whose asset was created
- WHEN the retry runs and the public id is read
- THEN the id contains the inspection and evidence ids
- AND the same asset is replaced, leaving only one
