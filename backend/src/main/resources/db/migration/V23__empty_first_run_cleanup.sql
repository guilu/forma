-- Empty first-run: remove seeded personal/demo data (FOR-169).
--
-- Goal: a fresh database (and a pre-onboarding install) must contain NO active
-- user data, so a new user never sees Diego's personal data or preloaded demo
-- lists. This migration neutralizes only the KNOWN seeds inserted by earlier
-- migrations; it never truncates and never touches real user data.
--
-- Existing-install safety: every delete below is scoped by the fixed seed
-- identifiers used in V5/V22 (deterministic UUIDs) and, for the profile, by the
-- seeded values AND first_run_completed = FALSE. Real user data has random UUIDs
-- and/or diverged values (or first_run_completed = TRUE after onboarding), so it
-- is never matched. Idempotent: re-running finds nothing to delete.
--
-- Decisions (FOR-169):
--   * shopping_products are USER data, not a system master catalog -> removed.
--   * user_profile 'default-user' Diego seed -> removed when onboarding has not
--     run and the row still matches the seed; the profile is (re)created by
--     onboarding.
--
-- The in-code catalogs/plans (FoodCatalog, NutritionDayCatalog,
-- RunningPlanGenerator, WorkoutTemplateCatalog) are NOT touched here (they are
-- code, not DB); the application layer gates them behind first_run_completed so
-- they are not exposed as the user's ACTIVE plan pre-onboarding.

-- Shopping items first (shopping_list_id is a real FK to shopping_lists). Covers
-- the V5 legacy demo list and the V22 Mercadona list, and any item pointing at a
-- known seed product.
DELETE FROM shopping_list_items
 WHERE shopping_list_id IN (
         'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
         'f0000000-0000-0000-0000-000000000001')
    OR product_id IN (
         '11111111-1111-1111-1111-111111111111',
         '22222222-2222-2222-2222-222222222222',
         '33333333-3333-3333-3333-333333333333',
         '44444444-4444-4444-4444-444444444444',
         'e0000001-0000-0000-0000-000000000001',
         'e0000002-0000-0000-0000-000000000002',
         'e0000003-0000-0000-0000-000000000003',
         'e0000004-0000-0000-0000-000000000004',
         'e0000005-0000-0000-0000-000000000005',
         'e0000006-0000-0000-0000-000000000006',
         'e0000007-0000-0000-0000-000000000007',
         'e0000008-0000-0000-0000-000000000008',
         'e0000009-0000-0000-0000-000000000009',
         'e0000010-0000-0000-0000-000000000010',
         'e0000011-0000-0000-0000-000000000011',
         'e0000012-0000-0000-0000-000000000012',
         'e0000013-0000-0000-0000-000000000013',
         'e0000014-0000-0000-0000-000000000014',
         'e0000015-0000-0000-0000-000000000015',
         'e0000016-0000-0000-0000-000000000016',
         'e0000017-0000-0000-0000-000000000017',
         'e0000018-0000-0000-0000-000000000018',
         'e0000019-0000-0000-0000-000000000019',
         'e0000020-0000-0000-0000-000000000020',
         'e0000021-0000-0000-0000-000000000021',
         'e0000022-0000-0000-0000-000000000022',
         'e0000023-0000-0000-0000-000000000023');

DELETE FROM shopping_lists
 WHERE id IN (
         'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
         'f0000000-0000-0000-0000-000000000001');

DELETE FROM shopping_products
 WHERE id IN (
         '11111111-1111-1111-1111-111111111111',
         '22222222-2222-2222-2222-222222222222',
         '33333333-3333-3333-3333-333333333333',
         '44444444-4444-4444-4444-444444444444',
         'e0000001-0000-0000-0000-000000000001',
         'e0000002-0000-0000-0000-000000000002',
         'e0000003-0000-0000-0000-000000000003',
         'e0000004-0000-0000-0000-000000000004',
         'e0000005-0000-0000-0000-000000000005',
         'e0000006-0000-0000-0000-000000000006',
         'e0000007-0000-0000-0000-000000000007',
         'e0000008-0000-0000-0000-000000000008',
         'e0000009-0000-0000-0000-000000000009',
         'e0000010-0000-0000-0000-000000000010',
         'e0000011-0000-0000-0000-000000000011',
         'e0000012-0000-0000-0000-000000000012',
         'e0000013-0000-0000-0000-000000000013',
         'e0000014-0000-0000-0000-000000000014',
         'e0000015-0000-0000-0000-000000000015',
         'e0000016-0000-0000-0000-000000000016',
         'e0000017-0000-0000-0000-000000000017',
         'e0000018-0000-0000-0000-000000000018',
         'e0000019-0000-0000-0000-000000000019',
         'e0000020-0000-0000-0000-000000000020',
         'e0000021-0000-0000-0000-000000000021',
         'e0000022-0000-0000-0000-000000000022',
         'e0000023-0000-0000-0000-000000000023');

-- Personal profile seed (V20 MERGE of Diego). Delete only when onboarding has
-- not completed AND the row still matches the seed baseline, so a real or edited
-- profile (or one created by onboarding) is preserved.
DELETE FROM user_profile
 WHERE owner_id = 'default-user'
   AND first_run_completed = FALSE
   AND name = 'Diego'
   AND baseline_weight_kg = 73.6
   AND baseline_body_fat_pct = 14.7
   AND baseline_bmi = 22.7;
