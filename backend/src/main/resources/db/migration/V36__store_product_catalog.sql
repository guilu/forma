-- Store product catalog: global, admin-maintained purchasable products (FOR-191).
--
-- Additive on top of V35 (ADR-003). Sibling of food_catalog (V25): food_catalog answers "what is
-- this food nutritionally", store_product answers "where do I buy it and what does it cost". The
-- link between them is food_id, a real FK -- a store product with no matching food is buyable but
-- cannot be planned from, which is a legitimate state (cleaning products, a food not yet in the
-- catalog), so the column is nullable.
--
-- WHY A NEW TABLE INSTEAD OF WIDENING shopping_products (V4/V7/V32):
-- shopping_products is USER data. V32 gave it a user_id precisely because every account edits its
-- own prices, notes and package sizes, and V23 deleted the seeded rows on the grounds that "shopping
-- products are USER data, not a system master catalog". This table is the master catalog that
-- statement says does not exist yet: one row per product per store, shared by every account, editable
-- only by an admin. Nothing about shopping_products changes here, and the two coexist -- a later
-- slice decides how a user's list is built FROM this catalog.
--
-- ONE TABLE, ONE store COLUMN, not a table per chain: the columns are identical for every
-- supermarket, so a second table would duplicate the schema and every query against it. Adding
-- Carrefour becomes an enum value and a CHECK edit.
--
-- MONEY: price_eur is the product's own price for the package described by package_size (the
-- fitness_os.xlsm Mercadona sheet's "Precio estimado €" column). The V22 shopping_products seed also
-- carried a prorated WEEKLY line cost in estimated_price_eur; that figure is a property of one
-- person's plan, not of the product, so it is deliberately absent here. NUMERIC, never floating
-- point (V4 precedent).

CREATE TABLE store_product (
    id           VARCHAR(64)  PRIMARY KEY,
    store        VARCHAR(32)  NOT NULL,
    name         VARCHAR(200) NOT NULL,
    food_id      VARCHAR(64)  REFERENCES food_catalog (id),
    package_size VARCHAR(100),
    price_eur    NUMERIC(8, 2),
    url          TEXT,
    category     VARCHAR(32)  NOT NULL DEFAULT 'OTROS',
    notes        TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE store_product ADD CONSTRAINT chk_store_product_store
  CHECK (store IN ('MERCADONA', 'CARREFOUR'));

-- Mirrors domain.ShoppingCategory (V7's closed set), so a product filed here can be grouped by the
-- same aisles the shopping list already uses.
ALTER TABLE store_product ADD CONSTRAINT chk_store_product_category
  CHECK (category IN ('FRUTAS_Y_VERDURAS', 'PROTEINAS', 'LACTEOS_Y_HUEVOS',
                      'CEREALES_Y_LEGUMBRES', 'GRASAS_Y_ACEITES', 'OTROS'));

CREATE INDEX idx_store_product_store ON store_product (store);

-- The 23 Mercadona products, transcribed from V22 (docs/fitness_os.xlsm sheets Mercadona/Compra).
-- V22 seeded them into shopping_products and V23 deleted them again as personal data; they belong
-- here, where they are reference data nobody owns.
--
-- price_eur takes V22's price_per_unit_eur (the sheet's real product price), NOT its
-- estimated_price_eur (that column held the prorated weekly cost -- see the money note above).
-- Ids are derived from the linked food id so they read as what they are and stay stable.
INSERT INTO store_product (id, store, name, food_id, package_size, price_eur, url, category, notes)
VALUES
  ('mercadona-oats', 'MERCADONA', 'Copos de avena Brüggen', 'oats', '500 g', 1.55,
   'https://tienda.mercadona.es/product/86341/copos-avena-bruggen-caja', 'CEREALES_Y_LEGUMBRES',
   'Precio no extraíble de HTML público'),
  ('mercadona-whey-protein', 'MERCADONA', 'Whey proteína', 'whey-protein', '1 kg', 22.00,
   NULL, 'OTROS', 'No suele ser compra Mercadona'),
  ('mercadona-banana', 'MERCADONA', 'Plátanos', 'banana', 'kg', 1.99,
   'https://tienda.mercadona.es/search-results?query=platano', 'FRUTAS_Y_VERDURAS', NULL),
  ('mercadona-eggs', 'MERCADONA', 'Huevos grandes L', 'eggs', '12 uds', 2.70,
   'https://tienda.mercadona.es/product/31504/huevos-grandes-l-paquete', 'LACTEOS_Y_HUEVOS', NULL),
  ('mercadona-egg-whites', 'MERCADONA', 'Claras de huevo líquidas pasteurizadas', 'egg-whites',
   'botella', 1.85,
   'https://tienda.mercadona.es/product/31312/claras-huevo-liquidas-pasteurizadas-botella',
   'LACTEOS_Y_HUEVOS', NULL),
  ('mercadona-fresh-cheese', 'MERCADONA', 'Queso fresco batido 0% MG Hacendado', 'fresh-cheese',
   '500 g', 1.50,
   'https://tienda.mercadona.es/product/51071/queso-fresco-batido-desnatado-0-mg-hacendado-tarrina',
   'LACTEOS_Y_HUEVOS', NULL),
  ('mercadona-yogurt', 'MERCADONA', 'Yogur proteína', 'yogurt', 'pack', 1.40,
   'https://tienda.mercadona.es/search-results?query=yogur%20proteina', 'LACTEOS_Y_HUEVOS', NULL),
  ('mercadona-chicken', 'MERCADONA', 'Pechugas enteras de pollo', 'chicken', 'kg', 7.20,
   'https://tienda.mercadona.es/product/3724/pechugas-enteras-pollo-bandeja', 'PROTEINAS',
   'Fresco varía por bandeja'),
  ('mercadona-turkey', 'MERCADONA', 'Pavo lonchas/corte', 'turkey', 'paquete', 2.50,
   'https://tienda.mercadona.es/product/56162/pechuga-pollo-hacendado-corte-paquete', 'PROTEINAS',
   'Sustituible por pollo/pavo'),
  ('mercadona-tuna', 'MERCADONA', 'Atún natural', 'tuna', 'pack latas', 3.85,
   'https://tienda.mercadona.es/search-results?query=atun%20natural', 'PROTEINAS', NULL),
  ('mercadona-fish', 'MERCADONA', 'Merluza', 'fish', 'kg', 7.50,
   'https://tienda.mercadona.es/search-results?query=merluza', 'PROTEINAS',
   'Congelada suele abaratar'),
  ('mercadona-salmon', 'MERCADONA', 'Salmón', 'salmon', 'kg', 14.50,
   'https://tienda.mercadona.es/search-results?query=salmon', 'PROTEINAS',
   'Principal variable de presupuesto'),
  ('mercadona-rice', 'MERCADONA', 'Arroz redondo/largo', 'rice', '1 kg', 1.35,
   'https://tienda.mercadona.es/search-results?query=arroz', 'CEREALES_Y_LEGUMBRES', NULL),
  ('mercadona-whole-wheat-pasta', 'MERCADONA', 'Pasta integral', 'whole-wheat-pasta', '500 g', 1.10,
   'https://tienda.mercadona.es/search-results?query=pasta%20integral', 'CEREALES_Y_LEGUMBRES',
   NULL),
  ('mercadona-potato', 'MERCADONA', 'Patatas', 'potato', '5 kg', 5.60,
   'https://tienda.mercadona.es/search-results?query=patatas', 'CEREALES_Y_LEGUMBRES',
   'Precio citado en noticia reciente; verificar'),
  ('mercadona-sweet-potato', 'MERCADONA', 'Boniato', 'sweet-potato', 'kg', 2.35,
   'https://tienda.mercadona.es/search-results?query=boniato', 'CEREALES_Y_LEGUMBRES', NULL),
  ('mercadona-whole-wheat-bread', 'MERCADONA', 'Pan integral', 'whole-wheat-bread', 'paquete', 1.40,
   'https://tienda.mercadona.es/search-results?query=pan%20integral', 'CEREALES_Y_LEGUMBRES', NULL),
  ('mercadona-vegetables', 'MERCADONA', 'Verdura variada', 'vegetables', 'kg', 2.50,
   'https://tienda.mercadona.es/search-results?query=verdura', 'FRUTAS_Y_VERDURAS',
   'Mezcla fresco/congelado'),
  ('mercadona-salad', 'MERCADONA', 'Ensaladas preparadas', 'salad', 'bolsa', 1.35,
   'https://tienda.mercadona.es/search-results?query=ensalada', 'FRUTAS_Y_VERDURAS', NULL),
  ('mercadona-olive-oil', 'MERCADONA', 'Aceite de oliva virgen extra', 'olive-oil', '1 L', 5.50,
   'https://tienda.mercadona.es/search-results?query=aceite%20oliva', 'GRASAS_Y_ACEITES', NULL),
  ('mercadona-almonds-walnuts', 'MERCADONA', 'Frutos secos naturales', 'almonds-walnuts', '200 g',
   2.90, 'https://tienda.mercadona.es/search-results?query=almendras%20naturales',
   'GRASAS_Y_ACEITES', NULL),
  ('mercadona-berries', 'MERCADONA', 'Frutos rojos congelados', 'berries', 'bolsa', 2.25,
   'https://tienda.mercadona.es/search-results?query=frutos%20rojos%20congelados',
   'FRUTAS_Y_VERDURAS', NULL),
  ('mercadona-skim-milk', 'MERCADONA', 'Leche desnatada', 'skim-milk', '1 L', 0.90,
   'https://tienda.mercadona.es/search-results?query=leche%20desnatada', 'LACTEOS_Y_HUEVOS', NULL);
