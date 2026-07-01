# FOR-84: Configure CI quality gate

Jira: https://dbhlab.atlassian.net/browse/FOR-84
Story points: 5
Epic: FOR-79 Project Bootstrap

## Goal

Configure the initial CI quality gate for FORMA.

## Business Value

Prevents broken baseline changes from reaching main and gives agents a clear verification target.

## Scope

Create the first CI workflow.

The workflow should run:

- Backend build.
- Backend tests.
- Frontend build.
- Frontend tests or type checks.
- Lint/format checks where configured.

The pipeline should run on pull requests and updates to main.

## Architecture Notes

- Align with ADR-007 and FOR-71 if present.
- Keep the first pipeline simple and fast.
- Cache dependencies where practical.
- Documentation-only changes may still run basic checks unless optimized later.

## Acceptance Criteria

- CI runs on pull requests.
- CI runs on main updates.
- Backend failures fail the pipeline.
- Frontend failures fail the pipeline.
- CI result is visible in GitHub PRs.

## Out of Scope

- Production deployment.
- Release automation.
- Full security scanning unless already trivial to enable.

## Definition of Done

- CI workflow committed through PR.
- At least one successful CI run verified.
- Failure mode tested or reviewed.
- Documentation updated if commands are introduced.
