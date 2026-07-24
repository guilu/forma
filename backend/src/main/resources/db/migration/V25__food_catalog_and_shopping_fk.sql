-- Food catalog persistence: second consumer of ADR-011 data model v2 (FOR-173).
--
-- Additive on top of V23 (independent of FOR-172/V24 -- only Flyway version-number ordering is
-- shared; FOR-172 creates exercise_catalog only and does NOT touch food_catalog). Creates
-- food_catalog (global reference data, VARCHAR(64) verbatim ids reused from domain.FoodCatalog,
-- per ADR-011 Zone 1) and promotes shopping_products.linked_food_item_id (V4, a soft reference
-- with no FK) to a real, nullable foreign key.
--
-- Seed data is transcribed VERBATIM from domain.FoodCatalog (23 foods, docs/fitness_os.xlsm sheet
-- Macros). Key nutrients (fiber_g/sugars_g/sodium_mg/saturated_fat_g) are seeded as SQL NULL
-- wherever the source FoodItem field is null -- per FOR-134/FOR-152 "never fabricate" rule, never
-- as 0. Coexists with the static FoodCatalog/FoodCatalogService/nutrition consumers -- no consumer
-- is repointed in this migration (FOR-176-adjacent follow-up).
--
-- FK safety: V23 (FOR-169, empty-first-run) already deletes all 27 (V5 + V22) seeded
-- shopping_products rows, so on any freshly migrated DB (CI/fresh install) shopping_products is EMPTY when this FK is
-- added -- trivially clean. On a long-lived install where a user has set their own
-- linked_food_item_id via the API (which today performs zero validation on that column), a
-- defensive idempotent NULL-repair runs before ADD CONSTRAINT, nulling any unresolvable link rather
-- than failing the migration -- correct semantics for a nullable soft-ref (unresolvable -> NULL,
-- never dangling). Statement order: (1) CREATE, (2) seed 23, (3) NULL-repair, (4) ADD FK -- the FK
-- must follow the seed since referenced rows must already exist.

CREATE TABLE food_catalog (
    id               VARCHAR(64)  PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    serving_size_g   NUMERIC(6,1),
    kcal             INTEGER      NOT NULL,
    protein_g        NUMERIC(6,1) NOT NULL,
    carbs_g          NUMERIC(6,1) NOT NULL,
    fat_g            NUMERIC(6,1) NOT NULL,
    fiber_g          NUMERIC(6,1),
    sugars_g         NUMERIC(6,1),
    sodium_mg        NUMERIC(7,1),
    saturated_fat_g  NUMERIC(6,1),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 23 rows, verbatim from domain.FoodCatalog (order: id, name, serving_size_g, kcal, protein_g,
-- carbs_g, fat_g, fiber_g, sugars_g, sodium_mg, saturated_fat_g).
INSERT INTO food_catalog
  (id, name, serving_size_g, kcal, protein_g, carbs_g, fat_g, fiber_g, sugars_g, sodium_mg,
   saturated_fat_g)
VALUES
  ('oats', 'Copos de avena', 60.0, 370, 13.0, 60.0, 7.0, 10.6, 0.0, 2.0, 1.2),
  ('whey-protein', 'Whey proteína', 30.0, 390, 78.0, 8.0, 6.0, 0.0, NULL, NULL, NULL),
  ('banana', 'Plátano', 120.0, 89, 1.1, 23.0, 0.3, 2.6, 12.2, 1.0, 0.1),
  ('eggs', 'Huevos', 120.0, 143, 13.0, 1.0, 10.0, 0.0, NULL, 124.0, 3.3),
  ('egg-whites', 'Claras líquidas', 150.0, 48, 10.5, 0.7, 0.2, 0.0, NULL, NULL, NULL),
  ('fresh-cheese', 'Queso fresco batido 0%', 250.0, 46, 8.5, 3.5, 0.1, NULL, NULL, NULL, NULL),
  ('yogurt', 'Yogur proteína', 200.0, 59, 10.0, 4.0, 0.2, NULL, NULL, NULL, NULL),
  ('chicken', 'Pechuga pollo', 200.0, 110, 23.0, 0.0, 2.0, 0.0, 0.0, NULL, NULL),
  ('turkey', 'Pavo lonchas/corte', 150.0, 105, 22.0, 1.0, 2.0, NULL, NULL, NULL, NULL),
  ('tuna', 'Atún natural', 120.0, 116, 25.0, 0.0, 1.0, 0.0, 0.0, NULL, NULL),
  ('fish', 'Merluza', 200.0, 74, 16.0, 0.0, 1.0, 0.0, 0.0, NULL, NULL),
  ('salmon', 'Salmón', 180.0, 208, 20.0, 0.0, 13.0, 0.0, 0.0, NULL, NULL),
  ('rice', 'Arroz', 80.0, 360, 7.0, 79.0, 1.0, NULL, NULL, NULL, NULL),
  ('whole-wheat-pasta', 'Pasta integral', 80.0, 350, 13.0, 70.0, 2.0, NULL, NULL, NULL, NULL),
  ('potato', 'Patata', 300.0, 77, 2.0, 17.0, 0.1, NULL, NULL, NULL, NULL),
  ('sweet-potato', 'Boniato', 250.0, 86, 1.6, 20.0, 0.1, NULL, NULL, NULL, NULL),
  ('whole-wheat-bread', 'Pan integral', 80.0, 250, 9.0, 44.0, 4.0, NULL, NULL, NULL, NULL),
  ('vegetables', 'Verdura variada', 300.0, 35, 2.0, 6.0, 0.3, NULL, NULL, NULL, NULL),
  ('salad', 'Ensalada preparada', 150.0, 25, 1.5, 4.0, 0.2, NULL, NULL, NULL, NULL),
  ('olive-oil', 'Aceite oliva virgen extra', 10.0, 900, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0, 14.0),
  ('almonds-walnuts', 'Almendras/nueces', 25.0, 600, 20.0, 10.0, 54.0, NULL, NULL, NULL, NULL),
  ('berries', 'Frutos rojos congelados', 100.0, 50, 1.0, 10.0, 0.5, NULL, NULL, NULL, NULL),
  ('skim-milk', 'Leche desnatada', 250.0, 35, 3.5, 5.0, 0.1, NULL, NULL, NULL, NULL);

-- Defensive idempotent NULL-repair: unlink any pre-existing non-catalog soft-ref so the FK below
-- applies cleanly on ANY install. No-op on fresh/CI DBs (shopping_products is already empty after
-- V23). Only matters for a long-lived production DB with a user-set bogus linked_food_item_id.
UPDATE shopping_products
   SET linked_food_item_id = NULL
 WHERE linked_food_item_id IS NOT NULL
   AND linked_food_item_id NOT IN (SELECT id FROM food_catalog);

-- Promote the soft-link to a real, nullable FK -- first-ever integrity enforcement on
-- linked_food_item_id. Column stays NULLABLE (unlinked products remain valid).
ALTER TABLE shopping_products
  ADD CONSTRAINT fk_shopping_products_food
  FOREIGN KEY (linked_food_item_id) REFERENCES food_catalog (id);
