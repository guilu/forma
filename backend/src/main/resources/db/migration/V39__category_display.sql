-- Category names and icons become data (FOR-197).
--
-- Additive on top of V38 (ADR-003). Until now the label and the glyph of every category lived in
-- the frontend bundle — CATEGORY_LABELS in foodDisplay.ts, SHOPPING_CATEGORY_LABELS in
-- storeDisplay.ts — so renaming "Lácteo" or picking a different emoji meant a deploy. That is the
-- exact thing the admin screen exists to avoid.
--
-- WHAT THIS TABLE IS NOT: a list of categories. The SET stays closed, in the domain enums and in
-- the CHECK constraints of V7, V35 and V36 — this table only says how each member is written and
-- drawn. Adding or removing a category is a schema change on purpose: rows point at these codes,
-- and a category that disappears from under them is a broken reference, not a preference.
--
-- Two vocabularies, one table, told apart by `scope`: FoodCategory files an ingredient by what it
-- is made of, ShoppingCategory files a product by which aisle it sits in. They overlap in words and
-- in nothing else, so merging them would force one screen to lie.

CREATE TABLE category_display (
    scope VARCHAR(16) NOT NULL,
    code  VARCHAR(32) NOT NULL,
    label VARCHAR(64) NOT NULL,
    icon  VARCHAR(16),
    PRIMARY KEY (scope, code)
);

ALTER TABLE category_display ADD CONSTRAINT chk_category_display_scope
  CHECK (scope IN ('FOOD', 'SHOPPING'));

-- Seeded with exactly what the two frontend maps say today, so this migration changes where the
-- labels live and nothing about what anybody sees.
INSERT INTO category_display (scope, code, label, icon) VALUES
  ('FOOD', 'CARBOHIDRATO', 'Carbohidrato', '🌾'),
  ('FOOD', 'PROTEINA', 'Proteína', '🍗'),
  ('FOOD', 'FRUTA', 'Fruta', '🍎'),
  ('FOOD', 'VERDURA', 'Verdura', '🥦'),
  ('FOOD', 'GRASA', 'Grasa', '🫒'),
  ('FOOD', 'LACTEO', 'Lácteo', '🥛'),
  ('SHOPPING', 'FRUTAS_Y_VERDURAS', 'Frutas y verduras', '🥦'),
  ('SHOPPING', 'PROTEINAS', 'Proteínas', '🍗'),
  ('SHOPPING', 'LACTEOS_Y_HUEVOS', 'Lácteos y huevos', '🥛'),
  ('SHOPPING', 'CEREALES_Y_LEGUMBRES', 'Cereales y legumbres', '🌾'),
  ('SHOPPING', 'GRASAS_Y_ACEITES', 'Grasas y aceites', '🫒'),
  ('SHOPPING', 'OTROS', 'Otros', '🛒');
