# firebase-email-auth Specification

## Purpose

Firebase Auth Email/Password authentication: sessions, typed errors, credential hygiene, profiles and validation.

## Requirements

### Requirement: Email/Password Authentication

Registration, sign-in and sign-out MUST use Firebase Auth Email/Password. Credentials MUST NOT be verified against local rows. Sign-in MUST establish an authenticated uid that scopes all data access.

#### Scenario: Register

- GIVEN a new email and valid password
- WHEN the inspector registers
- THEN an account exists and the session is authenticated with a uid

#### Scenario: Sign in and sign out

- GIVEN an existing account
- WHEN the inspector signs in and later signs out
- THEN the session is established and then terminated

### Requirement: Session Persistence

An authenticated session MUST survive app restart without re-entering credentials, and restore MUST NOT require network. After explicit sign-out, a restart MUST NOT restore a session.

#### Scenario: Restart keeps session

- GIVEN a previous sign-in
- WHEN the process is killed and the app relaunches offline
- THEN the app starts authenticated

#### Scenario: Sign-out is durable

- GIVEN the inspector signed out
- WHEN the app restarts
- THEN the login screen is shown

### Requirement: Domain Error Mapping

Failures MUST surface as domain errors: INVALID_CREDENTIALS, EMAIL_ALREADY_IN_USE, WEAK_PASSWORD, NETWORK_UNAVAILABLE or UNKNOWN. Raw Firebase exception types or SDK codes MUST NOT reach the UI.

#### Scenario: Wrong password

- GIVEN a registered email
- WHEN sign-in uses a wrong password
- THEN the UI receives INVALID_CREDENTIALS

#### Scenario: Duplicate registration

- GIVEN an email already registered
- WHEN registration is attempted
- THEN the UI receives EMAIL_ALREADY_IN_USE

#### Scenario: Unmapped failure

- GIVEN an SDK failure with no mapping
- WHEN it occurs
- THEN the UI receives UNKNOWN and no SDK code

### Requirement: No Local Password Storage

Passwords MUST NOT be persisted locally in plaintext, hashed or otherwise, and the local credential column MUST be removed. Firebase Auth is the only credential store.

#### Scenario: No stored credentials

- GIVEN a successful registration or sign-in
- WHEN local persistence is inspected
- THEN no password value exists

#### Scenario: Clean start

- GIVEN a device holding schema version 2 with local users
- WHEN the app upgrades
- THEN local users are discarded and no plaintext credential remains

### Requirement: Domain-Layer Validation

Email format and password policy MUST be enforced by domain logic independent of the Composable. Invalid input MUST NOT trigger an authentication request. Passwords MUST be at least 6 characters.

#### Scenario: Invalid email

- GIVEN the input "correo"
- WHEN registration is submitted
- THEN a field error appears and no request is made

#### Scenario: Weak password

- GIVEN a 4-character password
- WHEN registration is submitted
- THEN a field error appears and no request is made

### Requirement: Inspector Profile

A profile MAY be stored at `users/{uid}`; when present it MUST contain displayName and cedula and no credential. Only the owner MUST read or write it. Registration MUST create it, and write failure MUST NOT lose the account.

#### Scenario: Owner-only access

- GIVEN the profile of uid A
- WHEN uid B reads or writes it
- THEN Firestore denies the request

#### Scenario: Profile at registration

- GIVEN a successful registration with name and cedula
- WHEN the session is authenticated
- THEN `users/{uid}` contains both fields and no password

### Requirement: Offline Sign-In Failure

When Firebase is unreachable during sign-in, the attempt MUST fail with NETWORK_UNAVAILABLE, MUST NOT create a partial session, and MUST NOT authenticate against cached local data.

#### Scenario: Cold offline sign-in

- GIVEN no connectivity and no persisted session
- WHEN valid credentials are submitted
- THEN the UI receives NETWORK_UNAVAILABLE and the app stays unauthenticated

#### Scenario: Persisted session unaffected

- GIVEN a persisted session and no connectivity
- WHEN the app launches
- THEN the session is restored without a sign-in attempt
