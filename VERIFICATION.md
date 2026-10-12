# M2 verification strategy

DroidMentor uses local unit tests for pure logic and simulated networking, and Android tests for Room, DataStore, connectivity and Compose integration. These checks have different purposes: compiling an instrumentation APK does not execute its tests.

## Scope and current evidence

The current code implements the five screens, manual factories, repositories and offline UI guards. On 2026-10-12, the latest local check passed with 108 debug unit tests, no failures/errors/skips, and lint with 0 errors and 59 existing warnings. The instrumentation APK compiled successfully. No emulator or device was opened or used in this session, as requested.

The previous device run on 2026-10-11 passed 73 tests before the real M2 screens were added. It remains evidence for unchanged Room, DataStore, connectivity and repository implementations. It does **not** establish that the new screens have passed device or visual testing. No real Gemini request or real credential has been used in these checks.

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
| Emulator Android 14 / API 34, arm64 | Previous 73-test run passed on 2026-10-11; new screen tests not executed |
| Android API 26, the declared minimum | Pending |
| Android API 35, the declared target | Pending |
| Physical phone | Pending |
| Small viewport, landscape and enlarged font | Pending visual and interaction checks |
| Light and dark themes | Existing theme checked previously; new screens still need visual checks |

## Manual checklist before the M2 demo

- [ ] Open all three Title actions and return using the visible Back action and Android Back.
- [ ] Confirm About displays the actual Gradle version, author, course and library credits.
- [ ] Enter the user's key directly in Settings; test masking, visibility, save and clear without printing or recording its value.
- [ ] Create and reopen a local chat; verify empty state, previews and dates.
- [ ] Complete a real Gemini exchange and a second turn using the full saved history and repeated persona.
- [ ] Reopen the conversation offline; confirm saved messages remain readable, the banner stays visible and the draft remains editable.
- [ ] Confirm Send/Retry stay disabled with missing prerequisites or an unresolved attempt; verify retry reuses the saved row.
- [ ] Confirm keyboard handling, multiline input, scrolling and readable role bubbles on a device.
- [ ] Confirm typed persisted error text is shown without raw provider response or credential content.
- [ ] Confirm screenshots and the milestone video never reveal a credential.

A real provider check is separate from MockEngine tests. The BYOK project's model access and quotas still require live verification; do not assume simulation proves access.

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

The requested code work stops at M2-29. Video, link publication and the `mentor_2` delivery tag remain M2-30 and M2-31.
