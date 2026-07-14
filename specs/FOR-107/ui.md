# FOR-107 UI Spec

No UI — backend/read-model story.

## API surface

- `GET /api/v1/profile` — profile + unit preferences + default objectives +
  theme preference + `firstRunCompleted`, with sensible defaults before any
  data has been saved.
- Update endpoints for profile fields, unit preferences, default objectives,
  theme preference and onboarding answers (exact routes/verbs decided during
  implementation; see `spec.md` Open Questions and `ai-context.md`).

Downstream UI stories (FOR-119 profile/units, FOR-120 theme persistence,
FOR-121 onboarding persistence) consume this API; they own their own
`ui.md`.
