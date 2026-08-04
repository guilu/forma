-- Nutrition plans: what somebody is meant to eat, as rows instead of constants.
--
-- Additive on top of V52 (ADR-003). Implements ZONE 2 of ADR-011 and sections 1-4 of
-- docs/FORMA_Spec_Modelo_Datos_Plan_Alimentacion.md.
--
-- Today the whole plan is three constants in domain/NutritionDayCatalog.java. Nobody can edit it,
-- it cannot vary by week, and it is the same for every account. This gives it a home.
--
--   nutrition_plan
--     └── nutrition_plan_day        (week 1 monday, week 1 tuesday, ...)
--           └── nutrition_plan_meal (desayuno, comida, cena)
--                 └── nutrition_plan_meal_item  (avena 60 g | 1 plátano mediano | 1 ración de guiso)
--
-- WHAT IS DELIBERATELY NOT HERE
--
-- No `calculated_kcal`, `calculated_protein_g` or any sibling, though section 11 of the source
-- document asks for them. A day's calculated total IS the sum of its items; storing it freezes an
-- answer that has to move when somebody corrects a food. The document's own reason for wanting them
-- — "the AI can state an incorrect total" — argues for VALIDATING what the AI wrote, not for keeping
-- a second copy of it. ADR-011 reached the same conclusion independently ("Derived aggregates
-- (nutrition day macro totals) are computed on read, never stored"), and it is the rule V44
-- (primary_macro), V47 (no ratio) and V52 (no recipe macros) already follow.
--
-- No `kcal_snapshot` / `protein_g_snapshot` / ... on the items, though section 5 asks for them. The
-- argument there is that a historic plan should not change when a food is corrected, and that is
-- true of HISTORY — but a plan is not history, it is an intention still in force. A plan that says
-- 222 kcal after the catalog moved to 228 is lying about what somebody will eat tomorrow. The
-- immutable record already exists and already snapshots: meal_log_entry (V13/V17) freezes kcal and
-- macros at the moment something was actually eaten. Plan = intention, log = history; the source
-- document draws that same line itself in section 10.
--
-- No `calendar_date` on the day, though section 2 asks for it. With start_date on the plan and
-- (week_number, day_number) on the day, the date is start_date + 7*(week-1) + (day-1). Nullable
-- "while the plan is a template" is exactly the case where start_date is null too, so the column
-- would be null precisely when it is underivable and redundant whenever it is not.
--
-- No `day_of_week`, for the same reason: it follows from day_number.
--
-- No `quantity` + `unit` + `grams` triple on the item (section 4). See the item table below.
--
-- No `weight_state` on the item (section 12). V51 records preparation on the FOOD, and the item's
-- macros come from that food; a state on the item could only ever agree with it (saying the same
-- thing twice) or disagree with it (making the item's own macros wrong). A food that exists both
-- raw and cooked is two foods, which is what V51 assumes.
--
-- ONE DEVIATION FROM ADR-011, RECORDED ON PURPOSE
--
-- ADR-011 puts status and active_marker on a shared `plan` parent so a nutrition plan and a training
-- plan activate together. That parent is not created here: there is no training_plan table and none
-- is being built, so it would be a join through an empty table. The invariant it exists to own lives
-- on nutrition_plan for now. When training_plan lands, that is the moment to lift both children
-- under the parent. ADR-011 is Proposed, not Accepted, which is what makes this a deviation to
-- record rather than one to refuse.

CREATE TABLE nutrition_plan (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id),
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    -- The goal this plan serves, in the vocabulary the profile already speaks (domain/MainGoal:
    -- COMPOSICION, RENDIMIENTO, HABITO). NULL means nobody said, not "no goal".
    objective   VARCHAR(32),
    status      VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    -- 'ONE ACTIVE PLAN PER USER', portably. active_marker is always NULL except while the plan is
    -- ACTIVE, when it holds '1'. SQL compares NULLs as distinct on both H2 and PostgreSQL, so a
    -- UNIQUE over (user_id, active_marker) admits any number of inactive plans and at most one
    -- active one. Do NOT "tidy this away": it is ADR-011's portable substitute for a PostgreSQL
    -- partial unique index, which H2 cannot parse. The CHECK below keeps it from disagreeing with
    -- status, so the pair can never say two different things.
    active_marker CHAR(1),
    start_date  DATE,
    end_date    DATE,
    -- What this plan was asked to hit. A band rather than a number because that is how a calorie
    -- objective is actually stated (2200-2400). All nullable: NULL means "no plan-level target",
    -- and the profile's own figures (V20 base_calories_kcal, protein_target_g, ...) stand instead.
    target_kcal_min  INTEGER,
    target_kcal_max  INTEGER,
    target_protein_g NUMERIC(6,1),
    target_carbs_g   NUMERIC(6,1),
    target_fat_g     NUMERIC(6,1),
    -- Audit of how the plan came to exist, not the plan itself.
    generated_by        VARCHAR(16) NOT NULL DEFAULT 'HUMAN',
    generation_prompt   TEXT,
    -- Model, catalog version, constraints. TEXT holding JSON rather than JSONB: H2 has none
    -- (ADR-011), and nothing queries inside this — it is read whole or not at all. The day this is
    -- filtered on is the day it stops being a blob and becomes columns.
    generation_metadata TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE nutrition_plan ADD CONSTRAINT chk_nutrition_plan_status CHECK (
  status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'ARCHIVED')
);

ALTER TABLE nutrition_plan ADD CONSTRAINT chk_nutrition_plan_marker_value CHECK (
  active_marker IS NULL OR active_marker = '1'
);

-- Written with IS NOT NULL rather than = '1', and that is not a style choice. ADR-011 proposes
--   (status = 'ACTIVE' AND active_marker = '1') OR (status <> 'ACTIVE' AND active_marker IS NULL)
-- and that form has a hole: for an ACTIVE row with a NULL marker, `active_marker = '1'` evaluates to
-- UNKNOWN rather than FALSE, the whole expression comes out UNKNOWN, and a CHECK constraint ACCEPTS
-- unknown. An ACTIVE plan carrying no marker would slip through — and since the unique index only
-- constrains rows that HAVE a marker, that is precisely the row that escapes the one-active-plan
-- rule the pair exists to enforce. IS NOT NULL is never UNKNOWN, and status is NOT NULL, so this
-- form is two-valued throughout. The value itself is checked separately above.
ALTER TABLE nutrition_plan ADD CONSTRAINT chk_nutrition_plan_active_marker CHECK (
  (status = 'ACTIVE' AND active_marker IS NOT NULL)
  OR (status <> 'ACTIVE' AND active_marker IS NULL)
);

ALTER TABLE nutrition_plan ADD CONSTRAINT chk_nutrition_plan_generated_by CHECK (
  generated_by IN ('HUMAN', 'AI')
);

-- A plan that ends before it starts is a typo, and every date derived from it would be nonsense.
ALTER TABLE nutrition_plan ADD CONSTRAINT chk_nutrition_plan_dates CHECK (
  start_date IS NULL OR end_date IS NULL OR end_date >= start_date
);

ALTER TABLE nutrition_plan ADD CONSTRAINT chk_nutrition_plan_kcal_band CHECK (
  target_kcal_min IS NULL OR target_kcal_max IS NULL OR target_kcal_max >= target_kcal_min
);

CREATE UNIQUE INDEX ux_nutrition_plan_user_active ON nutrition_plan (user_id, active_marker);

CREATE INDEX ix_nutrition_plan_user_status ON nutrition_plan (user_id, status);

CREATE TABLE nutrition_plan_day (
    id               UUID    PRIMARY KEY,
    nutrition_plan_id UUID   NOT NULL REFERENCES nutrition_plan (id),
    -- Kept from day one even while every plan is a single week (source document, section 2): a plan
    -- of four, eight or twelve weeks is the same shape, and retrofitting a key is not.
    week_number      INTEGER NOT NULL DEFAULT 1,
    -- Position within the week, 1 = monday. The weekday follows from it, and so does the calendar
    -- date once the plan has a start_date; neither is stored.
    day_number       INTEGER NOT NULL,
    -- RUNNING / STRENGTH / REST (domain/NutritionDayType). The source document calls this
    -- `training_type` and gives it its own values; there is already a vocabulary for "what kind of
    -- day is this", shared by the training calendar and the nutrition target through
    -- WeeklyTrainingDayPolicy, and a second one would drift from it. NULL = nobody classified it.
    day_type         VARCHAR(16),
    -- What this day was asked to hit. NOT the sum of its meals — that is computed on read. The two
    -- being different is the whole point: a day that adds up to 2100 against a target of 2320 is
    -- 220 kcal short, and a model where the target IS the sum can never say so.
    target_kcal      INTEGER,
    target_protein_g NUMERIC(6,1),
    target_carbs_g   NUMERIC(6,1),
    target_fat_g     NUMERIC(6,1),
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE nutrition_plan_day ADD CONSTRAINT chk_npd_week CHECK (week_number > 0);

ALTER TABLE nutrition_plan_day ADD CONSTRAINT chk_npd_day CHECK (day_number BETWEEN 1 AND 7);

-- One row per slot. Two rows claiming week 1 monday is two plans for the same morning, and nothing
-- would say which one to eat.
CREATE UNIQUE INDEX ux_npd_slot ON nutrition_plan_day (nutrition_plan_id, week_number, day_number);

CREATE TABLE nutrition_plan_meal (
    id                   UUID         PRIMARY KEY,
    nutrition_plan_day_id UUID        NOT NULL REFERENCES nutrition_plan_day (id),
    -- domain/MealType. A combined type like SNACK_POST_WORKOUT is not stored (source document,
    -- section 3): a day that has both a merienda and a post-entreno has two meals.
    meal_type            VARCHAR(32)  NOT NULL,
    name                 VARCHAR(200) NOT NULL,
    sort_order           INTEGER      NOT NULL DEFAULT 0,
    scheduled_time       TIME,
    target_kcal          INTEGER,
    target_protein_g     NUMERIC(6,1),
    target_carbs_g       NUMERIC(6,1),
    target_fat_g         NUMERIC(6,1),
    -- Free text for a meal that is a rule rather than a list ("una proteína, un carbohidrato y una
    -- verdura"). The structured form of that rule is section 8 of the source document and is
    -- deliberately not built: the document itself recommends instructions for the MVP.
    instructions         TEXT,
    -- Skippable, as a column. Today this is `meal.mealType() == MealType.POST_WORKOUT` written into
    -- NutritionDayResponse — a decision about one specific plan, hardcoded into the delivery layer
    -- for every plan there will ever be.
    optional             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_npm_slot ON nutrition_plan_meal (nutrition_plan_day_id, sort_order);

CREATE TABLE nutrition_plan_meal_item (
    id                    UUID         PRIMARY KEY,
    nutrition_plan_meal_id UUID        NOT NULL REFERENCES nutrition_plan_meal (id),
    -- Exactly one of these two. A row is a food or a dish, never both and never neither.
    food_id               VARCHAR(64)  REFERENCES food_catalog (id),
    recipe_id             VARCHAR(64)  REFERENCES recipe (id),
    -- Which portion `amount` counts, when it counts portions. NULL and a food means grams.
    serving_id            VARCHAR(64)  REFERENCES food_serving (id),
    -- ONE NUMBER, NOT THREE. The source document (section 4) asks for quantity + unit + grams; that
    -- is the same fact in three columns, free to disagree, and V49 already holds how many grams a
    -- named portion weighs. What `amount` counts is whatever the row names:
    --
    --   food_id + serving_id NULL   -> grams                (60.0 = 60 g de avena)
    --   food_id + serving_id set    -> portions of it       (1.0  = un plátano mediano)
    --   recipe_id                   -> servings of the dish (1.0  = una ración del guiso)
    --
    -- Grams are computed from food_serving.grams or from the recipe's ingredients, so a portion
    -- corrected from 120 g to 125 g moves every plan that says "un plátano mediano" — which is what
    -- somebody who wrote that meant.
    amount                NUMERIC(7,1) NOT NULL,
    sort_order            INTEGER      NOT NULL DEFAULT 0,
    preparation_notes     TEXT,
    optional              BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE nutrition_plan_meal_item ADD CONSTRAINT chk_npmi_amount CHECK (amount > 0);

ALTER TABLE nutrition_plan_meal_item ADD CONSTRAINT chk_npmi_food_xor_recipe CHECK (
  (food_id IS NOT NULL AND recipe_id IS NULL)
  OR (food_id IS NULL AND recipe_id IS NOT NULL)
);

-- A portion belongs to a food, so counting portions of a recipe is meaningless. The stronger rule —
-- that the serving belongs to THIS food — needs a composite key food_serving does not have, and is
-- enforced where items are written.
ALTER TABLE nutrition_plan_meal_item ADD CONSTRAINT chk_npmi_serving_needs_food CHECK (
  serving_id IS NULL OR food_id IS NOT NULL
);

CREATE UNIQUE INDEX ux_npmi_slot ON nutrition_plan_meal_item (nutrition_plan_meal_id, sort_order);

CREATE INDEX ix_npmi_food ON nutrition_plan_meal_item (food_id);

CREATE INDEX ix_npmi_recipe ON nutrition_plan_meal_item (recipe_id);

-- No seed here. The three days that exist today live in domain/NutritionDayCatalog as constants;
-- moving them into these tables is the next slice, together with pointing the endpoint at the
-- database and deleting the class. Seeding them now would mean two answers to "what do I eat on
-- monday" for as long as both exist.
