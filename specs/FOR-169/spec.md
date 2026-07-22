# FOR-169: Empty first-run mode — remove seeded personal/demo data

Jira: https://dbhlab.atlassian.net/browse/FOR-169
Type: Tarea (story, no epic)

## Summary

Make Forma boot completely empty on a fresh database and before onboarding. Today
several Flyway migrations seed personal/demo data (Diego's profile + targets, a
legacy shopping list, the Mercadona catalog + active list) and in-code catalogs
(`FoodCatalog`, `NutritionDayCatalog`, `RunningPlanGenerator`,
`WorkoutTemplateCatalog`) are surfaced as if they were the user's *active* plan.
This story removes/neutralizes the known seeds, gates hardcoded catalogs/plans
behind explicit first-run completion, returns honest empty states from the API,
and shows onboarding/configuration CTAs on the frontend — **without deleting real
data on existing installs.**

Separate three concepts explicitly (see Notes):

- **System master data / catalogs** (may exist, system-owned).
- **Available templates** (plans the user *could* adopt).
- **Active user data** (only after onboarding/explicit configuration).

## User/System Flow

1. Fresh DB → Flyway runs. No active user data rows exist (see Acceptance
   Criteria table).
2. User opens any screen before completing onboarding
   (`UserProfile.firstRunCompleted == false`).
3. Read models/services return empty (no seeded profile, no active list, no
   "active" plan/meals/training).
4. Each screen renders its empty state + a CTA to onboard/configure (reuse the
   FOR-60 shared empty/error states already wired across the app).
5. After onboarding (`firstRunCompleted = true`, already set by
   `SubmitOnboardingAnswersRequest` → `UserProfileService`) or explicit
   configuration, plans/lists/catalogs may be exposed as active user data.

## Functional Requirements

### Backend — migrations (idempotent, fresh-DB-safe, existing-install-safe)

- Add a new migration (next version is **V23**) that neutralizes only the
  **known seeds**, never real data:
  - Legacy shopping seed from `V5__shopping_lists.sql` (4 demo products, 1 active
    list, 4 items).
  - Mercadona seed from `V22__real_food_and_mercadona_catalog.sql` (23 products,
    1 active list, 23 items).
  - `user_profile` row `default-user` from `V20__user_profile_personal_targets.sql`
    **only if it still matches the known seed** (Diego's name/height/baseline/
    targets) **and** `firstRunCompleted = false` (no real onboarding yet).
- The migration must be safe on existing installs: match seeds by their known
  identifiers/values and skip rows that have diverged (user edited data), or scope
  the cleanup so it cannot touch real data. Prefer conditional
  `DELETE ... WHERE <matches known seed>` over blanket truncation.
- Decide + document whether `shopping_products` is **system master catalog**
  (kept, but not tied to an active list) or **user data** (removed). Acceptance
  criteria allows either as long as no *active user list* references them
  pre-onboarding.

### Backend — services/endpoints

- Ensure in-code catalogs/plans (`FoodCatalog`, `NutritionDayCatalog`,
  `RunningPlanGenerator`, `WorkoutTemplateCatalog`) are **not exposed as the
  user's active plan/meals/training** while `firstRunCompleted == false`. They may
  remain as available templates/master data, surfaced only after onboarding or
  explicit configuration.
- Read endpoints return correct empty states on first run (empty collections /
  documented "no active X" responses), never a fabricated active plan.

### Frontend — empty states + CTAs

- Show onboarding/configuration CTAs (not Diego's data or preloaded lists/plans)
  for: mediciones, lista de compra, nutrición/plan, entrenamiento, and dashboard
  (empty or guided). Reuse the empty-state components already built for these
  screens; wire their CTA to the onboarding/config entry point.

### Tests

- Fresh-DB / first-run backend tests asserting the Acceptance Criteria table.
- Migration tests: existing-install data is preserved; known seeds are removed.
- Frontend tests: each screen renders its empty state + CTA on first run (no
  seeded personal data leaks through).

## Non-Functional Requirements

- **Data safety (highest priority):** migrations must never delete real user data
  on existing installs. Only known seeds or fresh-DB scope.
- No fabrication (ADR-001, AGENTS.md "repository state has priority"): empty means
  empty; do not synthesize an "active" plan to fill the screen.
- Idempotent migration; app boots with no errors on an empty DB.
- Keep the master-data / template / active-data separation explicit in code and
  docs so future seeds don't regress this.

## Data Model Notes

- Gate: `UserProfile.firstRunCompleted` (domain field, set true by
  `UserProfileService` on onboarding submit). Use it as the first-run boundary.
- Seed sources to neutralize: `V5__shopping_lists.sql`,
  `V20__user_profile_personal_targets.sql`, `V22__real_food_and_mercadona_catalog.sql`.
- In-code (not DB) plans/catalogs: `FoodCatalog`, `NutritionDayCatalog`,
  `RunningPlanGenerator`, `WorkoutTemplateCatalog` (all under
  `dev/diegobarrioh/forma/domain/`).
- Tables expected empty on fresh DB (Acceptance Criteria):
  `body_measurements`, `shopping_lists`, `shopping_list_items`,
  `shopping_products` (unless kept as master catalog — decision + doc),
  `goal`, `goal_milestone`, `insight_history`, `training_session_status`,
  `weekly_tracking_record`, `meal_log_entry`, `water_intake_entry`,
  `user_profile` (0, or a minimal default with no personal data — technical
  decision + doc).

## Acceptance Criteria

- [ ] On a new DB after Flyway, no active user-data rows exist per the table above.
- [ ] App boots without errors on an empty DB.
- [ ] Frontend shows no Diego data nor preloaded lists/plans before onboarding.
- [ ] Screens show empty states + appropriate first-run CTAs.
- [ ] Migrations do not delete real data on existing installs — only known seeds or
      fresh-DB scope.
- [ ] Backend + frontend tests cover the empty first-run.

## Edge Cases

- Existing install where the user edited the seeded profile/list → cleanup must
  skip it (treated as real data), not delete it.
- Partially-seeded DB (some seed rows manually removed) → migration stays
  idempotent, no error.
- Catalog decision: if `shopping_products` stays as master catalog, no *active
  list* may reference it pre-onboarding.
- Onboarding completed but no measurements yet → measurements screen still empty
  (empty ≠ pre-onboarding), CTA to add first measurement.

## Open Questions

- `user_profile`: delete `default-user` entirely on fresh DB, or keep a minimal
  no-personal-data default row? (Recommend: no seeded personal profile; create on
  onboarding.)
- `shopping_products`: master catalog (kept) vs user data (removed)? Decide and
  document; AC allows either provided no active user list references it pre-onboarding.
- Should catalogs/templates be exposed via a distinct "templates" surface
  (available-but-not-active) now, or deferred to a later story? (Recommend: gate
  now, dedicated templates UI later.)
</content>
</invoke>
