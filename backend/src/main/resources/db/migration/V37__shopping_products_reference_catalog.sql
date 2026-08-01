-- Shopping products reference the global catalog (FOR-192).
--
-- Additive on top of V36 (ADR-003). shopping_products stays per-user (V32) and stays the thing a
-- shopping list item points at; what changes is where its facts come from. A row is now one of two
-- things:
--
--   1. A REFERENCE to a catalog product (store_product_id set). Name, price, package, url and notes
--      are read from the catalog unless this row overrides them. An override is this account's own
--      figure -- a user correcting what a product costs them must not move the price for everyone.
--   2. A PRODUCT OF ITS OWN (store_product_id null), exactly as before: everything lives on the row.
--
-- That is why the columns below lose their NOT NULL: for a referencing row, null means "use the
-- catalog's value", which is a different fact from "the user set it to this". Collapsing the two
-- would make every regenerate look like a user-entered price.
--
-- EXISTING ROWS ARE LEFT ALONE. Every current row keeps store_product_id null and therefore keeps
-- behaving exactly as it did: they were entered by hand and nothing here knows which catalog product
-- they meant. Guessing from linked_food_item_id would silently repoint someone's data at a row they
-- never chose -- if a match is wanted it belongs in the UI, where a person can confirm it.
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011). One statement per ALTER
-- (the FOR-100 H2 lesson).

ALTER TABLE shopping_products ADD COLUMN store_product_id VARCHAR(64);

ALTER TABLE shopping_products
  ADD CONSTRAINT fk_shopping_products_store_product
  FOREIGN KEY (store_product_id) REFERENCES store_product (id);

CREATE INDEX idx_shopping_products_store_product ON shopping_products (store_product_id);

-- A user holds one row per catalog product at most: a second one would give the same product two
-- prices in the same list with nothing to choose between them.
ALTER TABLE shopping_products
  ADD CONSTRAINT uq_shopping_products_user_store_product UNIQUE (user_id, store_product_id);

ALTER TABLE shopping_products ALTER COLUMN name DROP NOT NULL;
ALTER TABLE shopping_products ALTER COLUMN estimated_price_eur DROP NOT NULL;

-- A row must carry its own name/price or point at a catalog row that carries them. Without this a
-- row with neither would be a product with no identity and no cost.
ALTER TABLE shopping_products
  ADD CONSTRAINT chk_shopping_products_reference_or_own
  CHECK (store_product_id IS NOT NULL
         OR (name IS NOT NULL AND estimated_price_eur IS NOT NULL));
