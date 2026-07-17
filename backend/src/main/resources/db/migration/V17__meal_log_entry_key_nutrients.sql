-- Meal-log entry key nutrients (FOR-134): persist per-entry fibre/sugars/sodium/saturated fat.
--
-- Additive migration on top of V16 (ADR-003) — earlier migrations are untouched. All four columns
-- are NULLABLE with no default, so existing meal_log_entry rows (logged before this migration, under
-- FOR-127/V13) keep loading unchanged and simply report NULL = "unknown" for each key nutrient
-- (backward compatible; never a fabricated 0). This closes the FOR-134 persistence gap: a key
-- nutrient a food genuinely lacks stays NULL through the round trip, and the day-consumption read
-- model's documented null/partial-total rule then holds against the real persisted rows.
--
-- Snapshot semantics mirror the existing kcal/protein_g/carbs_g/fat_g columns: values are computed
-- once at logging time (from the FOR-30 FoodCatalog for a catalog entry via NutritionCalculator, or
-- taken directly for a free/ad-hoc entry) — a later change to the in-code catalog never rewrites
-- logged history.
--
-- Units: sodium_mg is in MILLIGRAMS (the conventional unit for sodium and how FoodItem carries it),
-- as an INTEGER matching the domain KeyNutrientTotals.sodiumMg type; the other three are in grams,
-- NUMERIC(6, 1) matching the protein_g/carbs_g/fat_g precision. One ADD COLUMN statement per ALTER
-- TABLE, per the V6/V7/V9 H2 lesson (H2 rejects multi-column ALTER TABLE ADD COLUMN).
ALTER TABLE meal_log_entry ADD COLUMN fiber_g NUMERIC(6, 1);
ALTER TABLE meal_log_entry ADD COLUMN sugars_g NUMERIC(6, 1);
ALTER TABLE meal_log_entry ADD COLUMN sodium_mg INTEGER;
ALTER TABLE meal_log_entry ADD COLUMN saturated_fat_g NUMERIC(6, 1);
