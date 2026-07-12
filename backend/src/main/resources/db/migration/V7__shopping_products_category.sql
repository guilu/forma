-- Shopping: add category to shopping_products (FOR-106).
--
-- Additive migration on top of V4 (ADR-003). A single ADD COLUMN statement per the
-- FOR-100 H2 lesson (H2 rejects multi-column ALTER TABLE ADD COLUMN; this story only
-- adds one column so a single statement is safe either way). NOT NULL DEFAULT 'OTROS'
-- backfills existing rows in place so old products keep loading unchanged (backward
-- compatible) while still guaranteeing a non-null value at the DB layer; the domain
-- (ShoppingProduct compact constructor) defaults a null/omitted category to OTROS too,
-- as a safety net for programmatic construction.
ALTER TABLE shopping_products ADD COLUMN category VARCHAR(32) NOT NULL DEFAULT 'OTROS';
