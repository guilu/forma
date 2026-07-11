# FOR-51 Test Plan

## Scope

Verify the dashboard composes its widgets, renders loading/empty/error/success
states, and links each widget to its feature page.

## Domain Tests

N/A.

## Application Tests

N/A — read models come from feature APIs (mocked in UI tests).

## API Tests

N/A — no new endpoint (consumes existing ones).

## UI Tests

- Dashboard renders all MVP summary widgets (body, training, nutrition, shopping,
  insight, sync).
- Each widget shows a loading state while its data resolves.
- Empty data (new user) → each widget shows its empty state, no error.
- A failing widget shows its error state without breaking siblings.
- The insight widget renders the FOR-45 main recommendation (message + reason).
- Each widget links to its feature route.

## Edge Cases

- All-empty new-user dashboard renders cleanly.
- Missing insight → insufficient-data recommendation shown.

## Fixtures

- Mocked API responses per widget: a populated week and an empty week; a failing
  fetch for one widget.
