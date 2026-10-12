# DroidMentor (PastaGPT)

A BYOK (Bring Your Own Key) Android LLM client built for the **Mobile Devices Programming (PDM)** course at **Instituto Superior de Engenharia de Lisboa (ISEL)**, Winter Semester 2026/2027. 
It uses Google Gemini's REST API in stateless mode and acts as a senior Android engineering mentor.

## Author

| Name | Student number | Email | GitHub |
| :--- | :--- | :--- | :--- |
| Luca Raona | 55603 | 55603@alunos.isel.pt | [@Stocca05](https://github.com/Stocca05) |

## Deliveries

| Milestone | Tag | Due date | Video |
| :--- | :--- | :--- | :--- |
| 1 — Project plan | `mentor_1` | 2026-09-21 | — (see [PROJECT_PLAN.md](PROJECT_PLAN.md)) |
| 2 — Core functionality | `mentor_2` | 2026-10-12 | _pending_ |
| 3 — Advanced features | `mentor_3` | 2026-11-16 | _pending_ |
| Final — Delivery | `mentor_f` | 2026-12-12 | _pending_ |

## Documentation

* 🇮🇹 [PROJECT_PLAN.md](PROJECT_PLAN.md) (Piano di Progetto)
* 🇬🇧 [PROJECT_PLAN.en.md](PROJECT_PLAN.en.md) (Project Plan)
* [VERIFICATION.md](VERIFICATION.md) (to be added)
* [CONTRIBUTING.md](CONTRIBUTING.md) (to be added)

## Tech stack

* Kotlin & Coroutines/Flow
* Jetpack Compose + Material 3
* Ktor Client (OkHttp engine)
* kotlinx.serialization
* Room
* DataStore (Preferences)
* **Manual dependency injection** via the Application class (no Hilt/Dagger)

Dependency and plugin versions are declared in `gradle/libs.versions.toml` and referenced through catalog aliases in the build files. M2 test dependencies include JUnit, coroutine test utilities, Ktor MockEngine, Room testing and Compose UI testing.

## UI foundation (M2-03)

The mandatory package layout is documented in [the project plan](PROJECT_PLAN.en.md#912-mandatory-package-layout). Packages awaiting implementation are preserved in Git with `.gitkeep` files.

`ui/theme` provides `DroidMentorTheme`, complete light/dark Material 3 colour palettes, sans-serif typography and shared spacing values in `DroidMentorSpacing`. The theme follows the device's light/dark setting. `MainActivity` includes previews for both palettes; the navigation host’s minimal destination content applies safe drawing insets.

The native launch theme uses no ActionBar; Compose owns the app content. The application label and placeholder share the `app_name` string resource.

## Domain models (M2-04)

The `domain` package contains six simple Kotlin models: `Chat`, `Message`, `Role`, `MessageStatus`, `MessageErrorCategory` and `ConversationTurn`. Fields are read-only. IDs are local strings; timestamps are milliseconds since the Unix epoch; message order is represented by a `Long` sequence.

`ConversationTurn` holds only role and text. `Message` also holds its local IDs, state and optional error category. The models contain no Android imports, Room annotations, network DTOs or credentials. `HistoryPayloadBuilder` checks complete conversation rules; `RoomChatRepository` coordinates the persisted state changes described below.

App Kotlin code uses short English comments to explain each statement and field, with KDoc on public declarations. This teaching style is also recorded in `AGENTS.md` for later work.

## Room entities (M2-11)

`data/local/entity` defines `ChatEntity` and `MessageEntity`. Messages reference their parent chat with `CASCADE` deletion, have an index on `chatId`, and use a unique `(chatId, seq)` index. IDs are local text values; sequence numbers and timestamps use `Long`. There are no API-key or image-data columns.

`data/local/converter/MessageConverters.kt` stores role, status and error enums by name and preserves null error categories. Those stored names must remain compatible when the app schema evolves.

Seven instrumented tests check the real SQLite behaviour using a separate in-memory database under `androidTest`: completed exchanges, Long sequences and null errors, every error category, cascade deletion, duplicate positions, positions shared by different chats and rejection of messages without a parent. The entity test database and its small DAO are test helpers; production DAO tests now use the real app database schema.

With an emulator or device connected, verify M2-11 with:
```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
```

## Room DAOs (M2-12)

`ChatDao` observes chats with their last-message preview, reads one chat, inserts chats, updates titles and deletes rows. `ChatWithLastMessage` uses an SQL-selected last-message ID and `@Relation`, so the list reads only the highest-sequence message for each chat. `@Transaction` keeps the chat and its preview in the same read snapshot.

`MessageDao` observes or reads full histories ordered by `seq`. Its insert method allocates the next per-chat position and saves the row in one transaction, replacing the caller's provisional sequence. Chat recency is updated in the same transaction and never moves backwards.

Status updates save a typed failure or reset the same row for retry. Only `completeExchange` marks an existing user row `SENT`: it saves the model reply in the same transaction, rejects repeated or out-of-order completions and rolls back the user change if reply insertion fails. `RoomChatRepository` enforces the one-request-per-chat rule through a per-chat operation gate and transactional preparation checks.

Fourteen additional instrumented tests cover the production DAOs, including observable updates, message-error changes in chat previews, concurrent sequence allocation, retry without duplicates, atomic success and rollback. They run against an in-memory instance of `DroidMentorDatabase`. Run the verification command above with an emulator connected; add `clean` if stale generated build copies cause duplicate-class errors.

## App database (M2-13)

`data/local/DroidMentorDatabase.kt` registers the two history entities and exposes `ChatDao` and `MessageDao`. Its `create` method uses application storage with the stable file name `droidmentor.db`; `DroidMentorApplication.database` now holds the shared app instance. Room's main-thread query protection stays enabled, and no destructive-migration fallback is configured.

Schema version 1 is exported by KSP to [app/schemas/pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase/1.json](app/schemas/pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase/1.json). Keep this generated JSON in Git and retain each released schema version for later migration tests. The schemas folder is also packaged into instrumentation-test assets.

Five more device tests use the production database to verify history and error persistence after closing and reopening a file, continued sequence allocation, rejection of main-thread queries, preservation of data when a migration is missing, and the exported schema contract. Each disk test uses a unique test-only file name and deletes only its own file. Together, the entity, DAO and database tests account for 26 device cases.

## Key preferences (M2-14)

`ApiKeyDataStore` provides one Preferences DataStore instance for the dedicated `droidmentor_credentials` file. `DataStoreSettingsRepository` implements the manually injectable `SettingsRepository` interface with `apiKey: Flow<String?>`, `saveApiKey` and `clearApiKey`. It trims surrounding whitespace, rejects blank input and removes only the key preference when clearing.

`readResult` distinguishes an unconfigured key from unreadable storage. Write operations return success, empty-input or storage-error outcomes. Storage failures expose no exception details, coroutine cancellation remains cancellation, and successful read objects hide the credential in their text representation. No repository code logs key values or stores them in Room or source constants.

Six unit tests cover failure handling, cancellation and safe diagnostic text. Six device tests use runtime-generated fictitious values and isolated Preferences DataStore files to check persistence, replacement, clear, Flow updates, corrupt-file preservation and the single-instance provider. No real Gemini credentials are used or printed.

## HTTP client configuration (M2-05)

`data/remote/HttpClientFactory.kt` creates the Ktor client with the OkHttp engine. `ContentNegotiation` uses kotlinx.serialization with unknown-field tolerance and null-field omission. `HttpTimeout` sets a 10-second connection limit and a 60-second socket inactivity limit. `DefaultRequest` supplies the Gemini base URL and JSON content type. HTTP failure statuses remain available for the later typed result mapper; redirects are not followed automatically.

The generated `BuildConfig.DEBUG` flag controls installation of Ktor's `Logging` plugin. In debug, the logger emits only a fixed activity message: it discards Ktor's original diagnostic text, including URLs and exception details, and does not collect headers or bodies. Release clients have no logging plugin. The factory accepts no API key, and the client owns its engine so closing the client also releases the engine. `DroidMentorApplication.httpClient` now shares this client with the app's remote source.

Eight tests check the production engine, build-variant logging and cleanup, relative URL resolution, JSON conversion, timeout propagation, inspectable HTTP failures, redirect prevention, debug log privacy on success and failure, and absent release logging. Requests use Ktor MockEngine and runtime-generated fictitious sensitive values held only in memory. Real API calls belong to subsequent M2 tasks.

Verify both build variants and run the existing device tests with an emulator connected:
```bash
./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain
```

The M2-05 verification passed 14 unit tests in debug, the same 14 in release, and 32 device tests. Both app variants built successfully; debug lint reported 0 errors and 59 warnings.

## Gemini JSON models (M2-06)

`data/remote/dto` contains serializable request, content, text-part, optional generation configuration, response, candidate, usage, prompt-feedback and error-envelope models. The supported fields follow the [Gemini REST reference](https://ai.google.dev/api/generate-content). Requests require both `systemInstruction` and the full supplied `contents`; `@SerialName` writes the persona as `system_instruction`, separately from user messages. The body contains no credential, cached content or server session field. Temperature and output-token overrides are optional and have no invented default values.

`ConversationTurn.toContent()` converts domain roles explicitly to `user` and `model` and preserves the entire turn text. The mapper belongs to the data layer; the domain retains no API dependencies. `HistoryPayloadBuilder` validates turn order and message states before this conversion, as described below.

Response fields preserve absent content, ordered text parts, thought markers, token counts and prompt-block reasons. Server finish and block reasons remain strings so new values do not break decoding. Unsupported response fields are ignored; incorrect types for supported fields are decoding failures. `ApiError` holds raw server text only in memory and hides all fields in its diagnostic text representation. Typed outcomes are handled by the M2-09 mapper described below.

Twelve DTO tests check exact field names, separate instructions, unchanged ordered history, optional settings, required request fields, multipart responses and metadata, missing fields, prompt and candidate blocks, future reason values, non-text parts, malformed payloads, safe error diagnostics, and conversion through the actual Ktor client. No real Gemini request or credential is used.

The full verification command shown above passed in 41 seconds: 26 unit tests in debug, the same 26 in release, and 32 device tests, with no failures, errors or skips. Both app variants built successfully; debug lint reported 0 errors and 59 warnings.

## History preparation (M2-08)

`HistoryPayloadBuilder.build(chatId, messages)` is a pure Kotlin domain operation. The caller supplies the complete saved history including the current pending user row. The builder sorts a separate list by `seq`, preserves every turn's role and entire text, and returns `HistoryPayloadResult.Success` only after all rows pass validation. A rejected history returns `Invalid` with a safe `HistoryValidationError` category and no partial payload.

Messages must have distinct usable local IDs, belong to the requested chat, and have distinct non-negative sequence positions. Sequence values need not be contiguous, and timestamps never affect order. Roles must begin with user, alternate strictly, and end with user. Every historical user and model row must be `SENT`; the final user must be `SENDING`. Blank text, stale error metadata, unresolved failures and invalid model states block preparation. No older turn is filtered or truncated, and the input is never changed.

Retry remains the repository and DAO's responsibility: they must reactivate the same failed row as `SENDING` and clear its error before rebuilding. The builder never appends a replacement row or bypasses a failed turn. Successful output contains only domain roles and text; API DTO conversion remains in the data layer. The production domain has no imports or framework dependencies.

Twenty-two tests cover empty and first histories, `2N + 1` counts, shuffled sequence order, tied/backwards timestamps, large and non-contiguous Long positions, 2,001 retained turns and long Unicode text, invalid ownership/IDs/sequences/roles/text, missing pending users, all 27 three-turn status combinations, every stale error category, unresolved failures, same-row retry, input preservation and independent builder calls.

The full verification command shown above passed in 31 seconds: 48 unit tests in debug, the same 48 in release, and 32 device tests, with no failures, errors or skips. Both app variants built successfully; debug lint reported 0 errors and 59 warnings.

## Mentor persona (M2-10)

`domain/MentorPersona.kt` defines the single `MentorPersona.SYSTEM_INSTRUCTION` constant. The persona describes a patient senior Android engineer helping a university student, answering in the student's language with clear explanations and small Kotlin examples. Examples use English identifiers and comments explaining each statement. Advice should favour official Android and Kotlin documentation, respect existing dependency versions, explain trade-offs and state uncertainty.

The text includes the course's manual dependency injection, Ktor, kotlinx.serialization, Room, Preferences DataStore, Compose and coroutine requirements. It also asks for stateless full-history requests, a separate repeated `system_instruction`, connectivity checks, typed failures and no credential disclosure. These are instructions for the mentor; the app enforces its own constraints in code.

The constant contains no model name or credential and has no framework dependencies. `GeminiRemoteDataSource` attaches it to `system_instruction` on every default call. The text has been reviewed and its request placement is covered by simulated data-source tests; live mentor responses remain to be checked with the user's BYOK project.

The full verification command shown above passed in 27 seconds: the existing 48 unit tests in debug, the same 48 in release, and 32 device tests, with no failures, errors or skips. Both app variants built successfully; debug lint reported 0 errors and 59 warnings.

## Typed API outcomes (M2-09)

`data/remote/ApiResult.kt` defines success and ten failure outcomes: invalid key, bad request, rate limit, server error, unavailable network, timeout, malformed response, empty response, blocked response and unknown failure. Failures contain no raw response text or exception. Rate limits retain only optional numeric retry metadata, and success hides its value in diagnostic text. `toMessageErrorCategory()` converts each failure to the domain enum already supported by Room; success has no error category. `RoomChatRepository` saves that category in the failed user row.

`GeminiResultMapper.execute` wraps one supplied Ktor operation together with response reading and decoding. HTTP status takes priority over error-body content: 400 maps to bad request, 401/403 to invalid key, 429 to rate limit, every 5xx to server error, and other non-success statuses to unknown. Ktor request, connection and socket timeouts are distinct from ordinary IO failure. JSON conversion failures are malformed responses. Ordinary unexpected exceptions become unknown; caller cancellation propagates, including cancelled work reported by an engine as IO failure.

The mapper reads `Retry-After` as unsigned decimal seconds or a standard RFC 1123 HTTP-date. A date uses the injected current clock, rounds fractional seconds up and clamps passed dates to zero. Missing, invalid and overflowing guidance remains null. It performs no automatic retry or wait.

Successful JSON uses the existing client converter. Explicit prompt blocks and [policy finish reasons](https://ai.google.dev/api/generate-content#FinishReason) produce blocked outcomes even if text is present. Only the first candidate is selected, and only its non-thought text parts are joined in their original order, with no trimming or invented separator. Missing or blank answer text is empty output; a supplied non-model author is malformed. Normal completion, optional stopping metadata and `MAX_TOKENS` allow usable text; unknown stopping reasons remain unknown outcomes.

Twenty MockEngine/unit tests cover usable multipart text, empty and malformed responses, all 100 statuses in the 5xx range, required client statuses, numeric/date/invalid retry headers, one attempt without backoff, prompt and candidate blocks, unknown reasons, unsupported media types, transport failures, the actual Ktor timeout timer, failed response-body reads, HTTP status validation exceptions, coroutine cancellation, persisted categories and diagnostic privacy. All simulated operations use an in-memory engine or a directly injected failure. `GeminiRemoteDataSource` now supplies its POST operation to this mapper.

The full verification command shown above passed in 21 seconds: 68 unit tests in debug, the same 68 in release, and 32 device tests, with no failures, errors or skips. Both app variants built successfully; debug lint reported 0 errors and 59 warnings.

## Gemini data source (M2-07)

`data/remote/GeminiRemoteDataSource.kt` sends `POST /v1beta/models/{model}:generateContent` through the shared configured Ktor client. Its single `MODEL_NAME` constant selects `gemini-3.5-flash-lite`, reconfirmed in the [official model documentation](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite) on 2026-10-11. Access and quotas for the user's BYOK project still require live verification.

`generateContent(history, systemInstruction, apiKey)` accepts the complete prepared domain turn list and defaults the instruction to `MentorPersona.SYSTEM_INSTRUCTION`. Every call creates a fresh request with all turns and the separate `system_instruction`. The key is trimmed in memory, validated for empty or unsafe header characters, and added only to that request's `x-goog-api-key` header. No key or session identifier is placed in the URL or JSON, and the source keeps no key, history or returned response ID between calls. It leaves client ownership with the service locator.

The constructor requires a suspending `isConnected` callback without an assumed-online default. It is called immediately before each POST inside the existing result mapper; offline calls stop before HTTP and become `NetworkUnavailable`. `ConnectivityObserver::isConnected` supplies the Android implementation without blocking the main dispatcher. Blank instructions, invalid role alternation, an absent final user turn and blank message text produce `BadRequest` before any HTTP operation. Saved-message state validation remains in the domain builder and repository.

The source uses `GeminiResultMapper` for its complete request/response operation, so HTTP, IO, timeout, malformed, empty and blocked outcomes remain typed. Cancellation is preserved before local guards and during POST. Thirteen integration tests check actual endpoint, headers, JSON, repeated persona/full history, changed per-request credentials, 501 retained turns, input guards, current connectivity and provider failures, HTTP/response/transport errors, cancellation, safe debug logs, key trimming and shared client ownership. Tests use MockEngine and generated fictitious credentials held only in memory. The Android connectivity provider, repository and manual service locator are implemented below; ViewModel/UI integration remains pending.

The first focused run was interrupted while KSP waited on offloaded build-cache files. Regenerating the build files resolved the wait. The complete verification used:
```bash
./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon
```
It finished with `BUILD SUCCESSFUL in 1m 9s`: 81 unit tests in debug, the same 81 in release, and 32 device tests, with no failures, errors or skips. Both app variants built successfully; debug lint reported 0 errors and 59 warnings.

## Android connectivity (M2-23)

`data/connectivity/ConnectivityObserver.kt` uses the application `ConnectivityManager`. Its cold `status: Flow<ConnectivityStatus>` registers a default-network callback while collected and unregisters it on cancellation. It observes the default route used by ordinary requests rather than treating any secondary network as usable. `CONNECTED` requires both `NET_CAPABILITY_INTERNET` and `NET_CAPABILITY_VALIDATED`; availability alone, captive portals, missing services and failed checks remain `DISCONNECTED`. These flags follow the [Android network-state guidance](https://developer.android.com/develop/connectivity/network-ops/reading-network-state).

Callback capability values are used directly rather than queried synchronously inside `onAvailable`. `DefaultNetworkTracker` keeps the current network identity, ignores delayed loss/capability events from an older route and prevents an initial snapshot from overwriting a newer callback. Statuses are deduplicated and conflated. Registration, initial reads and cleanup run on IO. The suspend `isConnected()` makes a fresh system check even without a status collector, requires the same two flags and rejects a network handover observed during the check.

`GeminiRemoteDataSource` now accepts a suspending connection callback, matching `observer::isConnected`. Ten local tests cover validation combinations, initial states, loss/recovery, handovers, stale events, snapshot races and independent subscriptions. Three Android tests check the main-dispatcher caller, 110 successive callback registrations with cancellation, and actual emulator offline/recovery with the observer wired into the source. The offline call reaches no HTTP engine; after reconnection, one request reaches MockEngine. Test networking changes are restricted to an emulator and original Wi-Fi/mobile-data settings are restored. No real Gemini request or credential is used.

The full clean verification passed in 50 seconds with 91 unit tests in each build variant and 35 device tests. After using an owned auto-closing shell stream in the device test, the final verification was:
```bash
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon
```
It finished with `BUILD SUCCESSFUL in 20s`: 91 unit tests in debug and the same 91 in release remained passing, all 35 device tests ran and passed, and debug lint reported 0 errors and 59 warnings. Original emulator Wi-Fi and mobile-data settings were checked restored. `DroidMentorApplication.connectivityObserver` now owns the shared observer; offline-banner integration remains pending. Repository integration is described below.

## Offline-first chat repository (M2-15)

`ChatRepository` exposes local create/rename/delete commands, new-message sending, same-row retry, and Room-backed chat/message observation. `RoomChatRepository` receives its database, settings, source, suspend connection check, clock and local ID generator manually. `ChatSummary` keeps history previews independent of Room. Read streams return `ChatReadResult` with persisted domain data or a storage failure; command acknowledgements contain no API result or model answer. `Stored` means the requested local outcome was persisted, including a failed network attempt. The UI reads that outcome from the saved message's status and error category.

Each chat has its own mutex. A competing send, retry, rename or delete is rejected as busy rather than queued; a different chat remains free to send. Preparation runs in a short Room transaction: it checks local existence and unresolved rows, validates the complete prospective history, and inserts one `SENDING` user or reactivates the selected final `FAILED` user with its original ID, sequence, text and creation time. The transaction ends before HTTP. A second repository object also sees the persisted unresolved guard when attempting to append a new turn.

The key and connectivity are checked before the optimistic write and checked again afterwards. Initially missing keys, unavailable preferences or offline state leave the draft and stored history unchanged. Changes after preparation settle the saved user as failed. All completed exchanges and the current user are rebuilt from Room and sent through the actual source with its final connection gate. Success uses the DAO's atomic reply insert/user `SENT` transaction; network failures save only a safe error label. Configured credential text is rejected in incoming user turns and legacy request histories, and an echoed credential in a model response is discarded as a blocked failure.

Cancellation remains cancellation, but a local cleanup section under `NonCancellable` first attempts to mark the owned pending user `FAILED/UNKNOWN` and then releases the chat gate. Local write failures return `StorageError`; cleanup cannot guarantee persistence when storage itself is unavailable. Room flow failures are caught upstream, preserving the consumer's own exception behaviour. No background application coroutine, automatic network retry, history truncation, server session or schema change is introduced.

Twenty-three device integration tests use the production Room schema and actual Ktor source with MockEngine. They cover local operations offline, pending-before-HTTP observation, complete stateless history, all ten persisted API failure categories, retry identity and error clearing, same-chat and independent-chat concurrency, the persisted guard across repository objects, initial and changed prerequisites, invalid histories/retry targets, cancellation at two stages, atomic completion rollback, credential boundaries, explicit storage errors and collector exception transparency. One test also uses the real Preferences DataStore implementation with an isolated runtime-generated fictitious credential and removes only its own file afterwards.

The first verification failed before compilation because Gradle could not open its generated execution-history cache. Removing only that cache allowed the clean build and initial 19 repository cases to pass in `1m 5s`. After the additional integration checks and upstream Flow handler, final verification used:
```bash
./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon
```
It finished with `BUILD SUCCESSFUL in 39s`: 91 unit tests in debug, the same 91 in release, and all 58 device tests passed, with no failures, errors or skips. Debug lint reported 0 errors and 59 warnings. The manual service locator is now connected; ViewModel/UI integration remains pending, and no real Gemini call or credential was used.

## Manual application graph (M2-01)

`DroidMentorApplication` is the `Application` class registered by `android:name` in the manifest. It constructs the existing database, dedicated key DataStore, Ktor client, connectivity observer, settings repository, Gemini source and chat repository by hand. Each property uses Kotlin's synchronised `by lazy`, so callers share one process-owned instance and startup does not eagerly open history or send requests. `Context.app` follows `applicationContext`, including through wrappers, rather than retaining an Activity.

The source and repository both receive `connectivityObserver::isConnected`; the repository receives the same Room database and settings repository exposed by the locator. The Application stores no credential value, history snapshot or server session. The graph owns its shared resources for the process lifetime, while network callbacks remain scoped to Flow collectors. `DroidMentorApplication` also implements the screen-facing `DependencyContainer` contract completed in M2-02 below.

Four Android tests verify the actual manifest-created application, wrapper lookup, 24 concurrent graph accesses, the existing single-instance DataStore provider, the allowed Ktor engine and database name, and a local repository/database binding round trip. That round trip creates and deletes only its own generated test row. Tests make no HTTP request and never read or change a configured key value.

Full verification used:
```bash
./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon
```
It finished with `BUILD SUCCESSFUL in 37s`: 91 unit tests in debug, the same 91 in release, and all 62 device tests passed, with no failures, errors or skips. Debug lint reported 0 errors and 59 warnings. After instrumentation left the target APK uninstalled, `:app:installDebug` succeeded in 44 seconds. A subsequent cold launch reported `Status: ok`, `LaunchState: COLD` and 395 ms. The app remains on minimal destination content; navigation is connected below, while separate M2 tasks add screen ViewModels and screen content.

## Manual ViewModel factories (M2-02)

`di/DependencyContainer.kt` exposes the chat and settings repository interfaces and a `Flow<ConnectivityStatus>`. `DroidMentorApplication` implements it with the existing shared repositories and cold connectivity stream, so screen constructors need no concrete Room, DataStore, client or Android connectivity component.

`di/DependencyViewModelFactory.kt` builds a factory for one ViewModel type with `viewModelFactory { initializer { … } }`. A screen supplies its constructor lambda and selects the dependencies it needs. By default the initializer resolves the container from the standard Application creation extras supplied by an Android ViewModel owner. Tests can pass a fake container explicitly and use empty extras. Missing or incompatible Application configuration fails with a safe construction diagnostic. AndroidX owns ViewModel lifetime: the same store reuses its instance, while a separate store gets a separate instance.

Six local tests verify lazy construction, exact fake injection without Android storage, owner scoping, missing dependencies, unsupported types and constructor failure propagation. Two Android tests verify the manifest-created Application and an incompatible Application. Only test fixtures are constructed here; actual screen ViewModels and UiState belong to M2-22.

Full verification used:
```bash
./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon
```
It finished with `BUILD SUCCESSFUL in 37s`: 97 unit tests in debug, the same 97 in release, and all 64 device tests passed with no failures, errors or skips. Debug lint reported 0 errors and the same 59 existing warnings. No dependency version, real credential or live Gemini request was introduced. Debug reinstallation succeeded in 4 seconds and a cold launch reported `Status: ok`, `LaunchState: COLD` and 342 ms.

## Navigation graph (M2-16)

`ui/navigation/DroidMentorNavHost.kt` registers `Title`, `ChatHistory`, `ActiveChat/{chatId}`, `About` and `Settings`, with Title as the M2 start destination. Paths follow Figure 1, page 2 of the [official course assignment](https://github.com/isel-leic-pdm/2627i/blob/main/assignments/PDM-2627-Assignment.pdf): Title opens history, settings or information; history opens a selected or newly created conversation by its local ID. Return callbacks use `popBackStack`, preserving the existing Title and history entries. Navigation Compose handles system Back, and `launchSingleTop` avoids duplicate top entries from repeated events. `DroidMentorRoutes` centralises names and safely URI-encodes the required chat ID; the graph receives its original decoded value and ignores blank selections.

MainActivity now hosts this graph. Separate Compose content slots receive only the callbacks and local ID needed by each screen, so later screen tasks can provide their real UI without controller access or business logic in the graph. Default content is intentionally minimal: the app still displays the Title label, with destination labels and return actions available for subsequent screen integration. The Title menu is implemented below; history, settings, active chat and their ViewModels are implemented in separate M2 tasks. `rememberNavController` restores saved composition state and the previous back stack; persistent active-session restoration remains M3-06.

Eight Android graph tests use test-only content to exercise registration, all Figure 1 branches and returns, system Back, repeated events, reserved and percent-like ID characters, invalid selection and saved controller restoration. One further test launches the actual MainActivity and checks the initial Title content has no Back button. The first full run failed in 45 seconds on overlapping controls in test fixtures; arranging those controls in separate rows fixed the interactions, and all eight focused graph tests passed in 23 seconds.

Final verification used:
```bash
./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon
```
It finished with `BUILD SUCCESSFUL in 46s`: 97 unit tests in debug, the same 97 in release, and all 73 device tests passed, with no failures, errors or skips. Debug lint reported 0 errors and the same 59 existing warnings. No new library, configured key access or real Gemini request was needed for navigation. Debug reinstallation succeeded in 4 seconds and a cold launch reported `Status: ok`, `LaunchState: COLD` and 356 ms.

## API key

This app is a BYOK (Bring Your Own Key) client. The user obtains an API key from [Google AI Studio](https://aistudio.google.com/) and enters it in the app's Settings screen. The API key must never be committed to the repository.

## Build Instructions

1. **Gradle runtime:** install JDK 17, as required by `gradle/gradle-daemon-jvm.properties`. Java and Kotlin also compile to JVM 17 bytecode. Android lint bundled with the current Android Gradle plugin fails under JDK 25.
2. **Android SDK:** install Android SDK Platform 35 and Android SDK Platform-Tools from Android Studio's SDK Manager. Set `sdk.dir` in the untracked `local.properties` file to your local SDK directory.
3. **Device:** Android API 26 or newer. For an emulator, also install Android Emulator and a compatible system image, then start an Android Virtual Device.
4. On macOS, select your installed JDK 17 for command-line builds:
   ```bash
   export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
   "$JAVA_HOME/bin/java" -version
   ```
5. To build the project from the command line:
   ```bash
   ./gradlew :app:assembleDebug
   ```
6. With an emulator or device connected, install the debug app:
   ```bash
   ./gradlew :app:installDebug
   ```
   Open **DroidMentor** from the device's app launcher. The current scaffold displays the app name.

To check the M2 dependency configuration, build the app and instrumentation APK, run available unit tests and run Android lint:
```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```
The unit-test task runs the settings repository error-path tests, HTTP client configuration tests, Gemini DTO contract tests, history validation tests, typed API outcome tests, Gemini data-source integration tests and network event-ordering tests. Building the instrumentation APK does not execute device tests; use `:app:connectedDebugAndroidTest` with an emulator or device connected.

## Main menu (M2-17)

`TitleScreen` displays a local conversation mark, the application title and three resource-backed actions: Conversations, Settings and About. It receives callbacks only. The layout scrolls when space is limited and uses the existing theme. Verification on 2026-10-12: debug/release builds, unit tests, lint and instrumentation APK compilation passed (`BUILD SUCCESSFUL in 1m 23s`). No emulator was opened; the real-menu Android test is compiled but not executed in this session.
