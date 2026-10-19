# M2 verification strategy

DroidMentor uses local unit tests for pure logic and simulated networking, and Android tests for Room, DataStore, connectivity and Compose integration. These checks have different purposes: compiling an instrumentation APK does not execute its tests.

## Scope and current evidence

The current code implements the five screens, manual factories, repositories and offline UI guards. On 2026-10-12, the latest local check passed with 109 debug unit tests and the same 109 release unit tests, no failures/errors/skips, and lint with 0 errors and 59 existing warnings. A later emulator verification on the same date passed 80 Android tests on Android 14 / API 34. Both app variants compiled, lint still reported 0 errors and 59 existing warnings, and no unit or device test failed or was skipped.

The 2026-10-11 run of 73 tests is historical evidence. The newer 80-test run includes six real-screen cases using production ViewModels, real isolated Room storage and Ktor MockEngine. Production history and preferences were left untouched. Real provider access remains a separate check; simulated responses do not prove BYOK access or quotas.

## Levels and coverage

| Level | Main evidence | Purpose |
|---|---|---|
| Domain unit tests | `HistoryPayloadBuilderTest` | Full history, ordering, strict roles, invalid input, failed-row retry and no truncation |
| Network unit tests | Client, DTO, mapper and remote-source test classes | Request JSON, headers, persona, statelessness, HTTP/transport failures, cancellation and log privacy using Ktor MockEngine |
| Connectivity unit tests | `DefaultNetworkTrackerTest` | Validated network state, stale callbacks and initial-snapshot races |
| Factory unit tests | `DependencyViewModelFactoryTest` | Fake dependencies, deferred construction and owner-scoped instances |
| ViewModel unit tests | `ScreenViewModelsTest` | Presence-only key state, draft retention, repeated events, Room-only content, same-row retry and offline/key guards |
| Android storage tests | Entity, DAO, database and preferences test classes | Real Room constraints, transactions, Flow observation, disk persistence and isolated Preferences DataStore |
| Android integration tests | Application, repository and connectivity tests | Manual graph, shared resources, persisted send outcomes, real emulator connectivity and callback cleanup |
| Compose navigation tests | `DroidMentorNavHostTest`, `MainActivityNavigationTest` | Five destinations, return paths, Android Back, saved state and real menu callbacks |
| Manual device checks | Checklist below | Layout, keyboard, interaction, offline reading and a real BYOK exchange |

Tests use generated fictitious credentials in memory or isolated test-only preference files. They never print key values. Device tests must clean up only their own rows/files and restore any network toggles they change.

## Reproducible commands

Use the repository's Gradle wrapper, JDK 17 and Android SDK Platform 35. Library versions come from `gradle/libs.versions.toml`.

Local verification without an emulator:

```bash
./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain --no-daemon
```

Focused domain/network checks:

```bash
./gradlew :app:testDebugUnitTest --tests 'pt.isel.pdm.droidmentor.domain.HistoryPayloadBuilderTest'
./gradlew :app:testDebugUnitTest --tests 'pt.isel.pdm.droidmentor.data.remote.*'
```

Only when a device run is requested and a device is already available:

```bash
./gradlew :app:connectedDebugAndroidTest --console=plain --no-daemon
```

Reports are written under `app/build/test-results`, `app/build/reports/lint-results-debug.html` and, after actual device execution, `app/build/outputs/androidTest-results/connected/debug`. Generated reports are not committed.

A clean build is useful in this cloud-synchronised workspace because older generated resources/caches have previously stalled incremental compilation. Clean only regenerated build files; do not delete source, Room data, preferences or the user's IDE configuration.

## Device matrix

| Device/API | Status |
|---|---|
| Emulator Android 14 / API 34, arm64 | 80-test run passed on 2026-10-12, including six new screen interaction cases |
| Android API 26, the declared minimum | Pending |
| Android API 35, the declared target | Pending |
| Physical phone | Pending |
| Small viewport, landscape and enlarged font | 320×640 viewport checked; Title tested with font scale 1.8 in portrait and landscape, with scrolling to every action |
| Light and dark themes | Title, Settings and About inspected; light/dark About and enlarged-font Title checked on 2026-10-12 |

## Manual checklist before the M2 demo

- [x] Open all three Title actions and return using the visible Back action and Android Back.
- [x] Confirm About displays the actual Gradle version, author, course and library credits.
- [x] Check Settings masking, visibility, save and clear with generated fictitious input in an isolated contract. Enter any real key only in the app for a separate live-provider check.
- [x] Create an isolated local chat from the actual history action; verify persisted-ID navigation and empty state. Production history/date layout remains available for manual use.
- [x] Complete a real Gemini exchange and a second turn: on 2026-10-14 the author confirmed two consecutive real replies in the app. This is author-reported manual evidence; the agent did not inspect that live exchange. Full-history payload and repeated persona are covered independently by automated tests.
- [x] Confirm an isolated persisted conversation remains readable after changing the injected validated status to offline; the actual banner stays visible and its draft remains editable. Physical emulator connectivity is separately covered by ConnectivityObserver tests.
- [x] Confirm Send/Retry stay disabled with missing prerequisites or an unresolved attempt; verify retry reuses the saved row.
- [x] Exercise actual input, keyboard and message widgets during emulator tests; inspect scrollable Title under enlarged font and landscape. Other APIs/physical-device layouts remain pending.
- [x] Confirm typed persisted error text is shown without raw provider response or credential content.
- [ ] Confirm screenshots and the milestone video never reveal a credential.

A real provider check is separate from MockEngine tests. The author reported a successful two-turn live check on 2026-10-14; access and quota must still be checked before recording the demo.

## Exit criteria and assignment constraints

Local exit criteria: debug/release compilation and unit tests pass, instrumentation sources compile, lint has no errors, and the patch passes `git diff --check`. Device/visual/live-provider results must be reported separately and cannot be replaced by APK compilation.

1. Manual DI through `DroidMentorApplication` and hand-written AndroidX factories; no DI framework.
2. Ktor Client networking only, with OkHttp used only as its engine.
3. kotlinx.serialization only.
4. Room is the only chat-history persistence mechanism.
5. The configured Gemini key is stored only in Preferences DataStore; never source, Room, BuildConfig or logs.
6. Stateless requests carry the full alternating user/model history starting with user.
7. Every request carries the persona in `system_instruction`, separately from user messages.
8. Room is the UI's history source of truth; connectivity is checked before every API call.
9. Network failures are typed, and structured coroutine cancellation is preserved.
10. Images are outside M2; any later support must persist only file paths/URIs, never Base64 in Room.

Implementation is complete through M2-29. Delivery work now covers M2-30 and M2-31: [the recording script](M2_VIDEO_SCRIPT.md) is ready, while the actual video, accessible link and annotated `mentor_2` tag remain pending.

Code delivery update — 2026-10-17: the author requested publication of the code and annotated `mentor_2` tag before the video, whose upload is planned for 2026-10-18. The code is integrated through a pull request into `main`; video acceptance remains pending and is not claimed by the code tag. The [CI run for `a0622fe`](https://github.com/Stocca05/PastaGpt/actions/runs/38578312143) completed successfully. This delivery changes documentation only; the previously verified application code is unchanged.

## M2-26 history coverage audit — 2026-10-12

The existing 22 domain tests cover every planned history case; no duplicate test was added. The focused Gradle command passed with `BUILD SUCCESSFUL in 27s`, 22 tests, zero failures/errors/skips.

| Required case | Existing test evidence |
|---|---|
| Empty history and first user turn | `emptyHistoryIsRejected`, `firstPendingUserProducesOneTurn` |
| All N prior exchanges plus current user | `everyCompletedExchangeIsIncluded` |
| Sequence ordering and large/gapped positions | `sequenceDeterminesTurnOrder`, `timestampsDoNotReorderTurns`, `largeAndNonContiguousSequencesAreAccepted` |
| Strict roles, final user, valid statuses | `initialModelTurnIsRejected`, `consecutiveEqualRolesAreRejected`, `completedHistoryWithoutPendingUserIsRejected`, `everyStatusCombinationIsChecked` |
| Full long history and untouched input | `longHistoryAndTextAreNotTruncated`, `inputMessagesAreNeverChanged` |
| Invalid ownership, identities, sequences, text and stale errors | Dedicated rejection cases in `HistoryPayloadBuilderTest` |
| Failed-row retry without a duplicate | `unresolvedFailureBlocksLaterTurns`, `retryUsesTheSameRowAfterItsErrorIsCleared` |
| No shared conversation session | `independentCallsDoNotShareHistory`; builder output contains only role/text turns, with request JSON separately checked by DTO/source tests |

## M2-27 network coverage audit — 2026-10-12

The existing remote tests already cover the planned Ktor MockEngine scenarios. The focused remote-package run passed with `BUILD SUCCESSFUL in 10s`: 53 tests (8 client, 12 DTO, 20 result mapper, 13 source), zero failures/errors/skips. No real network or credential was used.

| Required case | Existing evidence |
|---|---|
| 200 with usable answer and actual JSON | Mapper success cases, DTO contract tests and source request tests |
| 400, 401, 403, 429 and 5xx | `httpFailuresAreMappedBeforeReadingTheirBodies`, source failure cases |
| Retry-After numeric/date/invalid metadata | `unsignedRetryDelaysArePreserved`, `retryDatesUseTheCurrentClockAndRoundUp`, `invalidRetryMetadataIsIgnoredSafely` |
| IO and timeout, including body reads | `transportExceptionsAreTypedWithoutTheirDetails`, `ktorRequestTimerProducesTypedTimeout`, `bodyReadFailureIsANetworkFailure` |
| Malformed, empty and blocked replies | Mapper response rejection cases and source response tests |
| Stateless complete history and repeated persona | DTO and source tests inspect actual request JSON, changed keys and long retained history |
| No raw failure or credential exposure | Safe diagnostic/logging tests and typed error-category mapping |
| Cancellation remains structured | Mapper and source cancellation cases; cancellation is not converted into a false network result |

ViewModels receive local acknowledgements, and saved answer/error content is observed through Room; this boundary is separately checked by repository and ViewModel tests.

## M2-28 Room coverage audit — 2026-10-12

The existing device suites cover the planned DAO requirements. Room implementation, storage tests and exported v1 schema are unchanged from the verified foundation commit `15b270b`. The last actual device run was the 73-test successful run on 2026-10-11, including 14 DAO, 7 entity and 5 persistent-database tests. This session rechecked coverage and compiled the instrumentation APK (`BUILD SUCCESSFUL in 9s`); it did **not** rerun any device test.

| Required case | Existing evidence |
|---|---|
| Insert and observe Flow | `messageFlowEmitsInsertedRows`, `chatFlowEmitsTitleChanges`, preview Flow tests |
| Cascade delete with observation | `deletingChatClearsItsObservedHistory`, entity cascade case |
| Unique, monotonic per-chat sequence under concurrency | `concurrentInsertsAllocateConsecutiveSequences`, `sequenceAllocationIsSeparateForEachChat`, entity uniqueness cases |
| Atomic user/model completion and rollback | `completedExchangeSavesBothRowsOnlyOnce`, `failedReplyInsertRollsBackExchangeCompletion` |
| Persisted error category and same-row retry | `retryKeepsTheSameMessageAndClearsItsError`, entity category cases, disk reopen/retry case |
| No duplicate retry or reply | Completion duplicate guards and repository retry tests |
| Exported schema and real persistence | `exportedSchemaMatchesTheInitialDatabaseContract`, disk close/reopen cases |

Fresh device regression for the new screens remains pending in the manual checklist; unchanged DAO coverage is not evidence of tested new UI interactions.

## Selected-chat ownership check — 2026-10-12

Active Chat now supplies its local ID as the Compose ViewModel key. A new factory test checks that two selections under the same owner preserve different captured IDs, while revisiting an existing key reuses its original instance. Final local debug/release builds, both unit suites, lint and instrumentation APK compilation passed with `BUILD SUCCESSFUL in 43s`: 109 tests per variant, no failures/errors/skips. No device test was run.

## M2-29 CI configuration — 2026-10-12

The Android CI workflow is configured for push, pull request and manual dispatch. YAML parsing, required triggers, read-only permissions, four 40-character release commit references, executable wrapper mode and emulator-free task selection were checked locally. The action releases were verified against their official GitHub repositories; Java/Gradle workflow structure follows [GitHub's Gradle guidance](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle).

The final local command passed in 43 seconds after the selected-ID fix: 109 debug unit tests and the same 109 release unit tests, no failures/errors/skips, lint 0 errors and 59 unchanged warnings, both app variants and instrumentation APK compilation successful. A preceding clean full run passed in 1m 11s. Device tests were not executed in that earlier check; the later emulator run is recorded below.

On 2026-10-14, the status and logs of [GitHub Actions run 38383593411](https://github.com/Stocca05/PastaGpt/actions/runs/38383593411) were checked. The run completed successfully on 2026-10-13 for `8ad0d772efa035f35d9aaa196eef2b6456295b4f` on `codex/m2-dependencies`. The exact hosted command was:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain --no-daemon
```

Real log output: `BUILD SUCCESSFUL in 4m 2s`, `83 actionable tasks: 83 executed`. Every workflow step succeeded. This proves the hosted build/test/lint check; it does not represent a device run or live Gemini exchange.

## Emulator verification — 2026-10-12

The existing full suite first passed with 74 device tests and both 109-test unit variants (`BUILD SUCCESSFUL in 2m 5s`). Six new `ScreenDeviceTest` cases now cover actual Settings password/save/clear controls, offline local creation from history, a pending send followed by the Room-persisted model row, a saved provider error followed by same-row retry, readable offline history with editable input, and missing-key UI gating. They use only generated fictitious input, in-memory test preferences, isolated production-schema Room and the real Ktor/source/result-mapper stack around MockEngine. ViewModel cancellation is awaited before test storage closes.

ActiveChatScreen accepts an explicit model for isolated screen tests; its default still uses the ID-keyed manual factory and the destination owner. No business operation or production credential was moved into a Composable.

Cloud synchronisation caused source/resource IO timeouts, temporarily missing tracked files and identical numbered copies of source/resources/generated dex. Missing tracked files were restored only from the verified commit; identical conflict copies were preserved outside source. Tests were then run from a managed checkout outside the synchronised Documents directory, based on the same commit plus the verification changes. No dependency or app version was changed to bypass these failures.

Final command in that checkout:

```bash
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon
```

Result: `BUILD SUCCESSFUL in 1m 13s`, 109 debug unit tests, the same 109 release unit tests, 80 Android tests, zero failures/errors/skips, lint 0 errors and 59 unchanged warnings. `installDebug` succeeded in 6s; a final cold launch returned `Status: ok`, `LaunchState: COLD`, 1467 ms. Theme, font, rotation and network settings were restored to their original values. The emulator remains available for the separate live-key check.

## Saved API key screen regression — 2026-10-19

An existing configured key previously appeared beside an always-empty setup form. Initial loading and storage errors could also display the unconfigured label. The screen now distinguishes these states and opens a replacement draft only after Change key. Cancel does not save or delete anything. A successful replacement closes the form immediately; a failed replacement preserves the original key and the new draft. DataStore storage, the preference name and file, and provider networking are unchanged.

Six new unit cases cover no re-entry for an existing key, cancellation without writes, saved replacement recognised by a new screen owner, failed replacement retaining the original, loading/read-error guards, and acknowledgement before a delayed observer emission. Both unit variants passed 115 tests with zero failures/errors/skips. The existing privacy test now opens replacement mode explicitly. Two new Android screen cases cover configured/change/cancel/replacement controls and safe read-error presentation; the previous save/clear case also checks that a just-saved key does not leave a setup field on screen.

Verification command, using JDK 17 in the isolated managed checkout outside the cloud-synchronised Documents directory:

```bash
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain --no-daemon
```

Real output: `BUILD SUCCESSFUL in 3m 58s`, `139 actionable tasks: 139 executed`. Both app variants and the Android test APK compiled. Unit XML reports confirm 115 tests per variant with zero failures/errors/skips; lint XML has zero errors and 59 existing warnings. The five changed source/test files in the root workspace were compared byte-for-byte with the tested checkout. Reports were preserved outside the temporary checkout before cleanup.

The attempted emulator start used separate temporary data and cache paths. It failed before boot with `Your device does not have enough disk space to run avd`. Less than 500 MB was available on the Mac. No new device-test execution or visual inspection is claimed for this change, and no production key or conversation was read, overwritten or deleted for the attempt. The historical 80-device-test result above applies to the earlier code.
