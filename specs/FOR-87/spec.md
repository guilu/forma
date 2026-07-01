# FOR-87: Establish frontend testing baseline

Jira: https://dbhlab.atlassian.net/browse/FOR-87
Story points: 3
Epic: FOR-79 Project Bootstrap

## Goal

Establish the frontend testing baseline for FORMA.

## Business Value

Creates a predictable testing foundation for future UI stories and prevents UI regressions from agent-generated changes.

## Scope

Create the first frontend testing setup.

The baseline should include:

- Component/rendering test setup.
- Basic interaction test support where practical.
- Type checking or equivalent static validation.
- Test naming conventions.
- Example tests for skeleton behavior.
- CI execution support.

## Architecture Notes

- Align with ADR-006 and ADR-007.
- Keep the first setup lightweight.
- Prioritize loading, empty, error and interaction states in later stories.
- Do not test domain calculations in frontend tests because the frontend must not own them.

## Acceptance Criteria

- Frontend tests or type checks can run from one documented command.
- At least one skeleton UI test or equivalent check exists.
- Test conventions are documented.
- CI can execute frontend validation.
- Testing setup does not require product features to exist.

## Out of Scope

- Full E2E suite.
- Visual regression suite.
- Product screen tests before product screens exist.

## Definition of Done

- Frontend testing baseline committed through PR.
- Tests/checks verified locally.
- CI integration verified or prepared.
- Documentation updated.
