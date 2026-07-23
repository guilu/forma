# FOR-169 Test Plan

## Scope

Prove that a fresh DB / pre-onboarding state is completely empty of active user
data, that the app boots clean, that existing installs keep real data, and that
every screen shows an empty state + first-run CTA instead of seeded data.

## Domain Tests

- In-code catalogs/plans (`FoodCatalog`, `NutritionDayCatalog`,
  `RunningPlanGenerator`, `WorkoutTemplateCatalog`) are treated as master
  data/templates: not returned as the user's *active* plan while
  `firstRunCompleted == false`.
- `UserProfile.firstRunCompleted` correctly gates first-run vs configured behavior.

## Application Tests

- Read services return empty results on first run (no active shopping list, no
  active nutrition day/meals, no training status, no goals/insights) when the DB
  is fresh and `firstRunCompleted == false`.
- After onboarding sets `firstRunCompleted = true`, configured/available data can
  be exposed (guard flips correctly).

## Migration Tests (Flyway)

- **Fresh DB:** after all migrations incl. V23, assert row counts are 0 for:
  `body_measurements`, `shopping_lists`, `shopping_list_items`,
  `shopping_products` (0 or documented master-catalog behavior), `goal`,
  `goal_milestone`, `insight_history`, `training_session_status`,
  `weekly_tracking_record`, `meal_log_entry`, `water_intake_entry`;
  `user_profile` = 0 or a minimal no-personal-data default (per decision).
- **Existing install, untouched seed:** V23 removes the known V5/V22 shopping seed
  and the V20 `default-user` seed (when `firstRunCompleted == false`).
- **Existing install, edited data:** rows the user changed (diverged from the known
  seed, or `firstRunCompleted == true`) are PRESERVED — V23 must not delete them.
- **Idempotency:** running the migration against a partially-cleaned DB does not
  error and leaves the same end state.
- App context boots with an empty DB (Spring context + Flyway) without errors.

## API Tests

- First-run responses are honest empty states (empty collections / documented
  "no active X"), never a fabricated active plan or Diego's profile.

## UI Tests

- Mediciones: empty state + "registrar primera medición" CTA on empty API.
- Lista de compra: empty state + CTA (no Mercadona/legacy items).
- Nutrición: empty/plan-not-configured state + CTA (no active seeded plan).
- Entrenamiento: empty state + CTA (no active seeded sessions).
- Dashboard: empty or guided state (no Diego metrics/lists/plans).
- No screen renders seeded personal data when the API is empty.

## Edge Cases

- User edited the seeded profile/list before upgrade → not deleted.
- Partially-seeded DB (some seed rows already gone) → migration idempotent.
- Onboarding completed but no measurements yet → measurements still empty (empty ≠
  pre-onboarding) with an add-measurement CTA.
- `shopping_products` kept as master catalog → no active user list references it
  pre-onboarding.

## Fixtures

- Fresh-DB fixture (all migrations, no manual inserts).
- Existing-install fixture: known V5/V20/V22 seed rows present, `firstRunCompleted
  = false`.
- Edited-install fixture: seed rows diverged from known values and/or
  `firstRunCompleted = true`.
- Empty-API frontend fixtures per screen (measurements, shopping, nutrition,
  training, dashboard) returning empty collections.
</content>
</invoke>
