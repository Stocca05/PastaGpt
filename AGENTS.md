# Project
DroidMentor — a BYOK (Bring Your Own Key) Android LLM client for the Mobile Devices Programming course at ISEL. It talks to Google Gemini's REST API through the `generateContent` endpoint and acts as a senior Android engineering mentor. Root package: `pt.isel.pdm.droidmentor`. Single Gradle module `:app`.

# Non-negotiable constraints
These come from the graded assignment statement. Violating any of them fails the project regardless of code quality. State them as a numbered list:

1. Dependency injection is MANUAL. The `Application` subclass `DroidMentorApplication` is the service locator. Hilt, Dagger, Koin and any other DI framework are FORBIDDEN — no annotations, no plugins, no imports. ViewModels are built with hand-written `ViewModelProvider.Factory` using `androidx.lifecycle.viewmodel.viewModelFactory { initializer { } }`.
2. Networking is Ktor Client ONLY. Retrofit, Volley, OkHttp used directly and `HttpURLConnection` are FORBIDDEN. OkHttp may appear only as the Ktor engine.
3. Serialization is kotlinx.serialization ONLY. Gson, Moshi and Jackson are FORBIDDEN.
4. Chat history persistence is Room ONLY.
5. The Gemini API key is stored in Preferences DataStore ONLY. Never in Room, never in SharedPreferences, never in source constants, never in BuildConfig, never in a committed file.
6. The Gemini API must be used in its STATELESS form. Server-side multi-turn session identifiers are FORBIDDEN. Every request carries the full conversation history in the `contents` array with strictly alternating `user` / `model` roles, starting with `user`.
7. The mentor persona is set through the `system_instruction` field of the request, resent on every call. Never as the first `user` message.
8. The app is offline-first. Room is the single source of truth for the UI. The UI never observes a network result directly. Connectivity is checked BEFORE every API call.
9. Every HTTP error (400, 401, 403, 429, 5xx), timeout, IO failure and malformed response is mapped to a typed result. No exception ever reaches the UI layer.
10. If images are supported, only the file path/URI goes into Room. Base64 image data in the database is FORBIDDEN; Base64 is produced in memory at request build time only.

# Package layout
Document this tree as the mandatory layout:
  DroidMentorApplication.kt, MainActivity.kt
  di/            — DependencyContainer.kt
  domain/        — pure Kotlin, zero Android imports
  data/local/    — Room: entity/, dao/, converter/, relation/
  data/remote/   — Ktor: dto/, HttpClientFactory.kt, GeminiRemoteDataSource.kt, ApiResult.kt
  data/preferences/ — DataStore
  data/repository/
  data/connectivity/
  ui/theme/, ui/navigation/, ui/screens/<screen>/, ui/components/

# Coding conventions
- Kotlin, coroutines and Flow. No RxJava, no LiveData.
- Jetpack Compose with Material 3. No XML layouts except the manifest and resource files.
- Public declarations get KDoc. Private helpers do not need it.
- No `!!`. No `runBlocking` outside tests. No I/O on the main dispatcher.
- Immutable UiState data classes exposed as `StateFlow` from ViewModels.
- English identifiers, English comments, English commit messages using Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`).

# Session rules
- Do exactly one task per session. Do not implement future tasks "while you are there". Do not refactor files outside the task's declared scope.
- Never invent library versions. Always read `gradle/libs.versions.toml` and use the aliases declared there.
- Never print, log or write the API key anywhere.
- Before finishing, run the verification command given in the task and report its real output. If it fails, fix it; do not report success.