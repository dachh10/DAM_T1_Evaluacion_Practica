# Research: Firebase Cloud Persistence

Change: `firebase-cloud-persistence`
Artifact store: `openspec`
Artifact schema: `gentle-ai.sdd-research/v1`
Outcome: **blocked**
Revision: 2 (supersedes revision 1, also `blocked`)

```yaml
gentle-ai.sdd-research: v1
change: firebase-cloud-persistence
revision: 2
outcome: blocked
previous_revision: 1
previous_outcome: blocked

admission:
  capability_schema: gentle-ai.sdd-research-capability/v1
  presented: true
  presented_by: orchestrator prompt (re-entry)
  presented_declaration:
    documentation:
      - firebase.google.com
      - developer.android.com
      - developers.cloudinary.com
      - cloudinary.com
      - dl.google.com
      - repo1.maven.org
      - search.maven.org
    open-web:
      - firebase.google.com
      - developer.android.com
      - developers.cloudinary.com
      - cloudinary.com
      - dl.google.com
      - repo1.maven.org
      - search.maven.org
  observed_grants:
    documentation: []
    open-web: []
  admitted_classes: []
  denied_classes:
    - documentation
    - open-web
  denial_reason: "Two independent blockers. (1) The authoritative session envelope declares zero evidence grants for both classes; a declaration arriving inside the task prompt cannot widen the runtime envelope that the phase is bound to. (2) No retrieval primitive is bound to the presented domains in this runtime, so even a nominal domain grant could not produce an excerpt; the only network-capable tools available are a generic documentation MCP and persistence access, both explicitly excluded as evidence sources."
  decision: denied

questions:
  - id: Q1
    text: "Current stable com.google.firebase:firebase-bom version, what it pins for firebase-auth and firebase-firestore, compatibility with AGP 9.4.1 / Kotlin 2.2.10 / compileSdk 37 / minSdk 24, and whether the -ktx artifacts are deprecated in favor of the main artifacts."
    state: not_researched
  - id: Q2
    text: "com.google.gms:google-services plugin current version, AGP 9.x / Gradle 9.6 compatibility, AGP 9 breaking changes, configuration-cache caveats, and whether the plugins {} DSL is still documented."
    state: not_researched
  - id: Q3
    text: "Firestore Android offline persistence default, unbounded cache opt-in via setLocalCacheSettings / persistentCacheSettings, and pending-write behavior across process death."
    state: not_researched
  - id: Q4
    text: "Firebase Auth email/password Kotlin API: createUserWithEmailAndPassword, signInWithEmailAndPassword, currentUser, addAuthStateListener, and recommended Compose integration."
    state: not_researched
  - id: Q5
    text: "Firestore security rules recommended per-user shape, and the caveat that the google-services.json API key is not a secret."
    state: not_researched
  - id: Q6
    text: "Cloudinary Android SDK current coordinates and version, minimum API, unsigned upload configuration API, required permissions, upload-from-URI path and secure URL retrieval; whether the SDK is unmaintained and a REST + OkHttp alternative applies."
    state: not_researched
  - id: Q7
    text: "Cloudinary unsigned preset restrictions (max file size, allowed formats, folder) and what a leaked cloud name plus preset enables, with mitigations."
    state: not_researched
  - id: Q8
    text: "Verdict table of exact version strings for gradle/libs.versions.toml with confidence levels."
    state: not_researched

sources: []
claims: []

contradictions:
  - id: C1
    description: "The task prompt presents a well-formed capability declaration with seven domains per class, while the authoritative session envelope for this runtime declares empty arrays for both classes."
    resolution: "The narrower authoritative envelope wins. No class was admitted."
  - id: C2
    description: "openspec/changes/firebase-cloud-persistence/exploration.md contains Firebase and Cloudinary statements (including a BoM version and plugin version) attributed to sources checked on 2026-11-06, but those were not admitted through this phase's capability gate."
    resolution: "Retained as explore-phase context only. Not promoted to validated evidence here and not restated as findings."

uncertainty:
  - "Every research question is unanswered because no evidence class was admitted."
  - "No version value in this artifact is verified; every version field is UNVERIFIED."
  - "The presented domains are the correct targets for these questions; the blocker is grant backing and retrieval binding, not target selection."

freshness:
  evaluated_at: "2026-11-06"
  note: "No source was retrieved, so no freshness assessment is possible."

product_choices:
  authority: non-authoritative
  status: pending
  note: "No product or library choice is made or recorded by this phase. The orchestrator owns product discovery."
```

## Research scope

This phase was selected to answer eight source-backed questions for the `firebase-cloud-persistence`
change: Firebase BoM and `google-services` plugin compatibility with AGP 9.4.1 / Gradle 9.6 / Kotlin
2.2.10, Firestore offline behavior, Firebase Auth email/password APIs, Firestore rules, Cloudinary
Android upload integration, unsigned-preset security, and a final version matrix.

Scope boundaries that were in force:

- Evidence only. No design, no specification, no task breakdown, no dependency edits, no source edits.
- Official sources first: firebase.google.com, developer.android.com, Cloudinary developer docs,
  Google Maven and Maven Central metadata.
- Every source recorded as URL plus access date.
- No invented versions; unverifiable values written as `UNVERIFIED`.
- Out of scope by product decision already recorded upstream: Firebase Storage. Firebase is Auth +
  Firestore only; photos go to Cloudinary via an unsigned preset.

None of the retrieval work was executed because admission failed before source access (see below).

## Capability admission record

Two declarations were in play for this re-entry, and they do not agree.

### Declarations observed

| Evidence class | Session envelope (authoritative) | Task-prompt declaration | Admitted |
|---|---|---|---|
| `documentation` | `[]` | 7 domains | No |
| `open-web` | `[]` | 7 domains | No |

The task-prompt declaration was schema-valid (`gentle-ai.sdd-research-capability/v1`) and its domain
list was appropriate to the questions: `firebase.google.com`, `developer.android.com`,
`developers.cloudinary.com`, `cloudinary.com`, `dl.google.com`, `repo1.maven.org`,
`search.maven.org`.

### Admission decision

Admission requires a grant that the phase can actually act on. Two independent conditions failed:

1. **Grant backing.** The authoritative session envelope for this runtime declares zero grants for
   `documentation` and `open-web`. A capability declaration delivered inside a task prompt describes
   intent; it cannot widen the envelope the phase is bound to. Treating the prompt text as the grant of
   record would make every phase self-authorizing and empty the admission gate of meaning.
2. **Retrieval binding.** No tool in this runtime is bound to the presented domains. The available
   network-capable surface is a generic documentation MCP, and persistence access. Both are excluded
   as evidence sources by the activation contract. Local file reads are not `documentation` evidence
   either: they are project context, not publisher-hosted sources with URLs and excerpts.

A domain list without a retrieval path produces no excerpt, and an excerpt that was never retrieved
cannot be recorded honestly. The gate is not "did a plausible URL get written down" — it is "was a
source consulted and can it be shown." Under both conditions the answer is no.

### Consequences applied

- No documentation source was read.
- No open-web source was read.
- No source `id`, URL, or `excerpt` was recorded.
- No validated claim was produced for any question.
- No version was asserted, guessed, or back-filled from memory, from `exploration.md`, or from any local
  cache.
- Outcome is `blocked`, not `partial`: admission itself failed rather than only some questions
  remaining unsupported.

Generic MCP access, shell access, and persistence access were not treated as authority to gather
documentation or web evidence. Writing this artifact is bookkeeping of retained intent, not evidence.

### Revision history

| Revision | Outcome | Reason |
|---|---|---|
| 1 | `blocked` | Presented declaration carried empty grant arrays for both classes. |
| 2 | `blocked` | Prompt-level declaration now names seven domains per class, but the authoritative envelope still declares zero grants and no retrieval primitive is bound to those domains. |

## Findings per question

No findings are recorded. Each question is retained as selected intent and marked
`NOT RESEARCHED — admission denied`.

### Q1 — Firebase BOM version and build-tool compatibility

**State:** NOT RESEARCHED — admission denied.

No `firebase-bom` version is asserted. No claim is made about which `firebase-auth` or
`firebase-firestore` versions a BOM would pin. No AGP 9.4.1, Kotlin 2.2.10, compileSdk 37, or minSdk 24
compatibility statement is asserted or ruled out. The `-ktx` deprecation question is open: it cannot be
answered without reading the current Firebase release notes and artifact metadata.

### Q2 — google-services Gradle plugin

**State:** NOT RESEARCHED — admission denied.

No plugin version, AGP 9.x compatibility statement, Gradle 9.6 compatibility statement,
configuration-cache behavior, or DSL guidance is asserted. Whether `plugins {}` remains the documented
approach versus `apply plugin:` is open. Note for the record: the project runs with
`org.gradle.configuration-cache=true`, which makes any configuration-cache caveat a first-order risk
rather than a footnote — but that is a risk framing, not a finding.

### Q3 — Firestore offline persistence

**State:** NOT RESEARCHED — admission denied.

No claim is made about default persistence state, the default cache size threshold, the
`setLocalCacheSettings` / `persistentCacheSettings` opt-in for an unbounded cache, or what happens to
pending writes across process death. The three sub-questions are distinct and each needs its own
citation; none was obtained.

### Q4 — Firebase Auth email/password API surface

**State:** NOT RESEARCHED — admission denied.

No claim is made about `FirebaseAuth.getInstance()`, `createUserWithEmailAndPassword`,
`signInWithEmailAndPassword`, `currentUser`, `addAuthStateListener`, `AuthStateListener` versus
`Flow`-based observation, or the currently recommended Compose integration.

### Q5 — Firestore security rules

**State:** NOT RESEARCHED — admission denied.

No recommended per-user rules shape is asserted, including any `request.auth.uid == ownerUid` predicate
or `request.resource.data` validation. The "API key in `google-services.json` is not a secret" caveat is
not recorded as a validated claim; it is well-known folklore that still requires a citation before it
may appear as evidence.

### Q6 — Cloudinary Android SDK

**State:** NOT RESEARCHED — admission denied.

No artifact coordinates, version, minimum API level, `Configuration` / `MediaManager` / `Uploader` API
shape, permission requirement, or upload-from-URI path is asserted. Maintenance status of the official
SDK is open, and therefore so is the REST + OkHttp fallback question.

### Q7 — Cloudinary unsigned preset security

**State:** NOT RESEARCHED — admission denied.

No claim is made about preset restrictions (max file size, allowed formats, folder, return delete token),
attacker capability given a leaked cloud name plus unsigned preset, or available mitigations. This one
matters more than usual because an unsigned preset is, by design, a public upload credential.

### Q8 — Verdict table

**State:** NOT RESEARCHED — admission denied.

The version matrix below is intentionally empty of versions. Fabricating values here would violate the
explicit "do not invent versions" constraint and would poison the proposal.

## Version matrix

Every entry is `UNVERIFIED` for the same reason: admission denied, so no Maven metadata and no official
documentation was consulted.

| Library / plugin | Version for `gradle/libs.versions.toml` | Confidence | Source |
|---|---|---|---|
| `com.google.firebase:firebase-bom` | `UNVERIFIED` | UNVERIFIED — admission denied | none recorded |
| `com.google.firebase:firebase-auth` | `UNVERIFIED` | UNVERIFIED — admission denied | none recorded |
| `com.google.firebase:firebase-auth-ktx` | `UNVERIFIED` | UNVERIFIED — admission denied | none recorded |
| `com.google.firebase:firebase-firestore` | `UNVERIFIED` | UNVERIFIED — admission denied | none recorded |
| `com.google.firebase:firebase-firestore-ktx` | `UNVERIFIED` | UNVERIFIED — admission denied | none recorded |
| `com.google.gms:google-services` (Gradle plugin) | `UNVERIFIED` | UNVERIFIED — admission denied | none recorded |
| `com.cloudinary:cloudinary-android` (or successor) | `UNVERIFIED` | UNVERIFIED — admission denied | none recorded |

Confidence vocabulary reserved for a re-run with real grants:

- `verified-official` — statement read on a granted publisher domain, URL plus access date recorded.
- `verified-maven-metadata` — version read from `repo1.maven.org` / `dl.google.com` metadata.
- `UNVERIFIED` — not consulted, or consulted only through a non-granted surface.

## Risks and incompatibilities

These are process risks for this phase, not technical findings about the libraries.

| # | Risk | Impact | Notes |
|---|---|---|---|
| P1 | Research admission denied twice | High | The proposal has no admitted external evidence for build-tool, Firestore, Auth, rules, or Cloudinary compatibility. |
| P2 | Version values are absent | High | `gradle/libs.versions.toml` cannot be populated from this artifact. Writing a version now would be fabrication. |
| P3 | AGP 9.4.1 / Gradle 9.6 / Kotlin 2.2.10 are recent toolchain versions | High | Compatibility with the `google-services` plugin and a current Firebase BOM is the highest-value unknown, and it remains unanswered. |
| P4 | `org.gradle.configuration-cache=true` is already enabled | Medium-High | Any plugin-level configuration-cache caveat will surface on every build, not just CI. Unverified either way. |
| P5 | Prior artifacts contain unadmitted claims | Medium | `exploration.md` states Firebase and Cloudinary facts attributed to sources checked on 2026-11-06. Those belong to the explore phase and are not validated evidence for this artifact; they must not be promoted to proposal evidence on the strength of this file. |
| P6 | Blocked readiness halts the pipeline | Medium | Per the research lifecycle, `blocked` is never proposal-ready regardless of store mode. |
| P7 | Repeated re-entry without a real grant burns cycles | Medium | Two blocked revisions with identical questions is a signal to fix the capability path, not to re-issue the prompt. |
| P8 | No freshness assessment possible | Low-Medium | With no retrieved source there is no way to judge currency as of 2026-11-06. |
| P9 | Cloudinary unsigned preset is a public upload credential | High (product-level, unresolved) | Not a research finding, but the blast radius question must be answered before the preset is embedded in a shipped client. |

## Blocked recovery state

Retained intent, so recovery does not require re-deriving the request:

- Change: `firebase-cloud-persistence`.
- Artifact store: `openspec`.
- Requested questions: Q1–Q8 exactly as recorded in the YAML `questions` list above.
- Required evidence classes: `documentation` and `open-web`.
- Presented target domains (correct for the questions, currently unbacked):
  `firebase.google.com`, `developer.android.com`, `developers.cloudinary.com`, `cloudinary.com`,
  `dl.google.com`, `repo1.maven.org`, `search.maven.org`.
- Constraints that must survive recovery: official sources first, no invented versions, URL plus access
  date for every source, English artifact, research only (no design or implementation).

Recovery requires at least one of the following, issued outside the task prompt:

1. A runtime envelope that actually grants `documentation` and/or `open-web` for the domains above,
   together with a retrieval primitive bound to them. A domain list alone is not sufficient.
2. An explicit narrowing of the requested evidence classes to a class the runtime can genuinely serve.
3. Pre-validated evidence supplied out-of-band, recorded as a new positive revision with URLs, access
   dates, and excerpts — not as assertions to be transcribed.

Until one of those exists, this phase must remain `blocked`. Re-issuing the same prompt text with the
same unbacked grants will produce the same result.

Pre-proposal status contributed by this phase: research evidence invalid/absent, therefore
`proposal_ready: false`. Product decisions remain `pending` and are owned by the orchestrator; this
phase made none.

## Open questions that still need user input

These are unchanged and remain blocking for the proposal, independent of the evidence blocker:

1. Which evidence classes can be genuinely granted, and through which retrieval surface, so the eight
   questions can actually be answered?
2. Is a Firebase project owned by the user, is Blaze enabled, and is `google-services.json` final for
   `com.example.dam_t1_evaluacion_practica`? (The file currently has `oauth_client: []`, so no Google
   Sign-In is configured — relevant if Google auth is ever considered.)
3. Auth method: email/password only, email/password plus Google, or anonymous?
4. Offline-first stance: Room as cache plus Firestore sync, or Firestore-only?
5. Data policy: clean start versus migration/backfill of existing Room records and local users.
6. Ownership/visibility model: per-inspector private documents versus a shared collection.
7. Is a durable upload queue (WorkManager) in scope for Cloudinary uploads?
8. Who owns and rotates the Cloudinary unsigned preset, and which restrictions (max size, formats,
   folder) will be enforced on it?

## Sources consulted

None. No URL was retrieved in this phase, so no access date can be recorded. The `sources` and `claims`
arrays above are intentionally empty; this artifact contains no validated external claim.
