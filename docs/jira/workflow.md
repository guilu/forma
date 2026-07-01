# Jira workflow for Forma

## Goal

Jira is the operational backlog for Forma. GitHub is the source code repository. The markdown files under `docs/jira/` are a versioned specification of the intended Jira structure so agents can work even when Jira is unavailable.

## Jira instance

```txt
https://dbhlab.atlassian.net
```

Project key:

```txt
FOR
```

Cloud ID:

```txt
22e5f20e-fb6f-4efd-96e3-f0586f17bc0
```

## Source of truth

Priority order:

1. Jira issues and their current workflow status.
2. GitHub pull requests and commits.
3. `docs/jira/` markdown files as versioned planning backup.
4. Product docs under `docs/` for context.

## Agent workflow

```txt
Jira issue
  -> read linked docs
  -> create implementation plan
  -> create branch
  -> implement
  -> run tests
  -> open pull request
  -> reference Jira issue key in PR title/body
  -> move Jira issue to review
```

## Branch naming

```txt
feature/FOR-001-bootstrap-monorepo
fix/FOR-123-short-description
docs/FOR-456-update-architecture
```

## Commit style

Use conventional commits and include the Jira key when possible.

```txt
feat(FOR-001): bootstrap monorepo
fix(FOR-023): correct macro calculation
docs(FOR-010): clarify body measurement model
```

## Pull request title

```txt
FOR-001 Bootstrap monorepo
```

## Definition of Ready

A Jira issue is ready for an agent when it includes:

- clear goal
- business value
- technical notes
- acceptance criteria
- definition of done
- links to relevant docs
- label `codex-ready` or equivalent

## Definition of Done

An issue is done when:

- implementation is merged into main
- tests pass locally and in CI
- documentation is updated if needed
- Jira issue is moved to Done
- PR references the Jira issue

## Labels

Suggested labels:

```txt
mvp
codex-ready
backend
frontend
infra
body
training
nutrition
shopping
insights
integrations
ui
```

## Issue template

```md
## Goal

## Business value

## Technical notes

## Acceptance criteria

- [ ] ...

## Definition of Done

- [ ] Tests pass
- [ ] Documentation updated when needed
- [ ] PR references this issue
- [ ] CI green
```
