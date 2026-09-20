# DroidMentor

A BYOK (Bring Your Own Key) Android LLM client built for the Mobile Devices Programming course at Instituto Superior de Engenharia de Lisboa, Winter Semester 2026/2027. It uses Google Gemini's REST API in stateless mode and acts as a senior Android engineering mentor.

## Group PastaGPT

| Name | Student number | Email | GitHub |
| :--- | :--- | :--- | :--- |
| [Student Name 1] | [00000] | [email] | [@handle] |
| [Student Name 2] | [00000] | [email] | [@handle] |
| [Student Name 3] | [00000] | [email] | [@handle] |

## Deliveries

| Milestone | Tag | Due date | Video |
| :--- | :--- | :--- | :--- |
| 1 — Project plan | mentor_1 | 2026-09-21 | — (see PROJECT_PLAN.md) |
| 2 | mentor_2 | 2026-10-12 | _pending_ |
| 3 | mentor_3 | 2026-11-16 | _pending_ |
| Final | mentor_f | 2026-12-12 | _pending_ |

## Documentation

* [PROJECT_PLAN.md](PROJECT_PLAN.md)
* [VERIFICATION.md](VERIFICATION.md) (to be added)
* [CONTRIBUTING.md](CONTRIBUTING.md) (to be added)

## Tech stack

* Kotlin
* Jetpack Compose + Material 3
* Ktor Client
* kotlinx.serialization
* Room
* DataStore
* Manual dependency injection via the Application class (no Hilt/Dagger)

## Build

1. Minimum Android Studio version
2. JDK 17
3. `minSdk 26`
4. `./gradlew :app:assembleDebug`

## API key

This app is a BYOK (Bring Your Own Key) client. The user obtains an API key from Google AI Studio and enters it in the app's Settings screen. The API key must never be committed to the repository.
