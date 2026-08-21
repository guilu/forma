# Branching Strategy

## Default branch

`main` is the stable integration branch.

## Feature branches

Use one branch per story or coherent documentation change.

Format: `<type>/<kebab-description>`. The Jira key does not go in the branch name.

```text
feat/entrenamiento-sin-scroll
fix/training-week-scoped-sessions
refactor/design-system-buttons
style/quitar-enlace-estadisticas
chore/dev-api-fixtures
docs/agents-md-al-dia
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `style`, `security`.

## Pull requests

Pull requests should be small and reviewable.

PR titles use gitmoji + Conventional Commits, with the description in Spanish — the same format as the commits themselves:

```text
✨ feat(training): la semana pasa a ser la página, y cabe entera sin scroll
🐛 fix(compra): la lista de compra se puede crear, y se ve
```

## PR description checklist

There is no PR template file; the sections in use are `## What changed`, `## How it was tested` and `## Known limitations`.

Include:

- Summary of changes, and why.
- How it was tested.
- Screenshots for UI changes when useful.
- Known limitations or follow-ups.
- What was deliberately left out of scope, so a reviewer need not guess whether an omission was a decision.
- Jira issue link/key when the work came from a story.

## Merge policy

- Do not merge failing CI unless the failure is unrelated and explicitly documented.
- Prefer squash merge for story branches unless history preservation is useful.
- Keep main deployable.

## AI agent branches

AI agents should never work directly on `main`.

Agents create a branch in the format above and keep changes scoped to the work they were asked to do. See `AGENTS.md` for the full agent workflow.
