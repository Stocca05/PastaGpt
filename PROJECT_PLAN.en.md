# DroidMentor — Project Plan and Timeline

**Author:** Luca Raona (student no. 55603) — solo project, name PastaGPT retained
**Course:** Mobile Devices Programming (PDM) — Instituto Superior de Engenharia de Lisboa
**Academic year:** 2026/2027 — Winter Semester
**Professors:** Prof. Paulo Pereira, Prof. Diogo Cardoso
**Final deadline:** December 12, 2026
**Document:** Milestone 1 deliverable (tag `mentor_1`)

| Version | Date | Author | Status | Notes |
|---|---|---|---|---|
| 1.0 | 18/09/2026 | Luca Raona | Approved for M1 | Initial version delivered with `mentor_1` |
| 1.0.1 | 09/10/2026 | Luca Raona | M2 decisions agreed | Complete history, initial schema, sends and errors; no functional task marked complete |
| 1.0.2 | 09/10/2026 | Luca Raona | M2-03 completed | Package layout, Material 3 theme and verified light/dark rendering |
| 1.0.3 | 09/10/2026 | Luca Raona | M2-04 completed | Six domain models; simple English teaching comments across app Kotlin code |
| 1.0.4 | 09/10/2026 | Luca Raona | M2-11 completed | Room entities, enum converters and seven passing entity storage tests |
| 1.0.5 | 09/10/2026 | Luca Raona | M2-12 completed | Production DAOs, latest-message relation, atomic writes and fourteen passing DAO tests |
| 1.0.6 | 09/10/2026 | Luca Raona | M2-13 completed | Production database v1, exported Git-tracked schema and five passing database tests |
| 1.0.7 | 10/10/2026 | Luca Raona | M2-14 completed | Dedicated key DataStore, typed storage outcomes and twelve passing settings tests |
| 1.0.8 | 10/10/2026 | Luca Raona | M2-05 completed | Configured Ktor client, diagnostics without sensitive data and eight client tests passing in debug and release |
| 1.0.9 | 10/10/2026 | Luca Raona | M2-06 completed | Gemini DTOs and data-layer turn conversion, twelve JSON contract tests passed |
| 1.0.10 | 10/10/2026 | Luca Raona | M2-08 completed | Domain history builder, typed rejections and twenty-two passing validation tests |
| 1.0.11 | 10/10/2026 | Luca Raona | M2-10 completed | Centralised mentor persona, teaching tone, commented Kotlin examples and course constraints |
| 1.0.12 | 10/10/2026 | Luca Raona | M2-09 completed | Typed API outcomes, HTTP/response mapper, persisted error conversion and twenty passing tests |
| 1.0.13 | 11/10/2026 | Luca Raona | M2-07 completed | Stateless Gemini data source, mandatory connectivity gate and thirteen passing integration tests |
| 1.0.14 | 11/10/2026 | Luca Raona | M2-23 completed | Validated Android connectivity, callback cleanup, ten local and three device tests passed |
| 1.0.15 | 11/10/2026 | Luca Raona | M2-15 completed | Offline-first Room repository, per-chat sends, same-row retry and twenty-three passing integration tests |
| 1.0.16 | 11/10/2026 | Luca Raona | M2-01 completed | Registered Application, shared lazy manual graph, Context.app and four service locator tests passed |
| 1.0.17 | 11/10/2026 | Luca Raona | M2-02 completed | Application implements DependencyContainer, manual AndroidX factory, six local and two device tests passed |
| 1.0.18 | 11/10/2026 | Luca Raona | M2-16 completed | Five-destination graph matching Figure 1, return paths and saved state, nine Android tests passed |
| 1.1 | *(to be filled in)* | | Draft | Post-M2 revision |
| 1.2 | *(to be filled in)* | | Draft | Post-M3 revision |

> **Maintenance note.** This is a living document: at the end of every milestone the task status must be updated (`☐` → `☑`), along with actual effort and any rescheduling of unfinished tasks. The revision history is tracked in the table above and in the Git log.

---

## Table of contents

1. [Executive summary](#1-executive-summary)
2. [Author and working method](#2-author-and-working-method)
3. [Non-negotiable technical constraints](#3-non-negotiable-technical-constraints)
4. [Reference architecture](#4-reference-architecture)
5. [Working conventions and process](#5-working-conventions-and-process)
6. [Legend for estimates, priority and status](#6-legend-for-estimates-priority-and-status)
7. [Overall timeline](#7-overall-timeline)
8. [Milestone 1 — Planning and foundations (21/09/2026)](#8-milestone-1--planning-and-foundations-21092026)
9. [Milestone 2 — Working end-to-end core (12/10/2026)](#9-milestone-2--working-end-to-end-core-12102026)
10. [Milestone 3 — Advanced features and robustness (16/11/2026)](#10-milestone-3--advanced-features-and-robustness-16112026)
11. [Final Milestone — Consolidation and delivery (12/12/2026)](#11-final-milestone--consolidation-and-delivery-12122026)
12. [Verification strategy](#12-verification-strategy)
13. [Risk register](#13-risk-register)
14. [Requirements → tasks traceability matrix](#14-requirements--tasks-traceability-matrix)
15. [Appendices](#15-appendices)

---

## 1. Executive summary

DroidMentor is a **BYOK** (*Bring Your Own Key*) Android LLM client that gives the user a conversational mentor specialised in Android development. Communication goes through Gemini's REST API, `generateContent` endpoint, **in strictly stateless mode**: no server-side session identifier is allowed, so it is the application that rebuilds and transmits the entire conversation history on every single network interaction.

The application is designed as an **offline-first** experience: the local database (Room) is the single source of truth for the UI, all conversations remain readable without network access, and connectivity is checked *before* every API call, explicitly telling the user that new interactions are unavailable while offline.

The plan is organised across **four milestones** and follows an incremental life cycle with weekly sprints. The underlying strategy is **early verticalisation**: by Milestone 2 a complete vertical slice must exist (UI → ViewModel → Repository → Room + Ktor → Gemini → Room → UI), however minimal, so that the highest integration risks — stateless payload serialisation, manual service locator, consistency between network and persistence — are faced while there is still room to manoeuvre. The features with the highest logical complexity (conversation rewrite, active session restore, image attachments) are concentrated in Milestone 3, while the Final Milestone is dedicated to integration, hardening, regression testing and delivery, with a **planned code freeze on 08/12/2026** that leaves four days of slack before the deadline.

**Declared quality objectives**

| Objective | Verifiable metric |
|---|---|
| Stability | Zero crashes (ANRs/unhandled exceptions) during final regression and video recording |
| Offline-first | All conversations readable with airplane mode on; no empty or errored screens |
| Network robustness | Explicit, tested handling of 400, 401/403, 429, 5xx, timeouts, malformed responses |
| Constraint compliance | No trace of Hilt/Dagger, Retrofit, Gson/Moshi, or Base64 persisted in Room |
| Test coverage | ≥ 80 % on payload-building logic and error mapping |
| Delivery | 4 Git tags (`mentor_1`, `mentor_2`, `mentor_3`, `mentor_f`) created by their respective deadlines |

---

## 2. Author and working method

DroidMentor is a solo project: I build it, **Luca Raona** (student no. 55603), under the project name **PastaGPT**.

**Organising the work.** The WBS tables below group tasks by technical area (Networking, Persistence, UI, Connectivity, Verification, Delivery) — a thematic grouping to navigate the work, not an assignment to different people. A task that touches more than one area at once gets handled in a single sitting, without breaking it up midway, so the reasoning stays coherent start to finish.

**Verification method.** For the highest-risk tasks (marked 🔴 in §6), I write the tests before the implementation, not after — it's the most reliable way not to discover a conceptual error halfway through. For every pull request, I re-read the diff in full the day after writing it, not right away — after a few hours' distance you notice mistakes that stay invisible in the moment.

**Risk management.** The code-freeze buffer (§7, §13) is not extra time for new features: it's the reserve for something going wrong — an illness, another exam landing at the same time — because there's no one else who can absorb a delay on my behalf.

**Working cadence**

| Ceremony | Cadence | Duration | Content |
|---|---|---|---|
| Sprint planning | Monday | 15 min | Pick this sprint's tasks from the WBS, confirm the estimate still looks right |
| Progress log | Wed./Fri. | 5 min | One written line: done / in progress / blocked — useful raw material for the milestone video script later |
| Self-review + retrospective | Sunday | 30 min | Re-read the week's diffs cold, check them against the Definition of Done, adjust next week's plan |
| Milestone review | End of milestone | 60 min | Run the acceptance checklist end to end, record the video, tag |

---

## 3. Non-negotiable technical constraints

These constraints come directly from the assignment statement and act as a **cross-cutting acceptance criterion**: violating them invalidates the delivery regardless of how good the functional result is. Every pull request must be checked against this table.

| # | Constraint | Implementation implication | Anti-patterns to avoid | Tasks addressing it |
|---|---|---|---|---|
| V1 | **Manual dependency injection** through the `Application` class used as a *service locator*. Hilt/Dagger **forbidden**. | `DroidMentorApplication` exposes the singletons (database, DataStore, `HttpClient`, repositories) with *lazy* initialisation. ViewModels are created with hand-written `ViewModelProvider.Factory` (`viewModelFactory { initializer { … } }`). | Any `@HiltAndroidApp`, `@Inject`, `@Module`, `@Provides` annotation; Hilt KSP plugin in `build.gradle.kts`. | M2-01, M2-02 |
| V2 | **Networking exclusively with Ktor Client + Kotlinx Serialization.** | `ContentNegotiation` plugin with `Json { ignoreUnknownKeys = true }`; DTOs annotated `@Serializable`; OkHttp or CIO engine. | Retrofit, Volley, `HttpURLConnection`, Gson, Moshi, Jackson. | M2-05, M2-06, M2-07 |
| V3 | **Chat persistence with Room.** | `chats` and `messages` entities with a foreign key and `onDelete = CASCADE`; DAOs exposing `Flow`; exported and version-controlled schema. | Storing history in `SharedPreferences`, in JSON files, or only in memory. | M2-11, M2-12, M2-13 |
| V4 | **BYOK API key stored with DataStore.** | Dedicated `Preferences DataStore`, separate from any UI preferences; exposed as `Flow<String?>`; must be erasable. | Key in `SharedPreferences`, in Room, in a source constant, in a committed `local.properties`, in `BuildConfig`. | M2-14, MF-06 |
| V5 | **Mandatory stateless mode.** Server-side multi-turn session IDs are forbidden. | Every request carries the full `contents` array with strict alternation of `user` / `model` roles. Conversational state lives **only** in Room. | Using stateful APIs or endpoints (e.g. persistent sessions or *interaction ids*); storing a `conversationId` returned by the service. | M2-08, M2-15 |
| V6 | **Persona set via `system_instruction`.** | The `system_instruction` field is present in every request, with the text centralised in a single constant. No RAG or advanced prompt-engineering techniques are required. | Injecting the persona as the first `user` message of the `contents` array. | M2-10 |
| V7 | **Offline-first experience.** | Room is the single source of truth for the UI; connectivity is checked *before* the call; an explicit message is shown when offline. | Screens that depend on a network response to populate; silent `try/catch` that leaves the UI blank. | M2-15, M2-23, M2-24 |
| V8 | **Graceful HTTP error handling** (e.g. 429, 500). | Typed `sealed interface` mapping of outcomes; no exception propagated to the UI; understandable user feedback. | Crashing on `ClientRequestException`; raw technical messages shown to the user. | M2-09, M3-08, M3-09 |
| V9 | **Images (optional, valued requirement):** only the **file URI/path** is persisted in Room. | Files stored in the app's local storage; Base64 `inline_data` encoding built **in memory** only at request time and never written to the database. | A `TEXT` column holding the Base64 string; a `BLOB` with the image bytes. | M3-11 … M3-15 |
| V10 | **Deliveries via Git tags** `mentor_X` on the repository, with full access granted to the professors and a root `README.md` identifying you. | Annotated tags pushed to the remote; README updated at every milestone with the video link. | Lightweight tags created locally and never pushed; missing or incomplete README. | M1-01, M1-02, M1-09, M2-31, M3-18, MF-11 |

---

## 4. Reference architecture

### 4.1 Layering

Three-layer architecture with unidirectional data flow (UDF), without any DI framework.

```
┌──────────────────────────────────────────────────────────────────┐
│  UI  (Jetpack Compose + Navigation)                              │
│  TitleScreen · ChatHistoryScreen · ActiveChatScreen              │
│  AboutScreen · SettingsScreen                                    │
│        ▲ UiState (StateFlow)          │ user events              │
├────────┴───────────────────────────────▼─────────────────────────┤
│  PRESENTATION  ViewModel + immutable UiState                     │
│  created by hand-written ViewModelProvider.Factory               │
├──────────────────────────────────────────────────────────────────┤
│  DOMAIN  Chat · Message · Role · HistoryPayloadBuilder           │
│          (pure Kotlin, no Android dependencies)                  │
├──────────────────────────────────────────────────────────────────┤
│  DATA                                                            │
│   ChatRepository ──► Room (SINGLE SOURCE OF TRUTH)               │
│        │                                                         │
│        └──────────► GeminiRemoteDataSource (Ktor)                │
│   SettingsRepository ──► DataStore (BYOK key)                    │
│   ConnectivityObserver ──► ConnectivityManager                   │
├──────────────────────────────────────────────────────────────────┤
│  DroidMentorApplication  ← SERVICE LOCATOR (lazy singletons)     │
└──────────────────────────────────────────────────────────────────┘
```

### 4.2 Core principle: Room as the source of truth

The UI **never observes** a network call result directly. The cycle is:

1. The user sends a message → the repository checks the key, connectivity and absence of unresolved messages, then inserts it into Room with status `SENDING`.
2. The DAO's `Flow` notifies the UI, which displays the message right away (*optimistic update*).
3. The repository checks connectivity, builds the payload with **all** the history and calls Gemini.
4. On success, a Room transaction inserts the `MODEL` reply and marks the user message `SENT`. On failure, the message becomes `FAILED` with a persisted error category.
5. The UI updates again by observing Room only.

This choice satisfies V3, V5 and V7 simultaneously and makes the offline-reading requirement trivial.

### 4.3 Technology stack

| Area | Choice | Rationale |
|---|---|---|
| Language | Kotlin + Coroutines/Flow | Course standard; structured concurrency |
| UI | Jetpack Compose + Material 3 | Declarative, aligned with modern practices |
| Navigation | Navigation Compose with typed routes | Explicit graph, deep link to Settings |
| Networking | **Ktor Client** (OkHttp engine) | Constraint V2 |
| Serialisation | **Kotlinx Serialization** | Constraint V2 |
| Persistence | **Room** | Constraint V3 |
| Preferences/secrets | **DataStore (Preferences)** | Constraint V4 |
| Images | Coil (rendering only) | Asynchronous loading from a local URI |
| Testing | JUnit 4, `kotlinx-coroutines-test`, Ktor `MockEngine`, in-memory Room, Compose UI Test | Coverage across all three levels of the pyramid |
| CI | GitHub Actions | Automatic build and tests on every PR |

### 4.4 Project configuration

| Parameter | Value | Note |
|---|---|---|
| `minSdk` | 26 | Enables `java.time` and modern storage APIs |
| `targetSdk` / `compileSdk` | Latest stable available | To be pinned in `libs.versions.toml` |
| Modules | Single `:app` module with per-layer packages | Modularisation is not required; avoids unnecessary complexity |
| Permissions | `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA` (only if the optional requirement is implemented) | No storage permission needed thanks to the Photo Picker |

---

## 5. Working conventions and process

### 5.1 Repository and branching

- **Repository:** `pastagpt-droidmentor` (private), with full access granted to Prof. Paulo Pereira and Prof. Diogo Cardoso.
- **Model:** *trunk-based* with short-lived branches. `main` is always buildable and green in CI.
- **Branch naming:** `feat/<task-id>-<slug>`, `fix/<slug>`, `docs/<slug>`, `chore/<slug>`.
  Example: `feat/m2-08-history-payload-builder`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`), message in English, task reference in the body.
- **Pull requests:** used as a self-review checkpoint even solo — open a PR from the feature branch, let the constraint checklist template run against it, and merge only after re-reading the diff once yourself, ideally the next day. Recommended maximum PR size: ~400 changed lines.
- **Tags:** annotated and explicitly pushed.
  ```bash
  git tag -a mentor_1 -m "Milestone 1 - Project plan"
  git push origin mentor_1
  ```

### 5.2 Definition of Done (per task)

A task is *Done* only if all of the following hold:

- ☐ The code compiles with no new warnings and passes `./gradlew lint`.
- ☐ The tests required by the verification strategy for that level are in place.
- ☐ The full suite (`./gradlew test connectedAndroidTest`) is green locally and in CI.
- ☐ The diff has been re-read in full, after a break, before merging (see §2 on self-review).
- ☐ No constraint from section 3 has been violated (explicitly checked during review).
- ☐ No secret, key or token has reached the repository or the logs.
- ☐ The affected documentation (README, ADR, this plan) is updated in the same commit.

### 5.3 Definition of Done (per milestone)

- ☐ All planned tasks are *Done* or formally rescheduled with a written justification.
- ☐ The application installs and is usable on a physical device and on an emulator.
- ☐ The video (where required) is **5–7 minutes** long and covers every required point.
- ☐ The root `README.md` contains your identification and the video link.
- ☐ The `mentor_X` tag is created **and pushed** by the deadline.
- ☐ The milestone's acceptance checklist is filled in within this document.

---

## 6. Legend for estimates, priority and status

**Difficulty**

| Symbol | Level | Operational meaning |
|---|---|---|
| 🟢 | Low | Straightforward work, well-known APIs, negligible technical risk. Can be done solo. |
| 🟡 | Medium | Requires design work or integration across several components. Careful review advised. |
| 🔴 | High | Critical logic, high risk of regression or conceptual error. **Tests written before the implementation are mandatory** (the solo substitute for pair programming — see §2). |

**Priority**

| Code | Meaning |
|---|---|
| **M** | *Must* — explicit requirement of the statement: its absence compromises the grade |
| **S** | *Should* — expected quality, strongly recommended |
| **C** | *Could* — valued optional requirement or improvement |

**Estimate:** expressed in **ideal person-hours** (actual focused work, interruptions excluded). Recommended conservative conversion factor: **1 ideal hour ≈ 1.4 calendar hours**.

**Status:** `☐` to do · `◐` in progress · `☑` done · `⊘` rescheduled.

---

## 7. Overall timeline

| Phase | Period | Sprints | Focus | Estimated ideal hours |
|---|---|---|---|---|
| **Milestone 1** | 18/09 → **21/09/2026** | Sprint 0 | Planning, repository, project foundations | ~11 h |
| **Milestone 2** | 22/09 → **12/10/2026** | Sprints 1–3 | Architecture, stateless networking, persistence, complete vertical slice, verification strategy | ~92 h |
| **Milestone 3** | 13/10 → **16/11/2026** | Sprints 4–8 | Deletion, conversation rewrite, active session, resilience, images | ~83 h |
| **Final Milestone** | 17/11 → **12/12/2026** | Sprints 9–12 | Integration, hardening, UX, regression, delivery | ~62 h |
| | | | **Total** | **~248 h, all solo** (~21 ideal h/week over 12 weeks ≈ 29 calendar h/week at the 1.4× factor) |

> **Workload check.** 248 ideal hours solo over the ~12 weeks to 12/12/2026 is about 21 ideal hours a week — roughly 29 calendar hours a week once the 1.4× factor from §6 is applied. That is a substantial load on top of whatever else is running this semester, and it is not evenly spread: Milestone 2 alone is heavier than this average (see its own total below). If the pace does not hold against your real timetable, the place to cut is the *Could* tasks (the optional image requirement, M3-11…M3-15, MF-01) and the *Should* tasks, not the *Must* ones — see §13, risk R4.

### 7.1 Sprint goals

| Sprint | Week | Verifiable goal by end of sprint |
|---|---|---|
| **0** | 18–21/09 | Repository operational, plan approved, `mentor_1` tag pushed |
| **1** | 22–28/09 | Android project builds with all dependencies; service locator working; Gemini DTOs serialise/deserialise correctly under test |
| **2** | 29/09–05/10 | Room + DataStore operational; navigation across the 5 screens; Settings saves and reads back the key |
| **3** | 06–12/10 | **Complete vertical slice:** send message → model reply → persistence → offline re-read. M2 video recorded, `mentor_2` tag |
| **4** | 13–19/10 | Conversation deletion with CASCADE; deterministic message ordering |
| **5** | 20–26/10 | Conversation rewrite working end-to-end (truncation + resend) |
| **6** | 27/10–02/11 | Active session restored at launch; survival across process death |
| **7** | 03–09/11 | Network resilience: retry with backoff, per-message status, "Retry" action |
| **8** | 10–16/11 | Optional image requirement in a demonstrable state. M3 video, `mentor_3` tag |
| **9** | 17–23/11 | Image requirement closed, file life cycle, error hardening |
| **10** | 24–30/11 | UX review, adaptivity, accessibility, performance |
| **11** | 01–07/12 | Full regression across the device matrix; final documentation |
| **12** | 08–12/12 | **Code freeze 08/12.** Critical bugfixes only. Final video, release build, `mentor_f` tag |

### 7.2 Gantt chart

```mermaid
gantt
    title DroidMentor - PastaGPT (solo, Luca Raona) - 2026 Timeline
    dateFormat YYYY-MM-DD
    axisFormat %d/%m

    section Milestone 1
    Repository, plan and foundations    :m1, 2026-09-18, 4d
    Delivery mentor_1                   :milestone, 2026-09-21, 0d

    section Milestone 2
    Service locator and foundations     :m2a, 2026-09-22, 7d
    Ktor networking and Gemini DTOs     :m2b, 2026-09-22, 10d
    Room and DataStore                  :m2c, 2026-09-28, 8d
    UI and navigation                   :m2d, 2026-09-29, 10d
    Vertical slice and verification     :m2e, 2026-10-06, 5d
    Video and delivery mentor_2         :milestone, 2026-10-12, 0d

    section Milestone 3
    Deletion and ordering               :m3a, 2026-10-13, 7d
    Conversation rewrite                :m3b, 2026-10-20, 7d
    Active session and process death    :m3c, 2026-10-27, 7d
    Network resilience                  :m3d, 2026-11-03, 7d
    Images (optional)                   :m3e, 2026-11-03, 14d
    Video and delivery mentor_3         :milestone, 2026-11-16, 0d

    section Final Milestone
    Image closure and hardening         :mfa, 2026-11-17, 7d
    UX, adaptivity and performance      :mfb, 2026-11-24, 7d
    Regression and documentation        :mfc, 2026-12-01, 7d
    Code freeze                         :milestone, 2026-12-08, 0d
    Final video and delivery mentor_f   :milestone, 2026-12-12, 0d
```

---

## 8. Milestone 1 — Planning and foundations (21/09/2026)

> **Week 3 of the semester. Effective window: 4 days.**

### 8.1 Goal

Deliver the project plan required by the statement and set up the working infrastructure. Tasks marked *(pulled forward from M2)* are not required by the Milestone 1 acceptance criteria, but should be done now because they remove friction at the start of Sprint 1: creating the Gradle project skeleton the day before the first feature is the most common cause of slippage in the opening weeks.

### 8.2 WBS — Work Breakdown Structure

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M1-01 | Repository creation | GitHub repo `pastagpt-droidmentor`, private. Invite Prof. Paulo Pereira and Prof. Diogo Cardoso with **full access**. `.gitignore` for Android/Kotlin/IDEA. | M | 🟢 | 0.5 h | — | ☑ |
| M1-02 | Root `README.md` | Your identification: full name, student number, institutional email, GitHub handle. Project title and course reference. Placeholders for the milestone video links. | M | 🟢 | 0.5 h | M1-01 | ☑ |
| M1-03 | Project plan | Writing and review of this document (`PROJECT_PLAN.md`), with timeline, tasks and acceptance criteria. The statement also asks for "to whom" tasks are assigned: for a solo project that's implicit — every task is mine. Prominent link from the README. | M | 🟡 | 3 h | M1-01 | ☑ |
| M1-04 | Working conventions | `CONTRIBUTING.md` with branching, commit conventions, review policy. `.github/pull_request_template.md` with the section 3 constraint checklist. `CODEOWNERS`. | S | 🟢 | 1 h | M1-01 | ☑ |
| M1-05 | Project board | GitHub Projects with *Backlog / Sprint / In review / Done* columns. Import the tasks from this plan as issues, labelled by milestone and difficulty. | S | 🟢 | 1 h | M1-03 | ⊘ |
| M1-06 | Project scaffolding *(pulled forward from M2)* | Android Studio project: Kotlin, Compose, `minSdk 26`, package `pt.isel.pdm.droidmentor`. Verify the empty app launches on an emulator. | S | 🟡 | 2 h | M1-01 | ☑ |
| M1-07 | Version catalog *(pulled forward from M2)* | `gradle/libs.versions.toml` with Compose BOM, Navigation, Ktor (core, engine, content-negotiation, logging), kotlinx-serialization, Room (runtime, ktx, KSP compiler), DataStore Preferences, Coil, test libraries. Successful Gradle sync. | S | 🟡 | 1.5 h | M1-06 | ☑ |
| M1-08 | Gemini key provisioning | One API key from Google AI Studio (ideally a second, spare one from a different account). Check the free-tier limits and note the quotas (relevant for testing the HTTP 429 path). **No key committed.** | M | 🟢 | 0.5 h | — | ☑ |
| M1-09 | `mentor_1` tag | Final commit, annotated tag, tag push. Verify on GitHub that the tag is visible and that the professors have access. | M | 🟢 | 0.25 h | M1-02, M1-03 | ☑ |

**Estimated total:** ~10.25 ideal hours.

### 8.3 Acceptance checklist (Milestone 1)

Derived literally from the statement's *Acceptance criteria*:

- ☑ The repository is correctly tagged **`mentor_1`** and the tag has been pushed to the remote.
- ☑ The repository contains the **project plan timeline** indicating the planned tasks (the assignment the statement asks for is implicit: solo project, every task is mine).
- ☑ The **root `README.md`** contains your identification.
- ☐ Both professors have **full access** to the repository (verify in *Settings → Collaborators*).
- ☑ The `[email]` and `[@handle]` placeholders in the README template (Appendix C) have been replaced with your real details.

### 8.4 Milestone-specific risks

| Risk | Mitigation |
|---|---|
| A window of only 4 days | Absolute priority to M1-01, M1-02, M1-03, M1-09: they are the only graded tasks. Scaffolding can slip into Sprint 1 with no impact on the grade. |
| Professor invitations not accepted in time | Send the invitations on **day one** and check their status before tagging; record the invitation date in the README. |

---

## 9. Milestone 2 — Working end-to-end core (12/10/2026)

> **Week 6 of the semester. 3 weeks, Sprints 1–3.**

### 9.1 Goal

Build the entire architectural scaffolding and reach a **demonstrable vertical slice**: the user configures their key, opens a new conversation, sends a message, receives the mentor's reply, closes the app, enables airplane mode and reads the conversation back. All the technical constraints from section 3 must already be in force at this stage, because retrofitting a dependency-injection or serialisation decision later costs disproportionately more.

Pay particular attention to the fact that Milestone 2 explicitly requires the video to contain a **description of the elected verification strategy**: the workstream F tasks are neither optional nor deferrable.

### 9.1.1 Agreed M2 decisions — 09/10/2026

These decisions resolve ambiguities in the initial plan. They define the behaviour to implement in M2; they do not mark implementation tasks as complete.

1. **Complete history.** Each request includes every completed exchange and the current user message, ordered by `seq`. Older turns are never removed to save tokens. After N completed exchanges, the next request contains `2N + 1` elements; roles start with `user`, alternate and end with `user`.
2. **Initial Room schema.** Version 1 contains `chats` and `messages`, including `seq` and a nullable error category. The `(chatId, seq)` pair is unique; sequence allocation and insertion are atomic. `imagePath` is excluded from the M2 schema and domain. Later schema evolution follows actual implemented changes, with no migration dedicated to adding `seq`.
3. **One send per chat.** The send button remains disabled during a request. The repository enforces the same rule and rejects new turns while an unresolved `SENDING` or `FAILED` message exists in the chat. Invalid local history never triggers a request.
4. **States and atomic completion.** `SENDING` means a user message is awaiting a reply; `SENT` means a completed exchange; `FAILED` means an attempt ended without a usable reply. Persisting the `MODEL` reply and marking the user message `SENT` happen in the same transaction. Persisted model messages have status `SENT`.
5. **Minimal manual recovery.** M2 includes a *Retry* action to resume a failed send. It reuses the same user row, changes `FAILED` to `SENDING`, clears the error category and rebuilds the complete history. Failed messages are never skipped to append a new turn. Automatic backoff is outside the M2 implementation.
6. **Send prerequisites.** A missing key or unavailable network blocks sending; history remains readable and the draft remains editable. The repository rechecks the prerequisites and verifies connectivity immediately before each call. Connectivity loss during a request produces a persisted failure.
7. **Errors observed through Room.** `ApiResult` stays in the data layer. HTTP errors, timeouts, IO failures, malformed JSON, empty responses and blocked responses are typed outcomes mapped to a category persisted in the message. The UI translates that category into understandable text; neither raw error bodies nor API keys are persisted in Room.
8. **Domain independent of Gemini.** `HistoryPayloadBuilder` produces domain turns containing role and text. Gemini DTOs and turn conversion belong in `data/remote`; the domain imports no Android, Room, Ktor or data-layer DTOs.
9. **Model and persona.** One centralised model name. Initial candidate: `gemini-3.5-flash-lite`, documented by [Google](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite); access for the BYOK project must be checked before live tests. Use stateless `generateContent` and repeat `system_instruction` on every call.

### 9.1.2 Mandatory package layout

```text
pt/isel/pdm/droidmentor/
├── DroidMentorApplication.kt
├── MainActivity.kt
├── di/ — DependencyContainer.kt
├── domain/
├── data/
│   ├── local/ — entity/, dao/, converter/, relation/
│   ├── remote/ — dto/, HttpClientFactory.kt, GeminiRemoteDataSource.kt, ApiResult.kt
│   ├── preferences/
│   ├── repository/
│   └── connectivity/
└── ui/
    ├── theme/
    ├── navigation/
    ├── screens/<screen>/
    └── components/
```

> **M2-03 verification — 09/10/2026.** Package directories prepared; four Kotlin theme files added; initial content, native launch theme and light/dark previews connected. `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:installDebug --console=plain`: `BUILD SUCCESSFUL in 3s`, lint 0 errors and 59 warnings, unit tests `NO-SOURCE`. Cold launch and system light/dark switching verified on the Android 14 emulator (API 34). All 12 text/background colour pairs per palette exceed 4.5:1 contrast; minimum 6.46:1 in light mode and 5.49:1 in dark mode.

> **M2-04 verification — 09/10/2026.** Added `Chat`, `Message`, `Role`, `MessageStatus`, `MessageErrorCategory` and `ConversationTurn`, with read-only fields and public KDoc. The domain has no imports or framework annotations. Checked local string IDs, `Long` timestamps and sequence, typed optional errors, and turns limited to role and text. Every code line in the 11 app Kotlin files now has a simple English explanation; this preference is recorded in `AGENTS.md`. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`: `BUILD SUCCESSFUL in 4s`, lint 0 errors and 59 warnings, unit tests `NO-SOURCE`. This task adds data models; history validation and state transitions remain separate implementation tasks.

> **M2-11 verification — 09/10/2026.** Added `ChatEntity`, `MessageEntity` and `MessageConverters`: local primary keys, `CASCADE` foreign key, `chatId` index and unique `(chatId, seq)` index, typed enums with null error preservation, no credential or image columns. Seven instrumented SQLite tests passed on Android 14 (API 34): completed exchange round-trip and ordering, Long sequence and null error, all error categories, cascade isolation, duplicate sequence rejection, same sequence in different chats, missing-parent rejection. The in-memory database and minimal DAO exist only under `androidTest`; production M2-12, M2-13 and the full M2-28 DAO suite remain pending. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 31s`, 7 tests / 0 failures / 0 errors / 0 skips, lint 0 errors and 59 warnings, unit tests `NO-SOURCE`.

> **M2-12 verification — 09/10/2026.** Added `ChatDao`, `MessageDao` and `ChatWithLastMessage`, with line-by-line teaching comments. Verified full ordered histories, nullable latest-message previews, chat and message Flow updates, child-error changes with an unchanged parent, twenty concurrent per-chat sequence allocations, independent chat sequences, guarded failure/retry on the same row, atomic completion, replay rejection, rollback after reply-ID conflict, duplicate-ID rejection and cascade deletion. The test-only database exposes the production DAOs; the app database M2-13 and full DAO verification task M2-28 remain pending. After regenerating stale duplicated build outputs, `./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 17s`, 21 tests passed (14 DAO + 7 entity), no failures/errors/skips, lint 0 errors and 59 warnings, unit tests `NO-SOURCE`.

> **M2-13 verification — 09/10/2026.** Added the version-one `DroidMentorDatabase`, its two DAO accessors and a persistent builder using application context, a stable app file name and test-only file-name overrides. Main-thread queries and destructive migration fallbacks remain disabled. KSP exports `app/schemas/pt.isel.pdm.droidmentor.data.local.DroidMentorDatabase/1.json`, added to Git tracking and instrumentation assets. The 14 DAO tests now use the real app database in memory; 5 additional tests verify disk close/reopen persistence, retained errors and retry, missing-migration data preservation, main-thread protection and exported schema constraints. `./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 17s`, 26 device tests passed (7 entity + 14 DAO + 5 database), no failures/errors/skips, lint 0 errors and 55 warnings, unit tests `NO-SOURCE`. Service locator M2-01 and final DAO verification M2-28 remain pending.

> **M2-14 verification — 10/10/2026.** Added the single-instance `ApiKeyDataStore`, `SettingsRepository` interface, `DataStoreSettingsRepository` and typed read/write results. Verified null for an unconfigured key, save/reopen persistence, trimming and replacement, blank rejection without writes, selective clear and persisted deletion, Flow save/clear emissions, corrupt-file preservation and singleton reuse. Six unit tests cover read/write failures, unexpected storage exceptions, cancellation and redacted result text; six device settings tests use generated fictitious values and isolated preferences files. Key values are not logged or asserted as printable expected/actual values. `./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 30s`, 6 unit + 32 device tests passed, no failures/errors/skips, lint 0 errors and 59 warnings. Manual service-locator wiring M2-01 and the Settings screen M2-19 remain pending.

> **M2-05 verification — 10/10/2026.** Added `HttpClientFactory` with a client-owned OkHttp engine, JSON accepting unknown fields and omitting nulls, connect 10 s/socket 60 s timeouts and the Gemini base URL. HTTP failure statuses remain inspectable for the later mapper; automatic redirects are disabled. `BuildConfig.DEBUG` enables `Logging` only in debug: INFO without headers/bodies, with a logger that discards the original text and emits a fixed message without URLs or exception details. Eight tests verify the engine and cleanup, build variant, relative URL and JSON, engine timeout propagation, preserved HTTP statuses, blocked redirects, log privacy on success/failure and absent release logging. No real Gemini calls; only MockEngine and fictitious values generated in memory. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 57s`, 14 debug unit tests + the same 14 release unit tests + 32 device tests, zero failures/errors/skips, lint 0 errors and 59 warnings. Service locator wiring M2-01, DTOs M2-06 and typed mapping M2-09 remain pending.

> **M2-06 verification — 10/10/2026.** Added `@Serializable` DTOs for requests, content, text parts, optional generation settings, responses, candidates, token usage, block feedback and error envelopes. `systemInstruction` and `contents` are required; `@SerialName("system_instruction")` keeps the persona separate from history. `ConversationTurn.toContent()` explicitly converts roles in the data layer without changing text or adding API dependencies to the domain. Optional response fields and empty lists represent absent text; finish/block reasons remain strings compatible with future values. DTOs retain the thought marker; the request model contains no attachment, key or session field. `ApiError.toString()` hides all raw server details. Twelve new tests check the JSON contract, unchanged ordered turns, omitted settings, required request fields, multipart responses and metadata, absent fields, blocks, future values, parts without text, malformed JSON rejection, safe diagnostics and the real Ktor converter on MockEngine. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 41s`, 26 debug unit tests + the same 26 release unit tests + 32 device tests, zero failures/errors/skips, lint 0 errors and 59 warnings. No real Gemini calls. History validation M2-08, data source M2-07, typed outcomes M2-09 and the actual persona M2-10 remain pending.

> **M2-08 verification — 10/10/2026.** Added `HistoryPayloadBuilder`, `HistoryPayloadResult` and `HistoryValidationError` categories, with no domain imports. The builder receives a chat ID and complete saved history, sorts a copy by `seq`, preserves every role/text and returns all turns only after validation. It rejects empty input, blank/duplicate/foreign identities, negative/duplicate positions, roles not alternating from user, blank text, unresolved failures, inconsistent states and stale errors. Every earlier turn must be SENT; the last is USER/SENDING without an error. Retry must already reactivate the same row through the repository/DAO: the builder never changes messages or drops failed turns. Distinct Long positions need not be contiguous; no timestamp ordering, truncation or incremental sequence arithmetic that could overflow. Twenty-two tests also cover `2N + 1`, 2,001 turns and long Unicode text, all 27 three-turn status combinations, every stale error category, same ID/seq retry, unchanged input and independent calls. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 31s`, 48 debug unit tests + the same 48 release unit tests + 32 device tests, zero failures/errors/skips, lint 0 errors and 59 warnings. Repository wiring M2-15, data source M2-07, network errors M2-09 and persona M2-10 remain pending.

> **M2-10 verification — 10/10/2026.** Added `domain/MentorPersona.kt` with one `SYSTEM_INSTRUCTION` constant, KDoc and English teaching comments. The text defines a patient, practical senior Android mentor, replies in the student’s language, clear explanations and small Kotlin examples with English comments. It asks for official Android/Kotlin references, respect for existing versions and explicit uncertainty. It includes the course constraints for manual DI, networking/serialization, persistence, credentials, Compose/coroutines and the stateless protocol. The constant contains no model name or credential; the domain still has no imports. Reran the existing suite without adding tests that reproduce a constant’s wording. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 27s`, 48 debug unit tests + the same 48 release unit tests + 32 device tests, zero failures/errors/skips, lint 0 errors and 59 warnings. Text defined and reviewed; sending it through `system_instruction` on every call and checking live responses depend on data source M2-07.

> **M2-09 verification — 10/10/2026.** Added `ApiResult`, `GeminiResultMapper` and exhaustive `toMessageErrorCategory()` conversion. The wrapper accepts one Ktor operation and contains sending, reading and decoding: HTTP 400, 401/403, 429, every 5xx, request/connect/socket timeouts, IO, JSON/conversion errors and unexpected ordinary failures become results without raw bodies or exceptions. Caller cancellation propagates even when the engine reports it as IO. `Retry-After` supports unsigned decimal seconds and RFC 1123 HTTP dates, an injected clock, rounding up and zero for past dates; absent/invalid/overflowing values remain null, with no automatic retry. Prompt/candidate blocks take priority over text; only the first candidate is selected and non-thought text parts are joined without trimming or invented separators. Missing/blank text is `EmptyResponse`, an explicit non-model author is malformed, unknown finish reasons are `Unknown`; STOP/MAX_TOKENS or absent metadata allow usable text. Success diagnostic text hides its value; failures expose only categories and numeric metadata. Twenty new tests also cover all 100 5xx statuses, the actual Ktor timer, a failed response channel, throwing HTTP status validation, cancellation, Room-compatible labels and privacy using fictitious in-memory values. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain`: `BUILD SUCCESSFUL in 21s`, 68 debug unit tests + the same 68 release unit tests + 32 device tests, zero failures/errors/skips, lint 0 errors and 59 warnings. Endpoint/model and wrapper use in data source M2-07, persisted outcomes in repository M2-15 and connectivity gating remain to be integrated; no real Gemini calls.

> **M2-07 verification — 11/10/2026.** Added `GeminiRemoteDataSource` with a shared client, mandatory `isConnected` callback and manually injected result mapper. Every send uses POST `v1beta/models/{MODEL_NAME}:generateContent`; one constant selects `gemini-3.5-flash-lite`, reconfirmed in Google documentation. The key remains only in the call’s `x-goog-api-key` header, with in-memory trimming and rejection of empty or unsafe header input. Every call builds a fresh request containing all turns and separate `system_instruction`, defaulting to `MentorPersona.SYSTEM_INSTRUCTION`; the source retains no session ID, key or history. Invalid wire roles, blank text or blank persona block HTTP; the connection gate runs immediately before POST inside the mapper and offline produces `NetworkUnavailable`. Cancellation is preserved and the source never closes the shared client. Thirteen simulated tests verify actual endpoint/headers/JSON, repeated persona, changed keys without accumulation, 501 complete turns, local guards, dynamic connectivity and provider failures, HTTP/response/transport outcomes, cancellation, safe debug logs, trimming and client reuse. The initial focused check was interrupted while KSP waited to read dataless caches; after regenerating only build files, `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 1m 9s`, 81 debug unit tests + the same 81 release unit tests + 32 device tests, zero failures/errors/skips, lint 0 errors and 59 warnings. No real credentials or Gemini calls. Android connectivity provider M2-23, service locator M2-01 and repository/UI integration remain pending; BYOK model access and quotas require live verification.

> **M2-23 verification — 11/10/2026.** Added `ConnectivityStatus`, `ConnectivityObserver` and an internal callback tracker. Observe the default HTTP route with `registerDefaultNetworkCallback`; both INTERNET and VALIDATED are required. The cold Flow registers one callback per collector, unregisters on cancellation, deduplicates statuses and keeps the latest value. Initial snapshots and callbacks are protected against reordering; old-network events cannot disconnect a replacement route. The suspend `isConnected()` reads a current snapshot on IO even without a collector and rejects a network that changes during the read. The `GeminiRemoteDataSource` gate now accepts the suspend callback; actual wiring is verified by an Android test using MockEngine. Ten local tests cover capabilities, handovers, stale events, snapshot races and independent subscriptions. Three device tests verify main-dispatcher calling, 110 successive subscriptions with cleanup, actual offline and recovery: zero HTTP offline and one simulated call after reconnection. Toggles are emulator-only and restore original Wi-Fi/mobile-data settings. Added the existing MockEngine alias only to Android test dependencies. Initial clean verification succeeded in 50 s; after adjusting test shell-stream ownership, `./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 20s`, 91 unit tests per variant already passed in the clean build and remain valid, all 35 device tests reran and passed, zero failures/errors/skips, lint 0 errors and 59 warnings. Original Wi-Fi/mobile-data settings confirmed restored. Service locator instance M2-01, repository integration M2-15 and UI banner/gating M2-24 remain pending.

> **M2-15 verification — 11/10/2026.** Added the `ChatRepository` contract, `RoomChatRepository`, `ChatSummary` and local `ChatReadResult`/`ChatCommandResult` outcomes. The UI receives only Room observations and local acknowledgements: `Stored` confirms outcome persistence without returning model text or `ApiResult`. Local create/rename/delete work offline. Per-chat mutexes reject concurrent operations without blocking other chats; short transactional preparation checks existence, unresolved rows and complete history before inserting SENDING or reactivating the final FAILED user with the same ID/sequence/text/timestamp. No transaction remains open during HTTP. Key and connectivity are checked before writing and reread/rechecked before the source; changes after preparation become saved failures. Success atomically inserts MODEL and marks the user SENT; failures store only categories. Text containing the configured key is rejected before a new user insert; an echoed credential in a reply is blocked. Cancellation propagates after NonCancellable local cleanup of the owned row to retryable FAILED/UNKNOWN; unavailable storage has an explicit local outcome and can prevent cleanup. Flow catch handles only upstream failures, preserving collector exceptions. Twenty-three real Room + Ktor/MockEngine tests cover optimistic/atomic observation, full history, ten API categories, same-row retry, concurrency and the persisted guard, initial/changed prerequisites, invalid targets, two cancellation stages, completion rollback, credential boundaries, storage errors, Flow transparency and an isolated real Preferences DataStore cycle. The initial build stopped before compilation on unreadable Gradle execution history: regenerated only that cache, then the clean build succeeded in 1m 5s. Final verification `./gradlew :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 39s`, 91 debug unit tests + the same 91 release unit tests + 58 device tests, zero failures/errors/skips, lint 0 errors and 59 warnings. Room schema v1 unchanged; no real credentials or Gemini calls. Service locator M2-01 and ViewModel/UI integration remain pending.

> **M2-01 verification — 11/10/2026.** Added `DroidMentorApplication : Application`, registered through manifest `android:name`. Synchronised `by lazy` properties expose Room, the dedicated Preferences DataStore, Ktor HttpClient, ConnectivityObserver, SettingsRepository, GeminiRemoteDataSource and ChatRepository; constructors are wired by hand and the suspend Android check is supplied to both source and repository. `Context.app` resolves applicationContext without retaining an Activity. No DI framework, startup query/request or eager database opening. Four device tests verify the actual manifest-created class, context wrappers, 24 concurrent accesses to the same graph/provider/engine/database name, and a local repository→shared database round trip: only its own temporary row was created and removed, no credential value was read/written and no HTTP was sent. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 37s`, 91 debug unit tests + the same 91 release unit tests + 62 device tests, zero failures/errors/skips, lint 0 errors and 59 warnings. After instrumentation the target APK was not installed: `:app:installDebug` succeeded in 44 s and Android cold launch reported `Status: ok`, `LaunchState: COLD`, 395 ms. The graph owns process-lifetime resources; network callbacks remain collector-scoped. DependencyContainer contract and ViewModel factories M2-02 remain pending.

> **M2-02 verification — 11/10/2026.** Added `DependencyContainer` with ChatRepository, SettingsRepository and a Flow of ConnectivityStatus; `DroidMentorApplication` implements the interface using the same lazy repositories and existing observer’s cold Flow. `dependencyViewModelFactory<VM>` builds `ViewModelProvider.Factory` with `viewModelFactory { initializer { … } }`: it resolves the container from standard Application creation extras or accepts an explicit test container. Each screen supplies its constructor manually, without a DI framework, global ViewModel cache or credential access. Six local tests verify deferred construction, fake injection without an Application, same-store reuse and separate-owner instances, missing dependencies, unregistered types and constructor failures. Two Android tests verify the real container through APPLICATION_KEY and rejection of an incompatible Application. These ViewModels are test fixtures only; screen ViewModels/UiState M2-22 and navigation M2-16 remain pending. `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 37s`, 97 debug unit tests + the same 97 release unit tests + 64 device tests, zero failures/errors/skips. Lint: 0 errors and the same previous 59 warnings. No new dependency, real credential or live Gemini call. Debug reinstallation succeeded in 4 s and a cold launch reported `Status: ok`, `LaunchState: COLD`, 342 ms.

> **M2-16 verification — 11/10/2026.** Read and visually inspected Figure 1 of the [official course assignment](https://github.com/isel-leic-pdm/2627i/blob/main/assignments/PDM-2627-Assignment.pdf), page 2. Added `DroidMentorRoutes` and `DroidMentorNavHost`, connected to MainActivity and both previews. Five destinations: initial Title, ChatHistory, ActiveChat/{chatId}, About, Settings. The graph supplies Title→History/Settings/About and History→ActiveChat callbacks; returns popBackStack to the previous entry without adding another Title. launchSingleTop avoids duplicate top destinations. The required local ID uses Uri.encode and Navigation argument decoding, carrying no messages or credentials; blank selections do not navigate. rememberNavController retains saved composition state and its back stack. This does not implement the DataStore session M3-06. Compose content slots let separate screen tasks supply content receiving only events and IDs, without the controller: the interactive controls here are test fixtures; the app still displays minimal destination labels without a real menu, Room list, key editor or chat. Eight graph tests cover registered routes and Figure 1 returns, Android Back, repeated events, reserved ID characters, blank selection and saved-state restoration; a ninth checks actual MainActivity and no Back button on the initial Title. The first full run failed in 45 s on five interactions with overlapping test fixture controls; corrected only the test layout, then all eight focused tests passed in 23 s. Final verification `./gradlew clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain --no-daemon`: `BUILD SUCCESSFUL in 46s`, 97 debug unit tests + the same 97 release unit tests + 73 device tests, zero failures/errors/skips. Lint: 0 errors and the same previous 59 warnings. No new dependency, credential or live Gemini call. Debug reinstallation succeeded in 4 s; cold launch confirmed `Status: ok`, `LaunchState: COLD`, 356 ms. Screens M2-17..M2-21 and ViewModels M2-22 remain pending.

### 9.2 WBS — Workstream A: Foundations and manual dependency injection

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M2-01 | Service locator | `DroidMentorApplication : Application` exposing, via `by lazy`, the Room database, DataStore, the Ktor `HttpClient` and the repositories. Extension `val Context.app: DroidMentorApplication`. **No trace of Hilt/Dagger (V1).** | M | 🟡 | 4 h | M1-07 | ☑ |
| M2-02 | ViewModel factories | A `DependencyContainer` interface implemented by the `Application`, so it can be swapped for a fake in tests. `ViewModelProvider.Factory` built with `viewModelFactory { initializer { … } }`. | M | 🟡 | 3 h | M2-01 | ☑ |
| M2-03 | Package structure and design system | Mandatory layout from section 9.1.2. Material 3 theme, palette, typography, spacing, light/dark support. | S | 🟢 | 2 h | M1-06 | ☑ |
| M2-04 | Domain model | `Chat(id, title, createdAt, updatedAt)`, `Message(id, chatId, role, text, seq, status, createdAt, errorCategory?)`, `enum Role { USER, MODEL }`, `enum MessageStatus { SENDING, SENT, FAILED }` and a typed error category. Domain turns with role and text; pure Kotlin, no Android dependencies or Gemini DTOs. `imagePath` excluded from M2. | M | 🟢 | 2 h | — | ☑ |

### 9.3 WBS — Workstream B: Networking (Ktor + Kotlinx Serialization)

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M2-05 | `HttpClient` configuration | OkHttp engine; `ContentNegotiation` with `Json { ignoreUnknownKeys = true; explicitNulls = false }`; `HttpTimeout` (connect 10 s, socket 60 s); `DefaultRequest` with base URL `https://generativelanguage.googleapis.com/`; `Logging` plugin **enabled in debug only** and configured not to print headers or bodies containing the key. | M | 🟡 | 3 h | M2-01 | ☑ |
| M2-06 | Gemini DTOs | `@Serializable` types for `GenerateContentRequest(systemInstruction, contents, generationConfig)`, `Content(role, parts)`, `Part(text)`, `GenerateContentResponse(candidates, usageMetadata)`, `Candidate(content, finishReason)`, `ApiErrorEnvelope(error)`. Convert domain turns in the data layer. Use `@SerialName` for `system_instruction`; attachments excluded from M2. | M | 🟡 | 4 h | M2-05 | ☑ |
| M2-07 | `GeminiRemoteDataSource` | `suspend fun generateContent(history, systemInstruction, apiKey): ApiResult<String>`. `POST /v1beta/models/{model}:generateContent` with the `x-goog-api-key` header. Model name configurable from a single constant. **No session identifier (V5).** | M | 🔴 | 5 h | M2-06 | ☑ |
| M2-08 | `HistoryPayloadBuilder` | Pure domain component producing turns with role and text from every completed exchange and the current `SENDING` user message, ordered by `seq`. Validate strict `user`/`model` alternation and final `user` role. No truncation of older turns; an unresolved `FAILED` blocks new turns and is reactivated on the same row for retry. Convert to Gemini DTOs in the data layer. **Must be exhaustively tested.** | M | 🔴 | 5 h | M2-04 | ☑ |
| M2-09 | Error mapping | `sealed interface ApiResult<out T>` with `Success`, `InvalidApiKey` (401/403), `BadRequest` (400), `RateLimited(retryAfterSeconds)` (429), `ServerError` (5xx), `NetworkUnavailable` (`IOException`), `Timeout`, `MalformedResponse` (`SerializationException`), `EmptyResponse`, `BlockedResponse`, `Unknown`. No network exception escapes the data source to the UI; outcomes are mapped to error categories persisted in Room (V8). | M | 🟡 | 4 h | M2-07 | ☑ |
| M2-10 | Persona `system_instruction` | A single text defining the senior Android mentor: tone, scope of expertise, preference for official best practices, request for Kotlin examples. Centralised in `MentorPersona.kt` (V6). | M | 🟢 | 1 h | M2-06 | ☑ |

### 9.4 WBS — Workstream C: Persistence (Room + DataStore)

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M2-11 | Room entities | `ChatEntity(id, title, createdAt, updatedAt)` and `MessageEntity(id, chatId, role, text, seq, status, createdAt, errorCategory?)` in schema version 1, with `@ForeignKey(onDelete = CASCADE)`, an index on `chatId` and a unique `(chatId, seq)` index. Converters for roles, states and error categories. `imagePath` excluded from M2. | M | 🟡 | 4 h | M2-04 | ☑ |
| M2-12 | DAOs | `ChatDao`: `observeChats(): Flow<List<ChatWithLastMessage>>`, `insert`, `updateTitle`, `deleteById`. `MessageDao`: `observeMessages(chatId): Flow<List<MessageEntity>>`, insertion with atomic `seq` allocation, status/error updates, atomic exchange completion and history reads. Relation queries with `@Transaction` + `@Relation`. | M | 🟡 | 4 h | M2-11 | ☑ |
| M2-13 | Database | `DroidMentorDatabase : RoomDatabase` version 1, schema exported to `app/schemas` and **version-controlled in Git** (essential for testing later migrations). | M | 🟢 | 2 h | M2-12 | ☑ |
| M2-14 | `SettingsRepository` | Dedicated Preferences DataStore: `apiKey: Flow<String?>`, `saveApiKey`, `clearApiKey`. The key is never logged nor included in crash reports (V4). | M | 🟡 | 3 h | M2-01 | ☑ |
| M2-15 | Offline-first `ChatRepository` | Cycle from §4.2: check prerequisites and unresolved messages, optimistic insert, connectivity recheck, complete payload, call, atomic success persistence or failure persistence. One send per chat; manual retry on the same `FAILED` row. The UI observes **only** Room for messages and network errors (V7). | M | 🔴 | 5 h | M2-09, M2-12, M2-23 | ☑ |

### 9.5 WBS — Workstream D: User interface and navigation

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M2-16 | Navigation graph | `NavHost` with routes `Title`, `ChatHistory`, `ActiveChat/{chatId}`, `About`, `Settings`, matching Figure 1 of the statement (including the return paths). Back stack management. | M | 🟡 | 4 h | M2-03 | ☑ |
| M2-17 | Title Screen | Main menu: logo/title, entries *Conversations*, *Settings*, *About*. Entry point of the app when no active chat exists. | M | 🟢 | 2 h | M2-16 | ☐ |
| M2-18 | About Screen | Application name, version, your name, course and institution, third-party library credits. | M | 🟢 | 1.5 h | M2-16 | ☐ |
| M2-19 | Settings Screen | API key field with `PasswordVisualTransformation` and a visibility toggle, save, clear, *key configured / not configured* indicator, non-empty format validation, snackbar feedback. | M | 🟡 | 4 h | M2-14, M2-16 | ☐ |
| M2-20 | Chat History Screen | `LazyColumn` fed by the Room `Flow`: title, last-message preview, relative date. Illustrated empty state. FAB *New conversation*. | M | 🟡 | 4 h | M2-12, M2-16 | ☐ |
| M2-21 | Active Chat Screen | Message list with role-differentiated bubbles, input bar, send disabled during a request or while a failed message is unresolved, minimal *Retry* action, auto-scroll, waiting indicator and `imePadding` handling. Error text derived from the category in Room. | M | 🔴 | 6 h | M2-15, M2-16 | ☐ |
| M2-22 | ViewModels and UiState | One ViewModel per screen with an immutable `UiState` exposed as `StateFlow`; one-shot events (snackbar, navigation) via `Channel`/`SharedFlow`. No business logic inside Composables. | M | 🟡 | 4 h | M2-02, M2-15 | ☐ |

### 9.6 WBS — Workstream E: Connectivity

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M2-23 | `ConnectivityObserver` | Wrapper over the default `ConnectivityManager` network using `registerDefaultNetworkCallback`, `Flow<ConnectivityStatus>`, INTERNET and VALIDATED checks and callback cleanup. Fresh suspend check on IO. Instance to expose in service locator M2-01. | M | 🟡 | 3 h | M2-01 | ☑ |
| M2-24 | Offline gating | Connectivity check **before** every call (V7) and key check in the repository. When offline or the key is missing: sending blocked with understandable feedback; editable draft and fully readable history. Persistent offline banner in Active Chat. | M | 🟡 | 3 h | M2-21, M2-23 | ☐ |

### 9.7 WBS — Workstream F: Verification *(explicitly required in the M2 video)*

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M2-25 | `VERIFICATION.md` document | Formalisation of the strategy described in section 12: levels, tools, exit criteria, device matrix. It is the source from which the verification part of the video is built. | M | 🟡 | 2 h | — | ☐ |
| M2-26 | `HistoryPayloadBuilder` tests | Cases: empty history, first message, N completed exchanges plus a new message (`2N + 1`), `seq` ordering, role alternation and final `user` role, long history retained in full, rejection of invalid history and retry of the `FAILED` on the same row. No session identifier in the payload. | M | 🟡 | 3 h | M2-08 | ☐ |
| M2-27 | Network tests with `MockEngine` | Simulate 200 with a valid payload, 400, 401, 403, 429 with `Retry-After`, 5xx, IO failures, timeouts, malformed JSON, empty responses and blocked responses. Verify typed outcomes and that no network exception propagates to the UI. | M | 🟡 | 4 h | M2-09 | ☐ |
| M2-28 | Instrumented DAO tests | In-memory Room: inserts and `Flow` observation, cascade deletion, `seq` uniqueness and monotonicity under concurrent inserts, atomic exchange completion, error-category persistence and no duplicate rows on retry. | M | 🟡 | 3 h | M2-12 | ☐ |
| M2-29 | Continuous Integration | GitHub Actions workflow on `push` and `pull_request`: `assembleDebug`, `testDebugUnitTest`, `lint`. Status badge in the README. | S | 🟡 | 2 h | M1-07 | ☐ |

### 9.8 WBS — Workstream G: Delivery

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M2-30 | Milestone 2 video | **5–7 minute** recording following the Appendix D script: demonstration of the implemented functionalities, discussion of the most relevant decisions (manual service locator, stateless design, offline-first), **description of the verification strategy**, current project status. Upload to a platform with a stable link, access verified in a private browsing window. | M | 🟡 | 4 h | all M2 | ☐ |
| M2-31 | `mentor_2` tag | Add the video link to the `README.md`, commit, annotated tag, push. | M | 🟢 | 0.5 h | M2-30 | ☐ |

**Estimated Milestone 2 total:** ~92 ideal hours, all solo — about 31 ideal hours a week over the 3 weeks (≈43 calendar hours a week at the 1.4× factor). This is the heaviest sprint block in the whole plan; see the workload note in §7.

### 9.9 Acceptance checklist (Milestone 2)

- ☐ The repository is correctly tagged **`mentor_2`** and the tag has been pushed.
- ☐ The `README.md` contains the **video link**, verified as reachable from an outside account.
- ☐ The video is **between 5 and 7 minutes** long.
- ☐ The video **demonstrates the implemented functionalities**.
- ☐ The video **discusses the most relevant decisions**.
- ☐ The video **describes the elected verification strategy**.
- ☐ The video **presents the current status of the project**.

**Additional internal verification (not required by the statement, but necessary to avoid accumulating debt):**

- ☐ Search the whole codebase for `hilt`, `dagger`, `retrofit`, `gson`, `moshi`: **zero occurrences**.
- ☐ Inspect the payload actually sent: it contains `system_instruction` and the full `contents` array with alternating roles.
- ☐ Manual airplane-mode test: conversations remain readable and the unavailability message is clear.

---

## 10. Milestone 3 — Advanced features and robustness (16/11/2026)

> **Week 11 of the semester. 5 weeks, Sprints 4–8.**

### 10.1 Goal

Implement the requirements with the highest logical complexity — conversation deletion, **conversation rewrite**, active session restore at launch — and bring the optional image requirement to a demonstrable state. This is the milestone with the highest regression risk: every 🔴 task modifies invariants already covered by existing tests.

### 10.2 WBS — Conversation management

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M3-01 | Conversation deletion | Swipe gesture and/or context menu in the Chat History, confirmation dialog, deletion with `CASCADE` over the messages, snackbar with an *Undo* action (5 s window before the associated files are actually deleted). | M | 🟡 | 4 h | M2-12, M2-20 | ☐ |
| M3-02 | Conversation titling | Title generated automatically from the first characters of the first user message, with manual rename available. | S | 🟢 | 2 h | M3-01 | ☐ |
| M3-03 | **Conversation rewrite** | The user edits one of their own messages at **any position**. Atomic Room transaction: update the text, `deleteFromSeq(chatId, seq + 1)` which invalidates and removes all subsequent exchanges, rebuild the payload over the truncated history and resend it to the API. If the network call fails, the state prior to the resend must remain consistent. | M | 🔴 | 8 h | M2-15, M3-05 | ☐ |
| M3-04 | Rewrite UI | Long-press on a user bubble → context menu (*Edit*, *Copy*, *Delete*). In edit mode: dialog or pre-filled input bar with an explicit warning that subsequent messages will be deleted; confirmation required. | M | 🔴 | 5 h | M3-03 | ☐ |
| M3-05 | Deterministic ordering | Verify deterministic ordering already introduced in M2 schema version 1: monotonic per-chat sequences and consistent transactions during rewrite. **Do not rely on timestamps.** No migration to add `seq`, which already exists. | M | 🟡 | 3 h | M2-13 | ☐ |

### 10.3 WBS — State management and life cycle

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M3-06 | Active session restore | `activeChatId` saved in DataStore on entering the Active Chat and **cleared only** on explicit exit (back, closing the conversation). At launch, conditional routing: if present → Active Chat, otherwise → Title. Implements the statement's semantics ("*a user is considered previously engaged if they did not explicitly close or navigate away from the active conversation before the application was terminated*"). | M | 🔴 | 5 h | M2-14, M2-16 | ☐ |
| M3-07 | Surviving process death | `SavedStateHandle` for the unsent draft text and the scroll position. Verified with the *Don't keep activities* option and with `adb shell am kill`. | S | 🟡 | 4 h | M2-22 | ☐ |
| M3-08 | Retry policy | Exponential backoff with jitter for 429 and 503, reading the `Retry-After` header when present, at most 3 attempts, user-cancellable operation, no retry on 400/401/403. | M | 🔴 | 5 h | M2-09 | ☐ |
| M3-09 | Per-message status | Differentiated rendering for `SENDING` (progress indicator), `SENT`, `FAILED` (icon + *Retry* action on the bubble). Resending reuses the payload rebuilt from the current history. | M | 🟡 | 4 h | M3-08 | ☐ |
| M3-10 | Missing or invalid key | If the key is absent or the API returns 401/403: contextual message with a direct action leading to the Settings screen, no dead ends. | M | 🟡 | 3 h | M2-19, M2-09 | ☐ |

### 10.4 WBS — Optional requirement: images in the conversation

> **Optional but valued** requirement. Start it only if the milestone's *Must* tasks are well advanced by Sprint 6. Constraint V9 is absolute.

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M3-11 | Gallery selection | `ActivityResultContracts.PickVisualMedia` (Photo Picker): requires no runtime permission. Copy the selected file into `filesDir/images/` with a unique name. | C | 🟡 | 4 h | M2-21 | ☐ |
| M3-12 | Camera capture | `ActivityResultContracts.TakePicture` with a `FileProvider` configured in `file_paths.xml`; handling of the `CAMERA` permission with its rationale and of permanent denial. | C | 🔴 | 6 h | M3-11 | ☐ |
| M3-13 | Constraint-compliant persistence | An `imagePath` column (**relative** path, to survive sandbox changes) in `messages`. Test migration from the schema version actually shipped; no advance numbering based on adding `seq`. The `inline_data` Base64 encoding is produced **in memory** only for requests and is **never** written to the database (V9). | C | 🔴 | 5 h | M3-11, M2-08 | ☐ |
| M3-14 | Rendering in the conversation | Thumbnail inside the bubble with Coil, loading placeholder, handling of the *file no longer present* case, full-screen viewer. | C | 🟡 | 4 h | M3-13 | ☐ |
| M3-15 | File life cycle | Delete the file when the message or the conversation is deleted; delete on a rewrite that invalidates the message; orphan-cleanup routine at startup; resize/compress before sending to keep the payload small. | C | 🟡 | 4 h | M3-13, M3-01 | ☐ |

### 10.5 WBS — Quality and delivery

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| M3-16 | Accessibility and UI polish | `contentDescription` on every non-textual element, touch targets ≥ 48 dp, verified contrast, complete dark theme, correct behaviour on rotation. | S | 🟡 | 4 h | M2-21 | ☐ |
| M3-17 | Test suite extension | Tests for the rewrite (correct truncation, resulting payload), for retries, for Room migrations actually introduced after the M2 schema, Compose UI tests over the main flows. | M | 🟡 | 5 h | M3-03, M3-08 | ☐ |
| M3-18 | Milestone 3 video | **5–7 minute** recording: demonstration of the functionalities (with emphasis on rewrite, deletion, session restore, images if present), discussion of the relevant decisions, project status. | M | 🟡 | 4 h | all M3 | ☐ |
| M3-19 | `mentor_3` tag | Video link in the `README.md`, commit, annotated tag, push. | M | 🟢 | 0.5 h | M3-18 | ☐ |

**Estimated Milestone 3 total:** ~83 ideal hours, all solo — about 17 ideal hours a week over the 5 weeks (≈23 calendar hours a week at the 1.4× factor).

### 10.6 Acceptance checklist (Milestone 3)

- ☐ The repository is correctly tagged **`mentor_3`** and the tag has been pushed.
- ☐ The `README.md` contains the **video link**, reachable from outside.
- ☐ The video is **between 5 and 7 minutes** long.
- ☐ The video **demonstrates the implemented functionalities**.
- ☐ The video **discusses the most relevant decisions**.
- ☐ The video **presents the current status of the project**.

**Additional internal verification:**

- ☐ Rewriting a message in an **intermediate position** correctly deletes every subsequent exchange and generates new ones.
- ☐ Closing the app from inside a conversation and reopening it returns to that conversation; exiting first with *back* returns to the Title.
- ☐ Database inspection (App Inspection): the image column holds **paths**, not Base64 strings.

---

## 11. Final Milestone — Consolidation and delivery (12/12/2026)

> **~4 weeks, Sprints 9–12. Code freeze 08/12/2026.**

### 11.1 Goal

No new functionality after Sprint 9, except completing the optional requirement. The period is dedicated to integration, hardening, refining the user experience, systematic regression testing and producing the delivery materials. The statement requires an application that is *complete and stable, with all planned screens accessible* and with *all functionality working as specified in earlier milestones*: stability is therefore an explicit grading criterion, not a side concern.

### 11.2 WBS

| ID | Task | Operational description | Pri. | Diff. | Est. | Dep. | Status |
|---|---|---|---|---|---|---|---|
| MF-01 | Image requirement closure | Complete any M3-11…M3-15 tasks still open, with dedicated tests. If the decision is not to include the requirement, all dead code and UI references must be removed. | C | 🔴 | 8 h | M3-15 | ☐ |
| MF-02 | Error-handling hardening | Systematic review of every error path: missing key, revoked key, exhausted quota, response blocked by safety filters (`finishReason = SAFETY`), empty response, timeout, network loss mid-call. Every case produces an understandable message and a consistent state in Room. | M | 🟡 | 6 h | M3-08 | ☐ |
| MF-03 | UX review | Audit inspired by Steve Krug's *Don't Make Me Think* (the reference suggested by the statement): reduced cognitive load, unambiguous labels, no dead ends, main goals reachable in ≤ 3 taps from the Title. Usability test with two people who haven't worked on the project. | S | 🟡 | 5 h | — | ☐ |
| MF-04 | Adaptivity and orientation | Correct behaviour in portrait and landscape on every screen, keyboard handling, `WindowSizeClass` for tablets, no text truncation. | S | 🟡 | 5 h | MF-03 | ☐ |
| MF-05 | Performance | Stable keys in `LazyColumn`, recomposition audit with Layout Inspector, smoothness check on conversations with 300+ messages, pagination if needed. No I/O on the main thread (verified with StrictMode). | S | 🔴 | 5 h | — | ☐ |
| MF-06 | Security and release build | Ktor `Logging` plugin disabled in release; the key never logged in any environment; R8/ProGuard rules with verification that serialisation still works after obfuscation; key excluded from automatic backup (`dataExtractionRules`, `allowBackup`). | M | 🔴 | 5 h | — | ☐ |
| MF-07 | Full regression | Run the complete manual test matrix (Appendix E) on at least two API levels and on a physical device. Record the outcomes in a test-report table. | M | 🟡 | 6 h | MF-02 | ☐ |
| MF-08 | Final documentation | Complete `README.md`: description, screenshots, build instructions, procedure to obtain and configure your own BYOK key, architecture summary, library list, your identification, links to the four videos. KDoc on public components. | M | 🟡 | 4 h | — | ☐ |
| MF-09 | Release build | Signed APK generated and installed *from scratch* on a clean device, to validate the first launch with no key configured. Attached to the GitHub release. | S | 🟡 | 3 h | MF-06 | ☐ |
| MF-10 | Code freeze (08/12) | From this date only blocking bug fixes are accepted, each with a PR, a review and targeted regression testing. | M | 🟢 | — | — | ☐ |
| MF-11 | Final video | **5–7 minute** recording clearly showing the application in action: all screens, the complete conversational flow, rewrite, deletion, offline behaviour, error handling, images if implemented. | M | 🟡 | 5 h | MF-07 | ☐ |
| MF-12 | `mentor_f` delivery | Video link in the `README.md`, point-by-point verification of **every** acceptance criterion, final commit, annotated `mentor_f` tag, push. Visual confirmation on GitHub that the tag is on the remote. | M | 🟢 | 1 h | MF-11 | ☐ |

**Estimated Final Milestone total:** ~53 ideal hours, all solo — about 13–15 ideal hours a week over the ~4 weeks, plus the buffer of Sprint 12.

### 11.3 Acceptance checklist (Final Milestone)

- ☐ The app is **complete and stable**, with **all planned screens accessible** (Title, Chat History, Active Chat, About, Settings).
- ☐ **All functionality works as specified** in the earlier milestones and is integrated together.
- ☐ The **demo video clearly shows the application's features** and is **5–7 minutes** long.
- ☐ The repository is correctly tagged **`mentor_f`** and the tag has been pushed.
- ☐ The `README.md` contains the **link to the final video**.

**Final constraint verification (to be done before tagging):**

- ☐ V1 — no Hilt/Dagger dependency in any Gradle or source file.
- ☐ V2 — every HTTP call goes through Ktor; serialisation is entirely Kotlinx.
- ☐ V3 — all conversations live in Room.
- ☐ V4 — the API key lives exclusively in DataStore.
- ☐ V5 — inspect a real request: full history, alternating roles, no session ID.
- ☐ V6 — `system_instruction` present in every request.
- ☐ V7 — airplane-mode test passed.
- ☐ V8 — 429 and 500 simulated and handled without crashing.
- ☐ V9 — no Base64 in the database.
- ☐ V10 — four tags on the remote, complete README, professors with access.

---

## 12. Verification strategy

> To be formalised in `VERIFICATION.md` (task M2-25) and **presented in the Milestone 2 video**, where it is an explicit deliverable.

### 12.1 Test pyramid

| Level | Share | Subject | Tools |
|---|---|---|---|
| **Unit** | ~70 % | `HistoryPayloadBuilder`, error mapping, rewrite truncation logic, backoff, DTO serialisation | JUnit 4, `kotlinx-coroutines-test`, `kotlinx.serialization` |
| **Integration** | ~20 % | Ktor data source with `MockEngine`, DAOs with in-memory Room, repository with a fake data source, Room migrations | `ktor-client-mock`, `androidx.room:room-testing`, `androidx.test` |
| **UI / end-to-end** | ~10 % | Main flows: key configuration, sending a message, rewrite, deletion, offline behaviour | Compose UI Test, `createAndroidComposeRule` |

### 12.2 Priority test cases

**Stateless requirement (V5)**

1. After N completed exchanges, a request with the new user message contains `2N + 1` elements: alternating roles starting and ending with `user`; every completed exchange is included.
2. No field in the payload contains session or conversation identifiers.
3. `system_instruction` is present and populated in every request.
4. After an intermediate-position rewrite, the payload reflects the **truncated** history, not the original one.

**Network resilience (V8)**

| Simulated scenario | Expected behaviour |
|---|---|
| HTTP 429 with `Retry-After: 30` | Rate-limit message, deferred retry, no crash |
| HTTP 500 | Service error message, *Retry* action available |
| HTTP 401 | Explicit prompt to check the key in Settings |
| Timeout | Timeout message, message marked `FAILED` |
| Malformed JSON | `MalformedResponse` handled, no exception propagated |
| No network | Send blocked upstream, offline banner, history readable |

**Persistence (V3, V9)**

1. Deleting a chat cascades to all of its messages.
2. `seq` is strictly monotonic even under concurrent inserts.
3. Migrations after the M2 schema, when needed, preserve data (tested with `MigrationTestHelper`); `seq` already exists in version 1.
4. The image column holds paths; no Base64 value (automated assertion on length and format).

### 12.3 Manual execution matrix

| Configuration | Device | API | When |
|---|---|---|---|
| Development baseline | Pixel emulator | Latest stable | Every sprint |
| Minimum API | Emulator | 26 | End of M2, end of M3, MF-07 |
| Physical device | `[your model]` | `[level]` | End of every milestone |
| Tablet / landscape | Tablet emulator | Recent | MF-04 |

### 12.4 Exit criteria

- Automated suite fully green on `main`.
- Zero known crashes and zero open blockers.
- Every point of the manual matrix executed and recorded.
- Every constraint from section 3 explicitly verified.

---

## 13. Risk register

| ID | Risk | Prob. | Impact | Mitigation strategy |
|---|---|---|---|---|
| R1 | Exhausting the API free quota during development or, worse, while recording the video | High | Medium | Multiple keys (e.g. a second Google account) with rotation; use `MockEngine` in day-to-day development; **rehearse the demo with a fresh quota and record during low-traffic hours**; backup footage recorded in advance |
| R2 | The Gemini endpoint/model changes or is deprecated during the semester | Medium | High | Model name and API version in **a single constant**; tolerant DTOs (`ignoreUnknownKeys = true`); monthly check of the official documentation. **Stay on `generateContent`: the statement explicitly forbids server-side stateful variants** |
| R3 | Underestimating the conversation rewrite (M3-03) | High | High | Prototype the truncation logic alone in unit tests **before** touching the UI; tests written before the implementation (§2); one sprint of buffer |
| R4 | Overlap with exams and other projects | High | High | Estimates in ideal hours with a 1.4× factor; code freeze brought forward to 08/12; *Could* tasks are the first to be sacrificed if late |
| R5 | Regressions introduced by the 🔴 tasks of Milestone 3 | Medium | High | Green automated suite as a merge precondition; mandatory CI; no direct merges into `main` |
| R6 | Forgetting, weeks later, why a past decision was made — with no teammate to ask | Medium | Medium | Conventional Commits with the *why* in the body, not just the *what*; keep `AGENTS.md` and this plan's notes updated as decisions are made, not after |
| R7 | Accidentally committing the API key | Low | Very high | Complete `.gitignore`, `git-secrets` or a local pre-commit scan, checklist in the PR template, immediate key revocation if exposed |
| R8 | Video out of the required duration or missing required points | Medium | Medium | Timed script (Appendix D), at least one full dry run with a stopwatch before the real recording |
| R9 | Illness or unavailability close to a deadline, with no teammate to absorb the load | High | Very high | No backup is possible solo — the only real mitigation is the slack already built into the timeline (the 08/12 code freeze leaves four days before `mentor_f`). Treat that slack as reserved for this risk, not as extra feature time, and apply the same logic before each earlier tag |
| R10 | Professor access to the repository not correctly configured | Low | Very high | Explicit check at the end of **every** milestone, included in the acceptance checklists |

---

## 14. Requirements → tasks traceability matrix

Every requirement from the statement is mapped to the tasks that implement it and to the milestone where it is completed. Use this table as a self-assessment tool before every delivery.

| Requirement from the statement | Tasks | Completion milestone |
|---|---|---|
| BYOK LLM client using Gemini's REST API (`generateContent`) | M2-05, M2-06, M2-07 | M2 |
| `Application` class as service locator; Hilt/Dagger forbidden | M2-01, M2-02 | M2 |
| Networking via Ktor Client and Kotlinx Serialization | M2-05, M2-06 | M2 |
| Chat history in Room | M2-11, M2-12, M2-13 | M2 |
| API key in DataStore | M2-14, M2-19 | M2 |
| Stateless variant; no server-side session ID | M2-07, M2-08, M2-15 | M2 |
| Payload with the whole history, alternating `user`/`model` roles | M2-08, M2-26 | M2 |
| Mentor persona via `system_instruction` | M2-10 | M2 |
| Offline-first experience; conversations accessible without network | M2-15, M2-24 | M2 |
| Network availability verified before every call | M2-23, M2-24 | M2 |
| Graceful HTTP error handling (429, 500, …) | M2-09, M2-15, M2-21, M2-27; later extensions M3-08, MF-02 | M2 for typed handling and manual recovery |
| Title screen | M2-17 | M2 |
| Chat History screen | M2-20 | M2 |
| Active Chat screen | M2-21 | M2 |
| About screen | M2-18 | M2 |
| Settings screen | M2-19 | M2 |
| Navigation matching Figure 1 | M2-16 | M2 |
| Automatic return to the last active conversation at launch | M3-06 | M3 |
| Removal of previous conversations | M3-01 | M3 |
| Conversation rewrite, at any position, invalidating subsequent exchanges | M3-03, M3-04, M3-05 | M3 |
| *(Optional)* Images from gallery or camera | M3-11, M3-12 | M3 |
| *(Optional)* Only URI/path in Room, Base64 forbidden | M3-13, M3-15 | M3 |
| `README.md` identifying you | M1-02 | M1 |
| Full access granted to the professors | M1-01 | M1 |
| `mentor_1` tag with plan and timeline | M1-03, M1-09 | M1 |
| `mentor_2` tag with a 5–7 min video (functionality, decisions, verification, status) | M2-30, M2-31 | M2 |
| `mentor_3` tag with a 5–7 min video (functionality, decisions, status) | M3-18, M3-19 | M3 |
| `mentor_f` tag with the complete application and final video | MF-11, MF-12 | Final |

---

## 15. Appendices

### Appendix A — Stateless payload structure

Request to `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`, header `x-goog-api-key: <BYOK key>`.

```json
{
  "system_instruction": {
    "parts": [
      { "text": "You are DroidMentor, a senior Android engineering mentor. …" }
    ]
  },
  "contents": [
    { "role": "user",  "parts": [{ "text": "How do I handle state in Compose?" }] },
    { "role": "model", "parts": [{ "text": "Expose a StateFlow from the ViewModel …" }] },
    { "role": "user",  "parts": [{ "text": "And how do I survive process death?" }] }
  ],
  "generationConfig": { "temperature": 0.7 }
}
```

With an image attachment (optional requirement), the Base64 encoding is built **in memory** from the file on disk and is never persisted:

```json
{
  "role": "user",
  "parts": [
    { "text": "What's wrong with this layout?" },
    { "inline_data": { "mime_type": "image/jpeg", "data": "<base64 generated at runtime>" } }
  ]
}
```

**Invariants that must always hold:**

1. The `contents` array carries **the entire** relevant conversation history, not just the last turn.
2. Roles alternate strictly `user` → `model` → `user`, and the first element has role `user`.
3. No field carries server-side session or conversation identifiers.
4. `system_instruction` is repeated in **every** request, because the call is by definition stateless.

The useful response is at `candidates[0].content.parts[0].text`; the cases where `candidates` is empty or `finishReason` signals a block must also be handled.

> **Note.** Google has also introduced conversational interfaces with server-side state. **They must not be used:** the statement explicitly forbids server-side multi-turn sessions and requires the client to manage all the context.

### Appendix B — M2 Room schema (version 1)

```
chats
├── id           TEXT     PK
├── title        TEXT     NOT NULL
├── createdAt    INTEGER  NOT NULL
└── updatedAt    INTEGER  NOT NULL

messages
├── id           TEXT     PK
├── chatId       TEXT     NOT NULL  FK → chats.id  ON DELETE CASCADE   [INDEX]
├── seq          INTEGER  NOT NULL   -- deterministic per-chat ordering
├── role         TEXT     NOT NULL   -- USER | MODEL
├── text         TEXT     NOT NULL
├── status       TEXT     NOT NULL   -- SENDING | SENT | FAILED
├── errorCategory TEXT    NULL       -- stable typed error category
└── createdAt    INTEGER  NOT NULL
```

| Version | Content | Introduced in |
|---|---|---|
| 1 | `chats` + `messages`, with `seq`, unique `(chatId, seq)` index and nullable `errorCategory`; no `imagePath` | M2-11, M2-13 |

Later versions will be defined by actual schema changes; `seq` needs no future migration.

Every migration must come with a `MigrationTestHelper` test and with an update of the exported schema in `app/schemas`.

### Appendix C — Root `README.md` structure

```markdown
# DroidMentor

BYOK Android LLM client, developed for the Mobile Devices Programming course
(ISEL, Winter Semester 2026/2027).

## Author

| Name | Student number | Email | GitHub |
|---|---|---|---|
| Luca Raona | 55603 | [email] | [@handle] |

Solo project, developed under the name **PastaGPT**.

## Deliveries

| Milestone | Tag | Date | Video |
|---|---|---|---|
| 1 — Project plan | `mentor_1` | 21/09/2026 | — (see PROJECT_PLAN.md) |
| 2 | `mentor_2` | 12/10/2026 | [link] |
| 3 | `mentor_3` | 16/11/2026 | [link] |
| Final | `mentor_f` | 12/12/2026 | [link] |

## Documentation
- [Project plan](PROJECT_PLAN.md)
- [Verification strategy](VERIFICATION.md)
- [Contribution guidelines](CONTRIBUTING.md)

## Build and configuration
1. Requirements (Android Studio, JDK, minSdk 26)
2. How to obtain a Gemini key from Google AI Studio
3. Entering the key in the app's Settings screen
   (the key must NOT be placed in the source code or in configuration files)
```

### Appendix D — Timed video script (5–7 minutes)

| Time | Section | Content | Present in |
|---|---|---|---|
| 0:00–0:30 | Opening | Your name, project name (PastaGPT), purpose of the video | M2, M3, Final |
| 0:30–3:00 | Demonstration | Real use of the app on a device: complete flow, features new since the previous milestone, offline and error behaviour | M2, M3, Final |
| 3:00–4:30 | Relevant decisions | Manual service locator instead of Hilt, stateless history management, Room as the source of truth, HTTP error handling | M2, M3 |
| 4:30–5:30 | Verification strategy | Test pyramid, `MockEngine`, in-memory Room, CI | **M2 (mandatory)** |
| 5:30–6:30 | Project status | Completed and in-progress tasks, open risks and the plan for the next milestone | M2, M3 |
| ~6:30–7:00 | Closing | Summary and reference to the repository | All |

**Operating rules:** record the device screen with voice-over commentary; do a dry run to check the duration; API key configured and quota available **before** hitting *record*; no sensitive data visible on screen; upload to a platform with a stable link and verify accessibility from a private browsing window.

### Appendix E — Manual test matrix (final regression, MF-07)

| # | Scenario | Expected outcome | Result |
|---|---|---|---|
| 1 | First launch with no key configured | No crash; explicit prompt to configure the key in Settings | ☐ |
| 2 | Configure, save and read the key back after a restart | The key persists; it is masked on screen | ☐ |
| 3 | New conversation and sending the first message | The mentor's reply is received and persisted | ☐ |
| 4 | Multi-turn conversation (≥ 5 exchanges) | Context is maintained: the model remembers the previous turns | ☐ |
| 5 | Close and reopen the app from inside a conversation | Automatic return to the correct Active Chat | ☐ |
| 6 | Explicit exit with *back*, then reopen | Launch from the Title Screen | ☐ |
| 7 | Airplane mode: reading conversations | All readable; no error on screen | ☐ |
| 8 | Airplane mode: attempting to send | Sending blocked with a clear message; no crash | ☐ |
| 9 | Invalid key | Understandable message with a shortcut to Settings | ☐ |
| 10 | Quota exceeded (429) | Rate-limit message; deferred retry | ☐ |
| 11 | Deleting a conversation | Removed from the list; messages cascade-deleted; associated files deleted | ☐ |
| 12 | Rewriting the **last** message | Reply correctly regenerated | ☐ |
| 13 | Rewriting an **intermediate** message | All subsequent exchanges deleted and replaced | ☐ |
| 14 | Screen rotation on every screen | No loss of state or of the input draft | ☐ |
| 15 | Process death (*Don't keep activities*) | State and draft restored | ☐ |
| 16 | Sending an image from the gallery *(optional)* | Image shown in the bubble; **path** saved in Room | ☐ |
| 17 | Capturing and sending from the camera *(optional)* | As above; permission handled correctly, including on denial | ☐ |
| 18 | Database inspection after sending images | No Base64 string present | ☐ |
| 19 | Full navigation across all five screens | All paths and returns working, consistent back stack | ☐ |
| 20 | Long conversation (300+ messages) | Smooth scrolling; no ANR | ☐ |

---

*Document written for Milestone 1 of the Mobile Devices Programming practical assignment — ISEL, Winter Semester 2026/2027. Luca Raona — PastaGPT.*
