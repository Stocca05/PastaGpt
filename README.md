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

## API key

This app is a BYOK (Bring Your Own Key) client. The user obtains an API key from [Google AI Studio](https://aistudio.google.com/) and enters it in the app's Settings screen. The API key must never be committed to the repository.

## Build Instructions

1. **JDK:** 17
2. **minSdk:** 26
3. To build the project from the command line:
   ```bash
   ./gradlew :app:assembleDebug
   ```
