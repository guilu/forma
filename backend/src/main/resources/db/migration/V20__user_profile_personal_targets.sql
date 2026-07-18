-- Profile: personal plan targets + baseline (FOR-149, epic FOR-148 "Personalizar FORMA a Diego",
-- slice 1 of 7).
--
-- Additive on top of V19 (ADR-003) — earlier migrations (including V8's user_profile table) are
-- untouched. One column per ALTER TABLE ... ADD COLUMN statement (repo convention, see
-- ai-context.md Common Pitfalls: "Putting multiple columns in one ALTER statement").
--
-- These columns extend the FOR-107 user_profile aggregate with the target/reference data from the
-- Excel *Perfil* sheet: baseline (peso/grasa/IMC iniciales, plan reference values — NOT a
-- body_measurement row; SEGUIMIENTO stays empty until slice 7/FOR-155) and personal targets (base
-- kcal, body-fat/weight target ranges, fat/carb macros). Protein reuses the existing
-- protein_target_g column (V8) rather than adding a duplicate — the seed below sets it to 160.
ALTER TABLE user_profile ADD COLUMN baseline_weight_kg NUMERIC(5, 1);
ALTER TABLE user_profile ADD COLUMN baseline_body_fat_pct NUMERIC(4, 1);
ALTER TABLE user_profile ADD COLUMN baseline_bmi NUMERIC(4, 1);
ALTER TABLE user_profile ADD COLUMN base_calories_kcal NUMERIC(6, 1);
ALTER TABLE user_profile ADD COLUMN body_fat_target_min_pct NUMERIC(4, 1);
ALTER TABLE user_profile ADD COLUMN body_fat_target_max_pct NUMERIC(4, 1);
ALTER TABLE user_profile ADD COLUMN weight_target_min_kg NUMERIC(5, 1);
ALTER TABLE user_profile ADD COLUMN weight_target_max_kg NUMERIC(5, 1);
ALTER TABLE user_profile ADD COLUMN fat_target_g NUMERIC(6, 1);
ALTER TABLE user_profile ADD COLUMN carbs_target_g NUMERIC(6, 1);

-- Seed Diego's baseline + targets as plan (fijo) reference data under the fixed OWNER_ID
-- (ADR-002 single-user MVP; see UserProfileService.OWNER_ID = 'default-user'). Values are sourced
-- verbatim from docs/fitness_os.xlsm sheet Perfil, never fabricated (spec FOR-149):
--   Nombre Diego, Altura 1.80 m.
--   Baseline: peso 73.6 kg, grasa 14.7 %, IMC 22.7.
--   Objetivo grasa 12-13 %; objetivo peso 73-75 kg (recomposicion); kcal base 2300;
--   proteina 160 g (reutiliza protein_target_g); grasas 70 g; carbohidratos 260 g.
-- Only the fields explicitly given by the Perfil sheet are seeded (name, height, baseline,
-- targets); sex/mainGoal/activityLevel/birthDate are left unset here to avoid fabricating data not
-- present in the source sheet (documented gap, see spec.md/AGENTS.md "never invent requirements").
-- Merge rather than plain-insert so existing single-user databases (for example deployments where
-- onboarding/profile data was already saved before FOR-149) can apply V20 without a duplicate-key
-- startup failure. The seed is source-of-truth plan reference data from Perfil, so it updates the
-- fixed default-user row when present and inserts it only for fresh databases.
MERGE INTO user_profile AS target
USING (
    VALUES (
        'default-user', 'Diego', 180.0,
        160.0,
        73.6, 14.7, 22.7,
        2300.0, 12.0, 13.0,
        73.0, 75.0, 70.0, 260.0
    )
) AS source (
    owner_id, name, height_cm,
    protein_target_g,
    baseline_weight_kg, baseline_body_fat_pct, baseline_bmi,
    base_calories_kcal, body_fat_target_min_pct, body_fat_target_max_pct,
    weight_target_min_kg, weight_target_max_kg, fat_target_g, carbs_target_g
)
ON target.owner_id = source.owner_id
WHEN MATCHED THEN UPDATE SET
    name = source.name,
    height_cm = source.height_cm,
    protein_target_g = source.protein_target_g,
    baseline_weight_kg = source.baseline_weight_kg,
    baseline_body_fat_pct = source.baseline_body_fat_pct,
    baseline_bmi = source.baseline_bmi,
    base_calories_kcal = source.base_calories_kcal,
    body_fat_target_min_pct = source.body_fat_target_min_pct,
    body_fat_target_max_pct = source.body_fat_target_max_pct,
    weight_target_min_kg = source.weight_target_min_kg,
    weight_target_max_kg = source.weight_target_max_kg,
    fat_target_g = source.fat_target_g,
    carbs_target_g = source.carbs_target_g
WHEN NOT MATCHED THEN INSERT (
    owner_id, name, height_cm,
    protein_target_g,
    baseline_weight_kg, baseline_body_fat_pct, baseline_bmi,
    base_calories_kcal, body_fat_target_min_pct, body_fat_target_max_pct,
    weight_target_min_kg, weight_target_max_kg, fat_target_g, carbs_target_g
) VALUES (
    source.owner_id, source.name, source.height_cm,
    source.protein_target_g,
    source.baseline_weight_kg, source.baseline_body_fat_pct, source.baseline_bmi,
    source.base_calories_kcal, source.body_fat_target_min_pct, source.body_fat_target_max_pct,
    source.weight_target_min_kg, source.weight_target_max_kg, source.fat_target_g, source.carbs_target_g
);
