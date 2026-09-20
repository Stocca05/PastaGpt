# Contributing to DroidMentor (PastaGPT)

## Branching Strategy
We use a Trunk-Based Development approach:
- `main` is the primary branch and must always be stable.
- Feature branches are short-lived.
- Branch naming convention:
  - `feat/<id-task>-<slug>` for features (e.g., `feat/m2-08-history-payload`)
  - `fix/<slug>` for bugfixes
  - `docs/<slug>` for documentation updates
  - `chore/<slug>` for maintenance tasks

## Commit Convention
We follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` for new features
- `fix:` for bug fixes
- `refactor:` for code refactoring
- `test:` for adding or updating tests
- `docs:` for documentation updates
- `chore:` for tooling or dependencies

*Example:* `feat: add room database entities for chats`

## Review Policy
- Every change must go through a Pull Request against `main`.
- Pull Requests must pass all CI checks (linting, tests) before merging.
- Review the diff carefully before merging to catch logical errors.
- Adhere strictly to the Non-Negotiable Constraints (Section 3 of PROJECT_PLAN.md).
