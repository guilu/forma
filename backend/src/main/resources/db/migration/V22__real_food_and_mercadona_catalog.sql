-- Shopping: real Mercadona product catalog + rebuilt weekly list (FOR-152, epic FOR-148
-- "Personalizar FORMA a Diego", slice 4).
--
-- Additive on top of V21 (ADR-003) — earlier migrations (V4/V5/V7/V9) are untouched; no ALTER is
-- needed here since url/package_size (V4) and category (V7) already exist on shopping_products.
-- This is a DATA migration: it replaces the 4 generic demo products + demo list seeded by V5 with
-- Diego's real 23 Mercadona products (docs/fitness_os.xlsm sheets Mercadona/Compra), so the demo
-- rows are removed first (never left alongside the real ones, per spec FOR-152 Common Pitfalls).
--
-- Values (name, formato, precio estimado €, coste semanal €, link, categoría, cantidad semanal)
-- are copied verbatim from the Mercadona/Compra sheets — "prices are editable estimates" (existing
-- MVP rule), links are reference only. The weekly total is a plan (fijo) reference figure, not user
-- input.
--
-- Money model: `estimated_price_eur` stores the product's WEEKLY LINE COST (the sheet's "Coste
-- semanal €" column, already prorated for fractional weekly amounts like 0.25 bote de whey or
-- 0.15 botella de aceite), not a strict per-kg/per-package unit price — `price_per_unit_eur`
-- carries that separate real unit price (the sheet's "Precio estimado €" column) for reference.
-- Every shopping_list_items row below then uses quantity = 1 (the `quantity` column is INTEGER, so
-- it cannot hold the sheet's fractional weekly amounts directly) so the derived weekly total
-- (ShoppingBudgetCalculator: unit price x quantity, summed) equals the sum of the 23
-- `estimated_price_eur` values without inventing a schema change. This mirrors exactly how
-- ShoppingListService#regenerate() already rebuilds a list (one item per product, quantity 1, cost
-- = the product's current estimatedPriceEur) — see ShoppingListService javadoc — so a future
-- regenerate reproduces the same weekly total.
--
-- Rounding: two sheet lines have a 3-decimal "Coste semanal" (Plátanos 3.184, Aceite 0.825); NUMERIC
-- (8,2) requires 2 decimals, so they are stored HALF_UP-rounded (3.18, 0.83), matching
-- ShoppingBudgetCalculator's own RoundingMode.HALF_UP. The 23-line sum is therefore 104.11 € (the
-- sheet's unrounded total is 104.109 €) — a documented one-line, sub-cent rounding artifact, not a
-- data error; the monthly extrapolation (x4.33) is consequently 450.80 € vs. the sheet's unrounded
-- 450.79 €.
--
-- Category mapping: the Mercadona sheet's own "Categoría" column (Carbohidratos/Proteína/
-- Lácteo/Fruta/Verdura/Grasa/Suplemento) is mapped onto the existing closed ShoppingCategory enum
-- (FOR-106): eggs/dairy -> LACTEOS_Y_HUEVOS (the sheet groups eggs under "Proteína", but the enum's
-- LACTEOS_Y_HUEVOS bucket exists specifically for dairy+eggs); meat/fish -> PROTEINAS; fruit/veg ->
-- FRUTAS_Y_VERDURAS; oil/nuts -> GRASAS_Y_ACEITES; carb staples (avena/arroz/pasta/patata/boniato/
-- pan) -> CEREALES_Y_LEGUMBRES; the whey protein supplement (not a Mercadona grocery aisle item) ->
-- OTROS. Every product links (soft link, FOR-35) to its FOR-152-reseeded FoodCatalog id — all 23
-- Mercadona products line up 1:1 with all 23 Macros-sheet foods.
--
-- Whey proteína's sheet "Link Mercadona" is the literal placeholder "Usuario/externo" (not a real
-- product URL, note: "No suele ser compra Mercadona") -> stored as NULL url rather than a fabricated
-- link; the real annotation is preserved in `notes`.

DELETE FROM shopping_list_items;
DELETE FROM shopping_lists;
DELETE FROM shopping_products;

INSERT INTO shopping_products
  (id, name, url, package_size, estimated_price_eur, price_per_unit_eur, linked_food_item_id,
   last_checked_at, notes, category)
VALUES
  ('e0000001-0000-0000-0000-000000000001', 'Copos de avena Brüggen',
   'https://tienda.mercadona.es/product/86341/copos-avena-bruggen-caja', '500 g',
   1.55, 1.55, 'oats', NULL, 'Precio no extraíble de HTML público', 'CEREALES_Y_LEGUMBRES'),
  ('e0000002-0000-0000-0000-000000000002', 'Whey proteína',
   NULL, '1 kg',
   5.50, 22.00, 'whey-protein', NULL, 'No suele ser compra Mercadona', 'OTROS'),
  ('e0000003-0000-0000-0000-000000000003', 'Plátanos',
   'https://tienda.mercadona.es/search-results?query=platano', 'kg',
   3.18, 1.99, 'banana', NULL, NULL, 'FRUTAS_Y_VERDURAS'),
  ('e0000004-0000-0000-0000-000000000004', 'Huevos grandes L',
   'https://tienda.mercadona.es/product/31504/huevos-grandes-l-paquete', '12 uds',
   5.40, 2.70, 'eggs', NULL, NULL, 'LACTEOS_Y_HUEVOS'),
  ('e0000005-0000-0000-0000-000000000005', 'Claras de huevo líquidas pasteurizadas',
   'https://tienda.mercadona.es/product/31312/claras-huevo-liquidas-pasteurizadas-botella',
   'botella', 3.70, 1.85, 'egg-whites', NULL, NULL, 'LACTEOS_Y_HUEVOS'),
  ('e0000006-0000-0000-0000-000000000006', 'Queso fresco batido 0% MG Hacendado',
   'https://tienda.mercadona.es/product/51071/queso-fresco-batido-desnatado-0-mg-hacendado-tarrina',
   '500 g', 6.00, 1.50, 'fresh-cheese', NULL, NULL, 'LACTEOS_Y_HUEVOS'),
  ('e0000007-0000-0000-0000-000000000007', 'Yogur proteína',
   'https://tienda.mercadona.es/search-results?query=yogur%20proteina', 'pack',
   5.60, 1.40, 'yogurt', NULL, NULL, 'LACTEOS_Y_HUEVOS'),
  ('e0000008-0000-0000-0000-000000000008', 'Pechugas enteras de pollo',
   'https://tienda.mercadona.es/product/3724/pechugas-enteras-pollo-bandeja', 'kg',
   14.40, 7.20, 'chicken', NULL, 'Fresco varía por bandeja', 'PROTEINAS'),
  ('e0000009-0000-0000-0000-000000000009', 'Pavo lonchas/corte',
   'https://tienda.mercadona.es/product/56162/pechuga-pollo-hacendado-corte-paquete', 'paquete',
   5.00, 2.50, 'turkey', NULL, 'Sustituible por pollo/pavo', 'PROTEINAS'),
  ('e0000010-0000-0000-0000-000000000010', 'Atún natural',
   'https://tienda.mercadona.es/search-results?query=atun%20natural', 'pack latas',
   7.70, 3.85, 'tuna', NULL, NULL, 'PROTEINAS'),
  ('e0000011-0000-0000-0000-000000000011', 'Merluza',
   'https://tienda.mercadona.es/search-results?query=merluza', 'kg',
   7.50, 7.50, 'fish', NULL, 'Congelada suele abaratar', 'PROTEINAS'),
  ('e0000012-0000-0000-0000-000000000012', 'Salmón',
   'https://tienda.mercadona.es/search-results?query=salmon', 'kg',
   10.15, 14.50, 'salmon', NULL, 'Principal variable de presupuesto', 'PROTEINAS'),
  ('e0000013-0000-0000-0000-000000000013', 'Arroz redondo/largo',
   'https://tienda.mercadona.es/search-results?query=arroz', '1 kg',
   1.35, 1.35, 'rice', NULL, NULL, 'CEREALES_Y_LEGUMBRES'),
  ('e0000014-0000-0000-0000-000000000014', 'Pasta integral',
   'https://tienda.mercadona.es/search-results?query=pasta%20integral', '500 g',
   1.10, 1.10, 'whole-wheat-pasta', NULL, NULL, 'CEREALES_Y_LEGUMBRES'),
  ('e0000015-0000-0000-0000-000000000015', 'Patatas',
   'https://tienda.mercadona.es/search-results?query=patatas', '5 kg',
   2.80, 5.60, 'potato', NULL, 'Precio citado en noticia reciente; verificar',
   'CEREALES_Y_LEGUMBRES'),
  ('e0000016-0000-0000-0000-000000000016', 'Boniato',
   'https://tienda.mercadona.es/search-results?query=boniato', 'kg',
   2.35, 2.35, 'sweet-potato', NULL, NULL, 'CEREALES_Y_LEGUMBRES'),
  ('e0000017-0000-0000-0000-000000000017', 'Pan integral',
   'https://tienda.mercadona.es/search-results?query=pan%20integral', 'paquete',
   1.40, 1.40, 'whole-wheat-bread', NULL, NULL, 'CEREALES_Y_LEGUMBRES'),
  ('e0000018-0000-0000-0000-000000000018', 'Verdura variada',
   'https://tienda.mercadona.es/search-results?query=verdura', 'kg',
   6.25, 2.50, 'vegetables', NULL, 'Mezcla fresco/congelado', 'FRUTAS_Y_VERDURAS'),
  ('e0000019-0000-0000-0000-000000000019', 'Ensaladas preparadas',
   'https://tienda.mercadona.es/search-results?query=ensalada', 'bolsa',
   5.40, 1.35, 'salad', NULL, NULL, 'FRUTAS_Y_VERDURAS'),
  ('e0000020-0000-0000-0000-000000000020', 'Aceite de oliva virgen extra',
   'https://tienda.mercadona.es/search-results?query=aceite%20oliva', '1 L',
   0.83, 5.50, 'olive-oil', NULL, 'Coste semanal prorrateado', 'GRASAS_Y_ACEITES'),
  ('e0000021-0000-0000-0000-000000000021', 'Frutos secos naturales',
   'https://tienda.mercadona.es/search-results?query=almendras%20naturales', '200 g',
   2.90, 2.90, 'almonds-walnuts', NULL, NULL, 'GRASAS_Y_ACEITES'),
  ('e0000022-0000-0000-0000-000000000022', 'Frutos rojos congelados',
   'https://tienda.mercadona.es/search-results?query=frutos%20rojos%20congelados', 'bolsa',
   2.25, 2.25, 'berries', NULL, NULL, 'FRUTAS_Y_VERDURAS'),
  ('e0000023-0000-0000-0000-000000000023', 'Leche desnatada',
   'https://tienda.mercadona.es/search-results?query=leche%20desnatada', '1 L',
   1.80, 0.90, 'skim-milk', NULL, NULL, 'LACTEOS_Y_HUEVOS');

INSERT INTO shopping_lists (id, week_start_date, status, notes, generated_at)
VALUES ('f0000000-0000-0000-0000-000000000001', DATE '2026-07-06', 'ACTIVE', NULL,
        TIMESTAMP WITH TIME ZONE '2026-07-06 08:00:00+00');

INSERT INTO shopping_list_items
  (id, shopping_list_id, product_id, quantity, estimated_cost_eur, checked, unit, servings)
VALUES
  ('a0000001-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001',
   'e0000001-0000-0000-0000-000000000001', 1, 1.55, FALSE, 'UD', NULL),
  ('a0000002-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001',
   'e0000002-0000-0000-0000-000000000002', 1, 5.50, FALSE, 'UD', NULL),
  ('a0000003-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000001',
   'e0000003-0000-0000-0000-000000000003', 1, 3.18, FALSE, 'UD', NULL),
  ('a0000004-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000001',
   'e0000004-0000-0000-0000-000000000004', 1, 5.40, FALSE, 'UD', NULL),
  ('a0000005-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000001',
   'e0000005-0000-0000-0000-000000000005', 1, 3.70, FALSE, 'UD', NULL),
  ('a0000006-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000001',
   'e0000006-0000-0000-0000-000000000006', 1, 6.00, FALSE, 'UD', NULL),
  ('a0000007-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000001',
   'e0000007-0000-0000-0000-000000000007', 1, 5.60, FALSE, 'UD', NULL),
  ('a0000008-0000-0000-0000-000000000008', 'f0000000-0000-0000-0000-000000000001',
   'e0000008-0000-0000-0000-000000000008', 1, 14.40, FALSE, 'UD', NULL),
  ('a0000009-0000-0000-0000-000000000009', 'f0000000-0000-0000-0000-000000000001',
   'e0000009-0000-0000-0000-000000000009', 1, 5.00, FALSE, 'UD', NULL),
  ('a0000010-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000001',
   'e0000010-0000-0000-0000-000000000010', 1, 7.70, FALSE, 'UD', NULL),
  ('a0000011-0000-0000-0000-000000000011', 'f0000000-0000-0000-0000-000000000001',
   'e0000011-0000-0000-0000-000000000011', 1, 7.50, FALSE, 'UD', NULL),
  ('a0000012-0000-0000-0000-000000000012', 'f0000000-0000-0000-0000-000000000001',
   'e0000012-0000-0000-0000-000000000012', 1, 10.15, FALSE, 'UD', NULL),
  ('a0000013-0000-0000-0000-000000000013', 'f0000000-0000-0000-0000-000000000001',
   'e0000013-0000-0000-0000-000000000013', 1, 1.35, FALSE, 'UD', NULL),
  ('a0000014-0000-0000-0000-000000000014', 'f0000000-0000-0000-0000-000000000001',
   'e0000014-0000-0000-0000-000000000014', 1, 1.10, FALSE, 'UD', NULL),
  ('a0000015-0000-0000-0000-000000000015', 'f0000000-0000-0000-0000-000000000001',
   'e0000015-0000-0000-0000-000000000015', 1, 2.80, FALSE, 'UD', NULL),
  ('a0000016-0000-0000-0000-000000000016', 'f0000000-0000-0000-0000-000000000001',
   'e0000016-0000-0000-0000-000000000016', 1, 2.35, FALSE, 'UD', NULL),
  ('a0000017-0000-0000-0000-000000000017', 'f0000000-0000-0000-0000-000000000001',
   'e0000017-0000-0000-0000-000000000017', 1, 1.40, FALSE, 'UD', NULL),
  ('a0000018-0000-0000-0000-000000000018', 'f0000000-0000-0000-0000-000000000001',
   'e0000018-0000-0000-0000-000000000018', 1, 6.25, FALSE, 'UD', NULL),
  ('a0000019-0000-0000-0000-000000000019', 'f0000000-0000-0000-0000-000000000001',
   'e0000019-0000-0000-0000-000000000019', 1, 5.40, FALSE, 'UD', NULL),
  ('a0000020-0000-0000-0000-000000000020', 'f0000000-0000-0000-0000-000000000001',
   'e0000020-0000-0000-0000-000000000020', 1, 0.83, FALSE, 'UD', NULL),
  ('a0000021-0000-0000-0000-000000000021', 'f0000000-0000-0000-0000-000000000001',
   'e0000021-0000-0000-0000-000000000021', 1, 2.90, FALSE, 'UD', NULL),
  ('a0000022-0000-0000-0000-000000000022', 'f0000000-0000-0000-0000-000000000001',
   'e0000022-0000-0000-0000-000000000022', 1, 2.25, FALSE, 'UD', NULL),
  ('a0000023-0000-0000-0000-000000000023', 'f0000000-0000-0000-0000-000000000001',
   'e0000023-0000-0000-0000-000000000023', 1, 1.80, FALSE, 'UD', NULL);
