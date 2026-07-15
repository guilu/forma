-- Meal-log entries (FOR-127, first implementable slice of FOR-102).
--
-- One row per logged entry (per-entry-rows aggregate shape, mirroring the goal/goal_milestone
-- list-of-rows style, V11, rather than a single per-day summary row) — the per-day consumed totals
-- (MealLog#consumedTotals) are always derived on read by summing this table's rows, never stored
-- separately, so they can never drift out of sync with the entries (spec FOR-127 Open Questions).
-- Additive on top of V12 (ADR-003) — earlier migrations are untouched, and this table never
-- references (and this slice never mutates) nutrition_day/meal_template plan data, which is not
-- persisted at all (FOR-33 seeds the plan in code, not in the database).
--
-- food_item_id is a soft reference to the FOR-30 in-code FoodCatalog (not a FK — that catalog has
-- no table) and is NULL for a free/ad-hoc entry (spec FOR-127: "Free/ad-hoc entry with no catalog
-- food → store provided macros"). kcal/protein_g/carbs_g/fat_g are a snapshot of what was actually
-- consumed, computed once at logging time (via NutritionCalculator for catalog entries, or taken
-- directly for free entries) — a later change to the catalog never rewrites logged history.
--
-- owner_id exists even though authorization is not enforced yet (ADR-002 single-user MVP),
-- mirroring goal.owner_id/user_profile.owner_id: every row is account-scoped in shape, ready for a
-- real account id once authentication lands. Append-only for this slice (spec FOR-127 Open
-- Questions: "Default append-only unless trivial") — no update/delete path exists yet.
CREATE TABLE meal_log_entry (
    id           UUID PRIMARY KEY,
    owner_id     VARCHAR(64)  NOT NULL,
    log_date     DATE         NOT NULL,
    meal_type    VARCHAR(32)  NOT NULL,
    food_item_id VARCHAR(64),
    name         VARCHAR(200) NOT NULL,
    kcal         INTEGER      NOT NULL,
    protein_g    NUMERIC(6, 1) NOT NULL,
    carbs_g      NUMERIC(6, 1) NOT NULL,
    fat_g        NUMERIC(6, 1) NOT NULL,
    logged_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_meal_log_entry_owner_date ON meal_log_entry (owner_id, log_date);
