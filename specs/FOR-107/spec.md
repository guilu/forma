# FOR-107: [STUB] User profile & preferences backend

Jira: https://dbhlab.atlassian.net/browse/FOR-107
Epic: FOR-96

## Summary

Persist and expose over HTTP a single-user profile + preferences aggregate:
profile fields (name, email, birthdate, sex, height, activity level, main
goal — per FOR-58's Ajustes mockup), unit preferences (peso/altura/
distancia/energía — kg/cm/km/kcal), default objectives (déficit calórico,
proteínas, agua diaria), theme preference (light/dark/system, mirrors
`ThemeMode` in `frontend/src/theme/theme.ts`) and onboarding answers plus a
first-run completion flag (mirrors `OnboardingAnswers`/`OnboardingProgress`
in `frontend/src/pages/onboarding/onboardingStorage.ts`). This is a
consolidated backend prerequisite: it closes the "no profile backend yet"
gap documented in FOR-58's Data Model Notes and unblocks FOR-119
(profile/units edit), FOR-120 (server-side theme) and FOR-121 (onboarding
persistence). Marked `[STUB]` because it establishes the aggregate and API
shape; deeper preference domains (training/nutrition detail) can extend it
without a rewrite.

## User/System Flow

1. Client calls `GET /api/v1/profile` on Settings or app load.
2. If no profile exists yet (first run), the backend returns defaults with
   `firstRunCompleted: false` rather than a 404.
3. Client calls scoped update endpoints to change profile fields, unit
   preferences, default objectives, theme, or to submit onboarding answers.
4. Backend persists through a new hexagonal slice (domain aggregate +
   application port + `delivery/profile` controller + persistence adapter).

## Functional Requirements

- **Profile fields**: name, email, birthDate, sex, heightCm, activityLevel,
  mainGoal — read + update.
- **Unit preferences**: weight/height/distance/energy units — read + update;
  MVP ships the metric set already implied by FOR-58's mockup (kg/cm/km/
  kcal) as the default and only supported value, but the field is a real
  preference (not hardcoded) so FOR-119 can add a selector later.
- **Default objectives**: caloric deficit, protein target, daily water —
  read + update.
- **Theme preference**: `light | dark | system` — read + update; same
  vocabulary as the frontend's `ThemeMode` so FOR-120 can map it directly.
- **Onboarding**: persist per-step answers (profile, metrics choice, goal,
  training days, equipment, nutrition preference/restrictions — same shape
  as `OnboardingAnswers`) plus a `firstRunCompleted` flag, so FOR-121 can
  gate onboarding from a backend flag instead of `localStorage`.
- Hexagonal: new domain aggregate(s) behind an application port; delivery
  layer under `delivery/profile` (sibling of the existing `delivery/
  shopping`, `delivery/training`, `delivery/body`, `delivery/nutrition`
  packages); persistence adapter + migration per ADR-003.
- Additive only — does not change existing shopping/training/nutrition/body
  endpoints or contracts.

## Non-Functional Requirements

- Single-user MVP (ADR-002): no auth enforcement is added by this story, but
  the aggregate must be account-scoped in shape (an owner identifier field)
  so authorization can be layered on later without a data-model rewrite.
- Migration-driven schema (ADR-003). Watch the known H2-vs-PostgreSQL
  multi-column `ALTER TABLE ADD COLUMN` gotcha documented in FOR-100/
  FOR-106 (one column per statement).
- Versioned REST, consistent `ApiError` shape, input validation at the
  boundary (ADR-005).

## Data Model Notes

No profile/preferences/onboarding backend exists yet — verified: no
`delivery/profile` (or similarly named) package under
`backend/src/main/java/dev/diegobarrioh/forma/delivery`, which currently
only has `insights`, `shopping`, `web`, `training`, `body`, `nutrition` and
`error`. Likely new persistence: a profile table, a preferences table (unit
prefs + default objectives + theme) and an onboarding-progress table — or a
single combined table if the implementer judges the split unnecessary for
MVP scale; document the final shape in code comments. Frontend shapes to
mirror on the way in/out: `OnboardingAnswers` / `OnboardingProgress`
(`frontend/src/pages/onboarding/onboardingStorage.ts`), `ThemeMode`
(`frontend/src/theme/theme.ts`), and the profile/units/objectives fields
listed in `specs/FOR-58/spec.md`.

## Edge Cases

- First call before any profile row exists → return defaults (dark theme,
  metric units, `firstRunCompleted: false`), not a 404.
- Partial update (e.g. theme only) must not clobber unrelated preference
  fields.
- Invalid enum values (unit, theme mode, activity level) → 400
  `VALIDATION_ERROR`, never silently coerced to a default.
- Onboarding answers re-submitted after `firstRunCompleted: true` → allowed
  (treated as a profile edit), does not lock the record.

## Open Questions

- One combined aggregate/table vs. three (profile / preferences /
  onboarding) — recommend keeping the API surface as one read model even if
  persistence is split across tables internally; document the final choice
  in `ai-context.md`/code comments during implementation.
- Whether unit preference exposes a real multi-value enum now or a
  single-value metric default with room to extend — this story only needs
  to persist and expose the field; FOR-119 owns the selector UI/behavior.
