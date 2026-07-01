# Tests: FOR-90

## Verification Goal

Prove environment configuration works and secrets are not committed.

## Required Checks

- Verify example env files contain fake values only.
- Verify real local env files are ignored.
- Verify backend loads configuration from environment.
- Verify missing critical configuration fails clearly where applicable.
- Review frontend variables to ensure no backend-only secret is exposed.

## Suggested Test Coverage

- Configuration loading test.
- Missing required variable test.
- Secret file ignore check.

## Non-Goals

- Production secret manager tests.
- Provider OAuth integration tests.
- Rotation policy automation.
