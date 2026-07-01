# FOR-86: Establish backend testing baseline

Jira: https://dbhlab.atlassian.net/browse/FOR-86
Story points: 3
Epic: FOR-79 Project Bootstrap

## Goal

Establish the backend testing baseline for FORMA.

## Business Value

Creates a predictable testing foundation for future domain, application, adapter and API stories.

## Scope

Create the first backend testing setup.

The baseline should include:

- Unit test setup.
- Application/use case test setup.
- API test setup placeholder if appropriate.
- Database integration test approach if practical.
- Test naming conventions.
- Example tests for skeleton behavior.

## Architecture Notes

- Align with ADR-007.
- Keep tests fast by default.
- Do not overbuild a full testing framework before product code exists.
- Prefer clear examples for future agent-generated tests.

## Acceptance Criteria

- Backend tests can run from one documented command.
- At least one unit test exists.
- At least one application or API-level skeleton test exists where practical.
- Test conventions are documented.
- CI can execute the backend tests.

## Out of Scope

- Product domain tests before product domain behavior exists.
- External provider contract tests.
- End-to-end tests.

## Definition of Done

- Backend testing baseline committed through PR.
- Tests verified locally.
- CI integration verified or prepared.
- Documentation updated.
