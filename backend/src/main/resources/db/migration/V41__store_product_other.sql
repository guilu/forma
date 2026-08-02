-- A shelf for what no supermarket sells (FOR-200).
--
-- Additive on top of V40 (ADR-003). The catalog assumed every product came from a chain we track,
-- and plenty do not: whey protein is bought online, a local baker has no API, a market stall has no
-- catalogue at all. Those rows still belong here — they are what a plan is built from — so the
-- closed set of stores gains a bucket for them.
--
-- WHY A VALUE AND NOT A NULLABLE COLUMN: `store` is also the filter. With NULL meaning "no shop",
-- "Todas" and "Sin tienda" would be two different questions answered by the same absence, and
-- `findAll(null)` already means "every chain". A value keeps both readable and keeps the UNIQUE
-- (store, external_id) honest.
--
-- OTRAS has no catalogue behind it by definition, so nothing imported or refreshed will ever carry
-- it: those rows have no external_id, and the screen offers no refresh where there is no source.

ALTER TABLE store_product DROP CONSTRAINT chk_store_product_store;

ALTER TABLE store_product ADD CONSTRAINT chk_store_product_store
  CHECK (store IN ('MERCADONA', 'CARREFOUR', 'OTRAS'));
