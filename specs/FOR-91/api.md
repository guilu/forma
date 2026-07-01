# API Notes: FOR-91

## Correlation ID Behavior

Requests should receive or propagate a correlation ID. The exact header can be selected during implementation, but it must be documented and used consistently.

Suggested header:

- `X-Correlation-Id`

## Response Behavior

- Error responses should include the correlation ID when the API error baseline supports it.
- Successful responses do not need to include correlation ID in the body, but the header may be returned if useful.

## Logging Behavior

- Include correlation ID in request logs.
- Include correlation ID in error logs.
- Do not log secrets, tokens or full personal health payloads.

## Constraints

- Do not add product endpoints.
- Do not create a full tracing system.
- Do not expose internal stack traces.
