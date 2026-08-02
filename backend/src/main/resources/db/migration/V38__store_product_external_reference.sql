-- Store products remember where they were imported from (FOR-195).
--
-- Additive on top of V37 (ADR-003). Two columns, both nullable, both about the same thing: a row
-- that came from a shop's own catalogue can be found there again.
--
--   * external_id — the shop's own id for the product. Ours is a slug WE chose ("mercadona-oats"
--     for the V36 seed, "mercadona-4241" for an import), so it cannot be relied on to carry theirs:
--     the seeded rows were transcribed from a spreadsheet and have no shop id at all. Parsing our
--     id to recover theirs would make a naming convention load-bearing, which is how a rename
--     becomes an outage.
--   * image_url — the shop's product photo. Stored rather than derived because it is their CDN
--     path, not something we can build from an id.
--
-- Both stay NULL for the 23 seeded rows and for anything typed by hand: those were never imported,
-- so there is nothing to refresh them against, and the screen offers no refresh where there is no
-- source. That is the honest state, not a gap to backfill by guessing.

ALTER TABLE store_product ADD COLUMN external_id VARCHAR(64);

ALTER TABLE store_product ADD COLUMN image_url TEXT;

-- One row per shop product per chain: importing the same product twice must update the row that
-- exists, never sit a second copy beside it with a different price.
ALTER TABLE store_product
  ADD CONSTRAINT uq_store_product_store_external UNIQUE (store, external_id);
