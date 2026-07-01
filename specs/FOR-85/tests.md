# Tests: FOR-85

## Verification Goal

Prove formatting and linting commands exist and behave predictably.

## Required Checks

- Run backend formatting/linting check.
- Run frontend formatting/linting check.
- Run auto-format commands where provided.
- Verify CI can call the relevant checks where practical.
- Confirm no real product code was changed only to satisfy unrelated formatting churn.

## Suggested Test Coverage

- Command succeeds on the baseline repository.
- Intentional style violation fails locally or in CI where practical.
- Documentation includes check and fix commands.

## Non-Goals

- Full static analysis program.
- Security scanning.
- Custom architectural lint rules.
