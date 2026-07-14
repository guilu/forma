# FOR-107 AI Context

## Story

FOR-107 — [STUB] User profile & preferences backend
(https://dbhlab.atlassian.net/browse/FOR-107)

## Intent

Give FORMA a real backend for the profile, unit preferences, default
objectives, theme preference and onboarding data that today either doesn't
exist or is stuck in `localStorage` (`frontend/src/pages/onboarding/
onboardingStorage.ts`, `frontend/src/theme/theme.ts`). This is the shared
prerequisite for FOR-119, FOR-120 and FOR-121 — it must land first.

## Blocked by

None. This story blocks: FOR-119, FOR-120, FOR-121.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-002-authentication.md` (single-user MVP, auth-ready shape)
- `docs/adr/ADR-003-persistence.md` (migration-driven schema)
- `docs/adr/ADR-005-api-design.md` (REST conventions, `ApiError` shape)
- `specs/FOR-58/spec.md` (profile/units/objectives fields, documents the
  "no backend yet" gap this story closes)
- `frontend/src/pages/onboarding/onboardingStorage.ts` (`OnboardingAnswers`/
  `OnboardingProgress` shape to mirror)
- `frontend/src/theme/theme.ts` (`ThemeMode` vocabulary to mirror)
- Jira: https://dbhlab.atlassian.net/browse/FOR-107

## Domain Notes

- No existing `delivery/profile` package; the current bounded contexts under
  `backend/src/main/java/dev/diegobarrioh/forma/delivery` are `insights`,
  `shopping`, `web`, `training`, `body`, `nutrition`, `error`.
- Existing patterns to follow: `delivery/shopping` + `application/
  ShoppingListService` + `ShoppingListRepository` show the hexagonal
  slice shape (domain → application port → delivery controller →
  persistence adapter) to replicate for profile/preferences.
- `ShoppingProduct`/`ShoppingCategory` (FOR-106) show the pattern for adding
  a new enum-backed field with a migration and a backward-compatible
  default — reuse the same approach for unit/theme enums here.

## Architectural Constraints

- Domain aggregate(s) must not depend on persistence/framework types
  (hexagonal, ADR-001/architecture-overview).
- Delivery DTOs distinct from domain objects (ADR-005); thin controllers.
- Migration-driven schema changes only (ADR-003); one `ALTER TABLE ADD
  COLUMN` per statement if extending an existing table (H2 gotcha from
  FOR-100/FOR-106).
- Owner/account field present even though authorization isn't enforced yet
  (ADR-002 — "must not be designed as if authorization does not matter").

## Common Pitfalls

- Returning 404 on first `GET` instead of sane defaults — this breaks the
  Settings/onboarding first-load flow for FOR-119/FOR-120/FOR-121.
- Letting a partial `PATCH` null out unrelated fields.
- Drifting from the frontend's `ThemeMode`/`OnboardingAnswers` vocabulary,
  which forces translation logic in three downstream frontend stories.

## Suggested Implementation Order

1. Domain aggregate(s) + validation (profile fields, unit/theme enums,
   default objectives, onboarding answers + first-run flag).
2. Persistence adapter + migration (ADR-003).
3. Application port(s): read (with defaults) and scoped updates.
4. `delivery/profile` controller + DTOs (ADR-005), `GET`/update endpoints.
5. Tests per `tests.md`; confirm existing shopping/training/nutrition/body
   endpoints are unaffected.

## Validation

Backend build + tests once available per AGENTS.md Verification guidance
(`backend/` build and test commands). Exercise `GET /api/v1/profile` before
and after writes; confirm defaults, partial-update isolation and enum
validation.
