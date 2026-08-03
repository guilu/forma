-- Recipes: a named list of foods with amounts.
--
-- Additive on top of V51 (ADR-003). The last piece of the source document's model. A plan that says
-- "avena, leche, proteína y frutos rojos" four mornings a week is saying one thing, and saying it as
-- four separate meal entries loses that it is one dish somebody makes.
--
-- NOTHING IS STORED THAT THE INGREDIENTS ALREADY SAY. No kcal, no macros, no totals: they are the
-- sum over the ingredients of what food_catalog holds, so storing them would freeze an answer that
-- has to move when somebody corrects a food. Same reason food_equivalence has no `ratio` (V47) and
-- primary_macro is a default rather than a fact (V44).
--
-- A KNOWN DUPLICATE, WRITTEN DOWN RATHER THAN HIDDEN
--
-- domain/MealTemplate is already this shape: a name, a list of (food, grams), and no totals. It is
-- not persisted — it lives as constants in NutritionDayCatalog — so there is no table being
-- duplicated here, but there is a concept. A day's meal is a recipe plus a time and a day type, and
-- the honest model has one of them referencing the other.
--
-- That rewiring is deliberately NOT done here. docs/FORMA_Spec_Modelo_Datos_Plan_Alimentacion.md is
-- queued and is going to redefine how days and meals relate; reshaping the nutrition-day path now,
-- before reading it, risks doing the work twice. This comment is the debt, so that whoever reads
-- that document knows the two concepts are meant to become one.
--
-- NO meal_types TABLE, though the document lists one. There are already two ways to say "this is for
-- breakfast": the MealType enum, which classifies a meal that was LOGGED, and the tags seeded by
-- V50, which say a food SUITS a moment. A third would be the same fact three times with no rule
-- about which wins.

CREATE TABLE recipe (
    id          VARCHAR(64)  PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    -- How many portions the whole thing makes. Without it a stew for four reads as a meal for one,
    -- and every per-serving figure computed from it is wrong by a factor of four.
    servings    INTEGER      NOT NULL DEFAULT 1,
    notes       TEXT,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE recipe ADD CONSTRAINT chk_recipe_servings CHECK (servings > 0);

CREATE UNIQUE INDEX ux_recipe_name ON recipe (name);

CREATE TABLE recipe_ingredient (
    recipe_id  VARCHAR(64)  NOT NULL REFERENCES recipe (id),
    food_id    VARCHAR(64)  NOT NULL REFERENCES food_catalog (id),
    grams      NUMERIC(7,1) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    -- One line per food. Listing oats twice in one recipe is somebody having typed it twice, and
    -- the two amounts would have to be added by every reader or by none of them.
    PRIMARY KEY (recipe_id, food_id)
);

ALTER TABLE recipe_ingredient ADD CONSTRAINT chk_recipe_ingredient_grams CHECK (grams > 0);

CREATE INDEX ix_recipe_ingredient_food ON recipe_ingredient (food_id);

-- No seed. Which dishes exist is editorial, and the source document's one worked example ("avena
-- overnight": copos, leche, proteína, frutos rojos) states no amounts at all — writing them in
-- would be inventing the recipe rather than recording it (FOR-134).
--
-- Worth knowing before anybody fills this in: an ingredient's grams are read the way its food is
-- recorded (V51). A recipe listing 80 g of rice is listing it dry, because that is what
-- food_catalog holds, and the dish it produces is cooked. A recipe is a raw-to-cooked
-- transformation and this table does not model that — it records what goes in.
