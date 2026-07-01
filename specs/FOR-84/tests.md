# Tests: FOR-84

## Verification Goal

Prove CI runs the expected quality gate and fails when a required check fails.

## Required Checks

- Open or update a PR and confirm CI starts.
- Confirm backend build/test commands run.
- Confirm frontend build/test or type-check commands run.
- Confirm lint/format checks run if configured.
- Review at least one likely failure mode.

## Suggested Test Coverage

- Workflow syntax validation.
- Successful CI run on the branch.
- Visible PR status check.

## Non-Goals

- Deployment validation.
- End-to-end environment provisioning.
- Long-running performance checks.
