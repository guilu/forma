-- Labels a food can carry.
--
-- Additive on top of V49 (ADR-003). Everything the catalog says about a food so far is either a
-- number or a single choice: its macros, its group, its dominant macro. None of that answers "is
-- this vegan", "is it frozen", "does it have gluten" — questions with no natural place among the
-- columns and no reason to be one each, since the list of them is open by nature.
--
-- Two tables rather than a column: a food carries any number of labels and a label describes any
-- number of foods, and spelling that as a text column would produce something nobody can query
-- without LIKE.
--
-- WHAT A TAG IS FOR, AND WHAT IT IS NOT
--
-- The source document lists fifteen examples, and three of them are a trap:
--
--   "Alto en proteína", "Bajo en grasa", "Rico en fibra"
--
-- Those are not facts about a food, they are readings of its macros — which the catalog already
-- holds and which somebody may correct tomorrow. Written as tags they become a second answer that
-- nothing keeps in step, and there is no agreed threshold to keep it in step WITH: high in protein
-- compared to what? They are deliberately not seeded here, and a curator adding them is writing
-- down an opinion that the numbers will quietly outgrow. This is the same reasoning that kept
-- `ratio` out of food_equivalence (V47) and that made primary_macro a default rather than a fact
-- (V44).
--
-- Four more overlap with something that exists: "Desayuno", "Cena", "Snack" and "Post entreno" are
-- nearly the MealType enum. They are not the same assertion — MealType says a logged meal WAS
-- breakfast, a tag says a food SUITS breakfast — so both can be true at once, and they are seeded.
-- Worth knowing before somebody tries to make one drive the other.

CREATE TABLE tag (
    -- A slug, like the food catalog's ids: stable, readable in a URL, and never renamed once
    -- something points at it. The label people read is `name`.
    id         VARCHAR(64) PRIMARY KEY,
    name       VARCHAR(64) NOT NULL,
    sort_order INTEGER     NOT NULL DEFAULT 0,
    enabled    BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX ux_tag_name ON tag (name);

CREATE TABLE food_tag (
    food_id VARCHAR(64) NOT NULL REFERENCES food_catalog (id),
    tag_id  VARCHAR(64) NOT NULL REFERENCES tag (id),
    PRIMARY KEY (food_id, tag_id)
);

CREATE INDEX ix_food_tag_tag ON food_tag (tag_id);

-- The vocabulary is seeded; the labelling is not. Saying "vegano is a label that exists" claims
-- nothing about any food, so it is safe to write down. Saying WHICH of the 23 foods are vegan is a
-- claim about each of them, and inventing 23 of those would be exactly the fabrication this
-- redesign has refused everywhere else (FOR-134). food_tag starts empty and a person fills it.
--
-- Ordered by kind rather than alphabetically: what a food is made of, then how it was kept, then
-- when it suits. That is the order somebody scanning a list of checkboxes reads them in.
INSERT INTO tag (id, name, sort_order) VALUES
  ('vegano',        'Vegano',        1),
  ('vegetariano',   'Vegetariano',   2),
  ('sin-gluten',    'Sin gluten',    3),
  ('sin-lactosa',   'Sin lactosa',   4),
  ('integral',      'Integral',      5),
  ('fresco',        'Fresco',        6),
  ('congelado',     'Congelado',     7),
  ('procesado',     'Procesado',     8),
  ('desayuno',      'Desayuno',      9),
  ('snack',         'Snack',        10),
  ('cena',          'Cena',         11),
  ('post-entreno',  'Post entreno', 12);
