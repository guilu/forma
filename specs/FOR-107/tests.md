# FOR-107 Test Plan

## Scope

Verify the profile/preferences/onboarding aggregate persists correctly,
returns sane defaults on first run, validates input, and does not affect
existing shopping/training/nutrition/body endpoints.

## Domain Tests

- Profile/preferences aggregate rejects an invalid theme mode value.
- Profile/preferences aggregate rejects an invalid unit value.
- Default construction (no prior data) yields dark theme + metric units +
  `firstRunCompleted = false`.

## Application Tests

- Update use case persists a single changed field without clobbering the
  rest of the preferences record.
- Onboarding-answers use case upserts progress across repeated calls
  (in-progress → completed).
- Read use case returns defaults when no row exists yet, not an error.

## API Tests

- `GET /api/v1/profile` before any write → 200 with default payload.
- `PATCH` profile fields → subsequent `GET` reflects the change.
- `PATCH` theme preference → subsequent `GET` reflects the change; value
  vocabulary matches frontend `ThemeMode` (`light | dark | system`).
- `PATCH` with an invalid enum value → 400 `VALIDATION_ERROR`.
- Onboarding submit → `GET` reflects `firstRunCompleted: true` and the
  persisted answers.
- Existing `GET /api/v1/shopping/list`, training, nutrition and body
  endpoints are unaffected (regression check).

## UI Tests

N/A — backend story.

## Edge Cases

- Repeated `PATCH` calls in quick succession (single-user MVP, no
  concurrency control expected) → last write wins, no corruption.
- Missing optional fields in a partial update payload → left unchanged, not
  nulled out.

## Fixtures

- A clean-database integration test asserting the default profile payload
  before any write occurs.
- A seeded profile row for update/read round-trip tests.
