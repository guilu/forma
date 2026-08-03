-- Which food stands in for which, and on what grounds.
--
-- Additive on top of V46 (ADR-003). A plan that says "180 g de arroz" is useless to somebody who
-- has no rice, so the catalog needs to know what else would do — and "what else would do" depends
-- entirely on what the meal was for. Rice stands in for potato because of the carbohydrate; chicken
-- stands in for hake because of the protein. Those are different swaps and the table says which.
--
-- WHAT IS STORED IS THE DECISION, NOT THE ARITHMETIC.
--
-- Deliberately absent: `ratio` and `target_reference_g`. Both are functions of food_catalog and the
-- basis, so storing them would be storing an answer that stops being true the moment somebody
-- corrects a food's macros. That is not hypothetical here. The source document for this redesign
-- states "100 g arroz = 250 g patata"; computed from the seeded catalog the answer is 465 g,
-- because food_catalog holds rice raw (360 kcal/100 g) and the document was thinking of it cooked.
-- Had 250 been written into a column, nothing would ever have caught it.
--
--   target_reference_g = source_reference_g * source[basis] / target[basis]
--   ratio              = source[basis] / target[basis]
--
-- What IS stored is `source_reference_g`: "let us talk about this in portions of 100 g" is an
-- editorial choice, not a derivation, and it is the only number here that a person actually picks.
--
-- DIRECTIONAL. source -> target is one piece of advice and target -> source is another; wanting
-- both means writing both. Deriving the inverse automatically would assume a symmetry that stops
-- holding as soon as the two directions want different reference portions, or a different basis.

CREATE TABLE food_equivalence (
    id                      UUID         PRIMARY KEY,
    source_food_id          VARCHAR(64)  NOT NULL REFERENCES food_catalog (id),
    target_food_id          VARCHAR(64)  NOT NULL REFERENCES food_catalog (id),
    basis                   VARCHAR(16)  NOT NULL,
    source_reference_g      NUMERIC(7,1) NOT NULL,
    max_macro_deviation_pct NUMERIC(5,1),
    notes                   TEXT,
    enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Closed for the same reason PrimaryMacro is: calories and the three macronutrients are all there
-- is to hold equal, so this is a CHECK and not a table (V43 made the opposite call for food groups,
-- where the set really can grow).
ALTER TABLE food_equivalence ADD CONSTRAINT chk_food_equivalence_basis CHECK (
  basis IN ('CALORIES', 'PROTEIN', 'CARBS', 'FAT')
);

-- A food standing in for itself is not a substitution; every deviation would be zero and say
-- nothing.
ALTER TABLE food_equivalence ADD CONSTRAINT chk_food_equivalence_distinct CHECK (
  source_food_id <> target_food_id
);

ALTER TABLE food_equivalence ADD CONSTRAINT chk_food_equivalence_portion CHECK (
  source_reference_g > 0
);

-- A tolerance of zero would mark every substitution as excessive, which is a way of saying nothing.
-- Absent means nobody has decided what "too far" is — not that everything passes.
ALTER TABLE food_equivalence ADD CONSTRAINT chk_food_equivalence_tolerance CHECK (
  max_macro_deviation_pct IS NULL OR max_macro_deviation_pct > 0
);

-- The natural key. Basis is part of it on purpose: rice for potato on carbohydrate and rice for
-- potato on calories are two different pieces of advice, and a curator may well want both.
CREATE UNIQUE INDEX ux_food_equivalence_pair
  ON food_equivalence (source_food_id, target_food_id, basis);

CREATE INDEX ix_food_equivalence_source ON food_equivalence (source_food_id);

-- No seed. Which foods may stand in for which is editorial, and the one worked example available
-- (the "100 g arroz = 250 g patata" chain) disagrees with this catalog's own numbers by a factor of
-- nearly two — writing it in would be seeding a mistake (FOR-134).
--
-- Nor is the raw-versus-cooked gap it exposes fixed here: food_catalog records no preparation
-- state, so an equivalence between a raw food and a cooked one is silently comparing different
-- things. That is a real modelling gap, it predates this table, and it deserves its own change
-- rather than a column bolted on at the end of this one.
