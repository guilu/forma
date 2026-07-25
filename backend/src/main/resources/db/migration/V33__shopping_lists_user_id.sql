-- FOR-145c gap-table migration (4 of 5, ADR-012 design section 3 "5 GAP tables").
--
-- shopping_lists (FOR-39, migration V5) had ZERO owner-scoping -- ShoppingListRepository read/
-- wrote every account's active list and items. NET-NEW user_id column on the PARENT table only
-- (simple ADD COLUMN, id is a plain generated UUID PK, no PK reconstruction needed, same shape as
-- V30/V32). shopping_list_items (V5's child table, containment FK to shopping_lists) stays scoped
-- THROUGH its parent list -- no user_id column of its own, per the design's "child tables scoped
-- via parent join" default: every item query in JdbcShoppingListRepository now joins/filters via
-- its owning shopping_lists.user_id instead of adding a denormalized column here.
--
-- Portable on H2 (MODE=PostgreSQL, tests) and PostgreSQL (ADR-003/ADR-011).
ALTER TABLE shopping_lists ADD COLUMN user_id UUID;
UPDATE shopping_lists SET user_id = '00000000-0000-0000-0000-000000000000'
  WHERE user_id IS NULL;
ALTER TABLE shopping_lists ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE shopping_lists
  ADD CONSTRAINT fk_shopping_lists_user FOREIGN KEY (user_id) REFERENCES users (id);
CREATE INDEX idx_shopping_lists_user ON shopping_lists (user_id);
