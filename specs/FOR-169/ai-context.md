# FOR-169 AI Context

## Story

FOR-169 — Empty first-run mode: remove seeded personal/demo data.

## Intent

Forma must be able to boot completely empty on a fresh DB and before onboarding,
so a new user never sees Diego's personal data or preloaded demo lists/plans.
Success = a fresh database has zero active user-data rows, the app boots clean,
every screen shows an empty state + first-run CTA, and existing installs keep
their real data.

## Relevant Documents

- `AGENTS.md` (repository state has priority; no fabrication)
- `docs/architecture-overview.md`
- ADR-001 (no domain rules/fabrication in the wrong layer), ADR-006 (API boundary)
- Seed migrations: `backend/src/main/resources/db/migration/V5__shopping_lists.sql`,
  `V20__user_profile_personal_targets.sql`,
  `V22__real_food_and_mercadona_catalog.sql`
- In-code catalogs/plans (domain): `FoodCatalog.java`, `NutritionDayCatalog.java`,
  `RunningPlanGenerator.java`, `WorkoutTemplateCatalog.java`
- `UserProfile.java` (`firstRunCompleted`), `UserProfileService.java`,
  `OnboardingAnswers.java`, `SubmitOnboardingAnswersRequest.java`
- Frontend onboarding: `frontend/src/pages/onboarding/*`

## Domain Notes

- **Three data classes (keep separate):** system master data/catalogs • available
  templates • active user data. Only *active user data* must be empty on first run;
  catalogs/templates may exist but must not be surfaced as the user's active plan.
- **First-run gate:** `UserProfile.firstRunCompleted` (already set true by
  `UserProfileService` when onboarding answers are submitted). Use it as the
  boundary between first-run empty mode and configured mode.
- Latest migration is **V22**; the cleanup migration is **V23**.

## Architectural Constraints

- Migrations MUST NOT delete real data on existing installs — match known seeds by
  their identifiers/values (and `firstRunCompleted == false` for the profile) or
  scope to fresh DB only. Conditional deletes, never blanket truncation.
- Migration must be idempotent and leave the app booting cleanly on an empty DB.
- No fabrication: empty states are real; do not synthesize an "active" plan to
  fill a screen (ADR-001, AGENTS.md).
- Do not push presentation concerns into the backend; frontend owns CTA wiring.

## Common Pitfalls

- Truncating tables (would wipe real data on existing installs). Use
  seed-matching conditional deletes.
- Deleting `default-user` unconditionally even when the user has real onboarding
  data — guard with `firstRunCompleted == false` and seed-value match.
- Removing the in-code catalogs entirely — they are master data/templates; the fix
  is to stop exposing them as *active* pre-onboarding, not to delete them.
- Forgetting frontend leaks: a screen may still show seeded data via a cached
  fixture or a hardcoded default — verify each screen against a truly empty API.
- `shopping_products` ambiguity (catalog vs user data) — decide and document; keep
  behavior consistent with the active-list rule.

## Suggested Implementation Order

1. Confirm/lock the master-data vs template vs active-data decision (Open
   Questions) — especially `shopping_products` and `user_profile default-user`.
2. Backend: add V23 cleanup migration (conditional, idempotent, existing-install
   safe) + migration tests (fresh DB → empty; existing edited data → preserved).
3. Backend: gate in-code catalogs/plans behind `firstRunCompleted`; return empty
   states from read endpoints on first run + tests.
4. Frontend: ensure each screen (mediciones, lista de compra, nutrición,
   entrenamiento, dashboard) renders empty state + first-run CTA against an empty
   API + tests.
5. End-to-end fresh-DB smoke: boot clean, no Diego data, CTAs present.

## Validation

- Run Flyway on a fresh DB → assert the Acceptance-Criteria table (all listed
  tables 0, or documented minimal defaults).
- Run the migration against a fixture representing an existing install with edited
  data → assert real data preserved, only known seeds removed.
- Frontend tests: each screen shows its empty state + CTA when the API returns
  empty; no seeded personal data rendered.
- Manual: fresh boot shows onboarding/empty everywhere, no errors in logs.
</content>
</invoke>
