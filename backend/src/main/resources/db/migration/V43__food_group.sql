-- Food groups become data instead of a compiled enum.
--
-- Additive on top of V42 (ADR-003). Until now the set of groups lived in three places at once: the
-- FoodCategory enum, the chk_food_catalog_category CHECK added by V35, and the FOOD-scoped rows of
-- category_display (V39). V39 said as much and defended it -- "adding or removing a category is a
-- schema change on purpose: rows point at these codes, and a category that disappears from under
-- them is a broken reference". That was true while the set lived in code, where nothing but a CHECK
-- could protect the references.
--
-- A table with a foreign key protects them better. The database refuses to delete a group any food
-- still points at, and refuses a food filed under a group that does not exist -- which is exactly
-- what the CHECK was approximating. So the reason for keeping the set closed in code is gone, and
-- with it the reason for splitting a group's identity from how it is drawn: label and icon move
-- into the row they describe.
--
-- This also lets the four groups the catalog has always been missing exist at all. Legumbres,
-- bebidas, condimentos and suplementos are not exotic -- lentils and olive oil are the same kind of
-- fact -- they were simply unreachable without a migration.

CREATE TABLE food_group (
    id         VARCHAR(32) PRIMARY KEY,
    name       VARCHAR(64)  NOT NULL,
    icon       VARCHAR(16),
    color      VARCHAR(16),
    sort_order INTEGER      NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE
);

-- The six existing groups keep their codes verbatim. Renaming CARBOHIDRATO to CARBOHIDRATOS would
-- buy nothing and rewrite every food_catalog row that points at it. Their names and icons are taken
-- from category_display's FOOD rows, so nobody sees a single character change.
--
-- The four new ones follow the same singular, accent-stripped convention. `color` is left null for
-- all ten: no screen reads it yet, and picking ten hex values nobody chose would be inventing a
-- design decision (FOR-134).
INSERT INTO food_group (id, name, icon, sort_order) VALUES
  ('CARBOHIDRATO', 'Carbohidrato', '🌾', 1),
  ('PROTEINA',     'Proteína',     '🍗', 2),
  ('FRUTA',        'Fruta',        '🍎', 3),
  ('VERDURA',      'Verdura',      '🥦', 4),
  ('GRASA',        'Grasa',        '🫒', 5),
  ('LACTEO',       'Lácteo',       '🥛', 6),
  ('LEGUMBRE',     'Legumbre',     '🫘', 7),
  ('BEBIDA',       'Bebida',       '🥤', 8),
  ('CONDIMENTO',   'Condimento',   '🧂', 9),
  ('SUPLEMENTO',   'Suplemento',   '💊', 10);

-- food_catalog.category becomes a real reference. Same values, same nullability: a food nobody has
-- classified stays unclassified (V35's own reasoning, unchanged).
ALTER TABLE food_catalog ADD COLUMN food_group_id VARCHAR(32);

UPDATE food_catalog SET food_group_id = category WHERE category IS NOT NULL;

ALTER TABLE food_catalog ADD CONSTRAINT fk_food_catalog_group
  FOREIGN KEY (food_group_id) REFERENCES food_group (id);

-- The CHECK and the column it guarded are what the foreign key replaces.
ALTER TABLE food_catalog DROP CONSTRAINT chk_food_catalog_category;
ALTER TABLE food_catalog DROP COLUMN category;

-- category_display keeps only the shopping aisles. A food group's name and icon now live on the
-- group itself, so leaving these rows behind would give the same group two labels and no rule about
-- which one wins.
DELETE FROM category_display WHERE scope = 'FOOD';

ALTER TABLE category_display DROP CONSTRAINT chk_category_display_scope;

ALTER TABLE category_display ADD CONSTRAINT chk_category_display_scope
  CHECK (scope IN ('SHOPPING'));
