# FOR-57 Test Plan

## Scope

Verify the integrations screen lists connected/available providers, shows status
+ last sync, offers connect/disconnect/sync entry points, and never leaks
sensitive data.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A — consumes integrations endpoints (mocked; backend is future).

## UI Tests

- Available providers render with a connect action.
- Connected providers render with status + last-sync timestamp.
- Connect/disconnect and manual-sync entry points are present where supported.
- A sync error renders clearly with no token/PII.
- Empty connected state renders cleanly.

## Edge Cases

- No connected providers → available-only view.
- Sync error → visible, no sensitive data.
- Backend unavailable → entry points shown as non-functional/mocked.

## Fixtures

- Mocked provider states: Withings connected (with last sync), Google Fit / Apple
  Health available; a sync-error state.
