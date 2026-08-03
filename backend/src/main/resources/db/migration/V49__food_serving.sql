-- A food's portions, as rows.
--
-- Additive on top of V48 (ADR-003). food_catalog carried a single serving_size_g, which forced
-- every food to have exactly one sensible portion. That is wrong for most of them: a banana is
-- small, medium or large; oil is a teaspoon, a tablespoon or a splash; bread is a slice or a roll.
-- Logging "1.5 servings of banana" was the only way to say "a big one", which is a number nobody
-- thinks in.
--
-- This does NOT add those portions. It moves the one each food already has into a table that can
-- hold the others, and the rest is somebody sitting down to write them.
--
-- ONE DEFAULT PER FOOD, PORTABLY. The obvious constraint is a partial unique index, and H2's CREATE
-- INDEX has no WHERE clause (ADR-011). The documented substitute is a nullable sentinel: SQL
-- compares NULLs as distinct on both H2 and PostgreSQL, so a UNIQUE over (food_id, default_marker)
-- permits any number of NULLs and exactly one 'Y'. ADR-011 proposed this for active plans and
-- nothing has used it until now.
--
-- `default_marker` is the whole fact; there is no is_default beside it. Two columns for one truth is
-- how they end up disagreeing.

CREATE TABLE food_serving (
    -- VARCHAR rather than UUID because this table is backfilled, and the two databases spell
    -- "generate a UUID" differently (RANDOM_UUID vs gen_random_uuid) with no portable form. The
    -- backfilled rows take the food's own id, which is unique by construction since each food starts
    -- with exactly one; everything written later gets a minted uuid string. Both are opaque.
    id             VARCHAR(64)  PRIMARY KEY,
    food_id        VARCHAR(64)  NOT NULL REFERENCES food_catalog (id),
    -- What to call this portion: "Mediano", "Cucharada", "Rebanada". NULL for the plain one a food
    -- starts with, which is not "the unnamed portion" so much as "the portion, before anybody
    -- bothered to distinguish sizes".
    name           VARCHAR(64),
    grams          NUMERIC(7,1) NOT NULL,
    default_marker CHAR(1),
    sort_order     INTEGER      NOT NULL DEFAULT 0
);

ALTER TABLE food_serving ADD CONSTRAINT chk_food_serving_grams CHECK (grams > 0);

ALTER TABLE food_serving ADD CONSTRAINT chk_food_serving_marker CHECK (
  default_marker IS NULL OR default_marker = 'Y'
);

CREATE UNIQUE INDEX ux_food_serving_default ON food_serving (food_id, default_marker);

CREATE INDEX ix_food_serving_food ON food_serving (food_id);

-- Every food that had a serving keeps it, as its default and under no name. A food that had none
-- gets no row: "nobody has decided a portion for this" is a state V35 introduced on purpose and
-- inventing one here would undo that (FOR-134).
INSERT INTO food_serving (id, food_id, grams, default_marker)
SELECT id, id, serving_size_g, 'Y' FROM food_catalog WHERE serving_size_g IS NOT NULL;

-- The column goes. Leaving it would make a food's portion answerable from two places, which is the
-- same fault this redesign removed from the food catalog itself (#192), from the food groups (#193)
-- and from the equivalences (#200).
ALTER TABLE food_catalog DROP COLUMN serving_size_g;
