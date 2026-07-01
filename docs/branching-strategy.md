# Branching Strategy

## Default branch

`main` is the stable integration branch.

## Feature branches

Use one branch per story or coherent documentation change.

Recommended format:

```text
feature/FOR-123-short-description
fix/FOR-123-short-description
docs/short-description
```

## Pull requests

Pull requests should be small and reviewable.

PR titles for story work should start with the Jira key:

```text
FOR-123 Implement manual body measurement entry
```

## PR description checklist

Include:

- Jira issue link/key.
- Summary of changes.
- How it was tested.
- Screenshots for UI changes when useful.
- Known limitations or follow-ups.

## Merge policy

- Do not merge failing CI unless the failure is unrelated and explicitly documented.
- Prefer squash merge for story branches unless history preservation is useful.
- Keep main deployable.

## AI agent branches

AI agents should never work directly on `main`.

Agents should create branches named after the target Jira story and keep changes scoped to that story.
