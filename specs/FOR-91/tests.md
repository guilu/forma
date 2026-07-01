# Tests: FOR-91

## Verification Goal

Prove request correlation and logging baseline work without leaking sensitive information.

## Required Checks

- Send request without correlation ID and verify one is generated.
- Send request with correlation ID and verify it is propagated where supported.
- Verify logs include correlation ID.
- Verify secrets/tokens are not logged.
- Verify local logging output is readable.

## Suggested Test Coverage

- Correlation ID filter/interceptor test.
- Request logging smoke test.
- Error logging safety review or test.

## Non-Goals

- Full tracing backend tests.
- Metrics dashboard tests.
- Audit event tests unless a separate story introduces audit events.
