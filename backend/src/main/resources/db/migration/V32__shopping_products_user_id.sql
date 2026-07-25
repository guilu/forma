-- FOR-145c gap-table migration (3 of 5, ADR-012 design section 3 "5 GAP tables").
--
-- shopping_products (FOR-36, migration V4) had ZERO owner-scoping -- ShoppingProductRepository
-- read/wrote every account's rows. Its PK (id UUID) is a plain generated identity, so this is a
-- simple NET-NEW column, same shape as V30 (no PK reconstruction needed). The V25 FK
-- shopping_products.linked_food_item_id -> food_catalog(id) is untouched by this migration.
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011).
ALTER TABLE shopping_products ADD COLUMN user_id UUID;
UPDATE shopping_products SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE user_id IS NULL;
ALTER TABLE shopping_products ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE shopping_products
  ADD CONSTRAINT fk_shopping_products_user FOREIGN KEY (user_id) REFERENCES users (id);
CREATE INDEX idx_shopping_products_user ON shopping_products (user_id);
