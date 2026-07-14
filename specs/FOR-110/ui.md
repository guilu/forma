# FOR-110 UI Spec

No UI — backend/read-model story.

## API surface

- `GET /api/v1/insights/history` (naming indicative) — past weeks'
  persisted insights, most recent first.
- `GET` current-week insights (FOR-45/FOR-56, unchanged route) — its
  `checkIn` signals gain week-over-week delta fields.

FOR-124 consumes these endpoints for the insights history view and WoW
delta display; it owns its own `ui.md`.
