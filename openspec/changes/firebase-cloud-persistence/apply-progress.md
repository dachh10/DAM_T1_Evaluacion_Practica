# Apply Progress: Firebase Cloud Persistence

Change: `firebase-cloud-persistence` · Store: `openspec` · Slice 1 of 12 (Phase S0) · Mode: Standard
Status: **COMPLETE** — S0-1..7 done. Firebase BoM pinned down to `34.12.0`; `assembleDebug` green twice and the 42-test gate passes.

## Slice boundary

Start: `77fdcbe` (`feat: implement local persistence with room and user authentication`), branch
`feature/firebase-cloud-persistence`. End: intended `:app:assembleDebug` ×2 + `test` green. The slice
did not reach its end state.

## What changed (working tree, uncommitted)

| File | Action | What was done |
|---|---|---|
| `gradle/libs.versions.toml` | Modified | Added `firebaseBom = "35.1.0"`, `googleServices = "4.5.0"`, `cloudinary = "3.1.3"`, `workManager = "2.12.0"`; libraries `firebase-bom`, `firebase-auth`, `firebase-firestore`, `cloudinary-android`, `androidx-work-runtime`, `androidx-work-testing`; plugin `google-services`. No `-ktx` artifacts. |
| `build.gradle.kts` | Modified | `alias(libs.plugins.google.services) apply false`. |
| `app/build.gradle.kts` | Modified | `google.services` applied last; `buildConfig = true`; `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_UPLOAD_PRESET` `buildConfigField`s read from `local.properties` through `providers.fileContents(...).asText` with an empty-string fallback; BoM + auth + firestore + work runtime + work testing + cloudinary dependency. |
| `app/src/main/AndroidManifest.xml` | Modified | Explicit `INTERNET`; `android:name=".SecurityInspectionsApp"`; non-exported `FileProvider` with `${applicationId}.fileprovider`. |
| `app/src/main/res/xml/file_paths.xml` | Created | `<files-path name="evidence" path="evidence/" />`. |
| `app/src/main/java/.../SecurityInspectionsApp.kt` | Created | `Application` owning `container`. |
| `app/src/main/java/.../AppContainer.kt` | Created | Skeleton exposing only `db`. |
| `local.properties` (untracked) | Modified | `CLOUDINARY_CLOUD_NAME=f2ge9jhj`, `CLOUDINARY_UPLOAD_PRESET=ml_default`. No API key or secret. |

Pre-existing uncommitted edits in `ui/screens/ChecklistModuleScreen.kt` and
`ui/viewmodel/InspectionViewModel.kt`, and the untracked `Diagrama Clases Arquitectura.md`, were left
untouched. `app/google-services.json` and `local.properties` were not staged.

## S0-1 version resolution (verified)

Source: `https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/maven-metadata.xml`
(`<release>2.12.0</release>`, no alpha/beta/rc newer).

| Artifact | Resolved | Proof |
|---|---|---|
| `androidx.work:work-runtime` | **2.12.0** (pinned) | `:app:dependencies --configuration debugRuntimeClasspath` → `+--- androidx.work:work-runtime:2.12.0`; zero `FAILED` entries |
| `com.google.firebase:firebase-bom` | 35.1.0 | `:app:dependencies --configuration releaseRuntimeClasspath` |
| `com.google.firebase:firebase-auth` | 25.0.0 (BoM-managed) | same report |
| `com.google.firebase:firebase-firestore` | 27.1.0 (BoM-managed) | same report |
| `com.cloudinary:cloudinary-android` | 3.1.3 | same report |
| `com.google.gms:google-services` | 4.5.0 | plugin applied and `:app:processDebugGoogleServices` executed successfully |

## Blocker (exact text)

```
> Task :app:compileDebugKotlin FAILED
e: file:///C:/Users/diego/.gradle/caches/9.6.0/transforms/628630723691a2a5ff90a59bdb1812ab/transformed/firebase-auth-25.0.0-api.jar!/META-INF/java.com.google.android.gmscore.integ.client.firebase-auth-api_firebase-auth-api.kotlin_module Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.4.0, expected version is 2.2.0.

FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':app:compileDebugKotlin' (registered by plugin 'com.android.internal.application').
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details
BUILD FAILED in 4m 50s
```

Diagnosis: BoM 35.1.0 resolves `firebase-auth:25.0.0`, whose public API jar is compiled with Kotlin 2.4.0
metadata. The project's Kotlin Gradle plugin is `2.2.10` (`libs.versions.toml:9`), so the compiler refuses
the module. `kotlin-gradle-plugin` 2.4.x is available on Maven Central, but raising Kotlin also forces
`ksp` (`2.2.10-2.0.2`, compiler-coupled) and the Compose compiler plugin to move. Not decided here.

## Work Unit Evidence

| Evidence | Value |
|---|---|
| Focused command and exact result | `:app:dependencies --configuration debugRuntimeClasspath` → BUILD SUCCESSFUL, `work-runtime:2.12.0`, 0 `FAILED`. `:app:dependencies --configuration releaseRuntimeClasspath` → BUILD SUCCESSFUL, BoM 35.1.0 / auth 25.0.0 / firestore 27.1.0 / cloudinary 3.1.3. |
| Runtime harness | `:app:assembleDebug` → **FAILED** at `:app:compileDebugKotlin` (text above). APK at `app/build/outputs/apk/debug/app-debug.apk` is stale (20/09/2026) and is **not** evidence for this change. |
| Rollback boundary | Revert the eight files above; `local.properties` is untracked and outside version control. Nothing else in the repo depends on them yet. |

## Resolution (2026-11-06)

Root cause narrowed to a single artifact. Kotlin metadata was read directly out of each published AAR
(`classes.jar` entries carrying `Lkotlin/Metadata;`, then `javap -v`):

| Artifact | Metadata | Readable by Kotlin 2.2.10 |
|---|---|---|
| `firebase-auth` 25.0.0 (BoM 35.1.0) | 2.4.0 | no |
| `firebase-auth` 24.2.0 / 24.1.0 (BoM 34.13.0-34.19.0) | 2.3.0 | no |
| `firebase-auth` 24.0.1 (BoM <= 34.12.0) | 2.1.0 | yes |
| `firebase-firestore` 27.1.0 | 2.1.0 | yes |
| `firebase-common` 23.0.0 | 2.1.0 | yes |

Raising Kotlin was rejected on evidence, not preference: KSP moved to independent versioning at `2.3.0`
and its latest release is `2.3.13`, so no KSP exists for Kotlin `2.4.x`; a Kotlin bump would break the
Room/KSP step. BoM `34.12.0` is the highest release still mapping `firebase-auth` 24.0.1 together with
`firebase-firestore` 26.2.0 and `firebase-common` 22.0.1.

| Evidence | Value |
|---|---|
| Focused command and exact result | `:app:dependencies --configuration debugRuntimeClasspath` -> `firebase-auth -> 24.0.1`, `firebase-firestore -> 26.2.0`, `firebase-common 22.0.1`, `work-runtime 2.12.0`, `cloudinary-android 3.1.3`; no `FAILED`. |
| Runtime harness | `:app:assembleDebug` -> **BUILD SUCCESSFUL in 2m 21s**; second run -> **BUILD SUCCESSFUL in 4s**, logged `Reusing configuration cache` and `Configuration cache entry reused.` |
| Test gate | `:app:testDebugUnitTest` -> BUILD SUCCESSFUL; `ExampleUnitTest` 1/1 and `InspectionViewModelTest` 41/41; total **42 tests, 0 failures, 0 errors, 0 skipped**. |
| APK | `app/build/outputs/apk/debug/app-debug.apk`, 34,515,245 bytes, 2026-11-06 19:03 - a fresh artifact, replacing the stale 20/09/2026 one. |

Nothing remains unproven for S0.
