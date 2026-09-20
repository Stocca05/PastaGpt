## Description
<!-- Briefly describe the changes introduced by this PR. Include the task ID. -->

## Constraints Checklist (Section 3)
Before merging, please verify that this PR does not violate any project constraints:
- [ ] **V1**: Dependency injection is MANUAL. No Hilt/Dagger annotations or plugins.
- [ ] **V2**: Networking is Ktor Client ONLY (no Retrofit, Gson, etc.).
- [ ] **V3**: Chat history persistence is Room ONLY.
- [ ] **V4**: Gemini API key is stored in Preferences DataStore ONLY (never in code/repo).
- [ ] **V5**: API usage is strictly STATELESS (no server-side sessions).
- [ ] **V6**: Mentor persona is set through `system_instruction`.
- [ ] **V7**: Offline-first experience (Room is single source of truth).
- [ ] **V8**: Graceful HTTP error handling mapped to typed results.
- [ ] **V9**: If images are supported, ONLY file path/URI goes into Room (no Base64 in DB).

## Testing
- [ ] Tests have been added/updated for the changed logic.
- [ ] All tests pass locally.
