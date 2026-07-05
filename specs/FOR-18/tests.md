# FOR-18 Test Plan

## Scope

Verify the body measurement form: rendering, client-side validation, submit
behavior against the FOR-17 API, and success/error states.

## Domain Tests

N/A — no domain logic in the frontend (ADR-006).

## Application Tests

N/A — no frontend application/state-management layer beyond the page
component and API client are expected for this story.

## API Tests

N/A — backend API tests are covered by `specs/FOR-17/tests.md`. This story
tests the frontend's use of that API (mocked), not the API itself.

## UI Tests

- Form renders all required fields (date+time, weight, body fat %, BMI,
  optional notes).
- Submitting with a required field empty shows inline validation errors and
  does not call the API.
- Submitting a valid form calls `apiClient.request` with the expected
  payload shape (matching `specs/FOR-17/api.md`).
- A successful response shows a success state and triggers the expected
  list/dashboard refresh.
- An API error response shows the error message without a raw stack
  trace/response body.

## Edge Cases

- Very long `notes` text does not break layout.
- Rapid double-submit does not create duplicate requests (decide/document
  the chosen guard, e.g. disable button while pending).

## Fixtures

- A valid form payload matching `specs/FOR-17/api.md`'s request example.
- A mocked API success response and a mocked API error response
  (`VALIDATION_ERROR` shape) for testing both states without a live backend.
