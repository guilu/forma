-- Shopping: add unit + servings to shopping_list_items, generated_at to shopping_lists (FOR-108).
--
-- Read-model enrichment sibling of FOR-106 (which added productId/category). Additive migration on
-- top of V8 (ADR-003). One ADD COLUMN statement per ALTER TABLE, per the FOR-100/FOR-106 H2 lesson
-- (H2 rejects multi-column ALTER TABLE ADD COLUMN).
--
-- unit: NOT NULL DEFAULT 'UD' backfills existing rows in place so old items keep loading unchanged
-- (backward compatible), matching the ShoppingListItem compact constructor's own null -> UD default
-- as a safety net for programmatic construction.
--
-- servings: nullable, no default — a line item not linked to a nutrition food genuinely has no
-- servings count; NULL here (never a fabricated value) mirrors the domain/application resolution
-- rule (spec FOR-108 Edge Cases).
--
-- generated_at: NOT NULL DEFAULT CURRENT_TIMESTAMP backfills existing lists to the migration-run
-- timestamp, per spec FOR-108's suggested sentinel ("e.g. migration timestamp"), so the field is
-- never absent from the response.
ALTER TABLE shopping_list_items ADD COLUMN unit VARCHAR(16) NOT NULL DEFAULT 'UD';
ALTER TABLE shopping_list_items ADD COLUMN servings INTEGER;
ALTER TABLE shopping_lists ADD COLUMN generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
