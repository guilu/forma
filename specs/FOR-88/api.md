# API Notes: FOR-88

## API Base

Define or document the API base convention, preferably versioned, for example `/api/v1` if aligned with the selected stack.

## Error Response Baseline

A standard error response should include enough information to debug safely without leaking internals. Suggested fields:

- `code`: stable machine-readable error code.
- `message`: safe human-readable message.
- `correlationId`: request correlation identifier when available.
- `details`: optional safe validation details.

The final field names may differ, but the response must be consistent and documented.

## Validation Errors

Validation errors should identify fields and messages without exposing implementation internals.

## Security Constraints

- Never expose stack traces.
- Never expose secrets, tokens or provider payloads.
- Do not reveal whether another user's private resource exists.

## Product Endpoint Constraint

This story must not create body composition, training, nutrition, shopping, insights or integration endpoints.
