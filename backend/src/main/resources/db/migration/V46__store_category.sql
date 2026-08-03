-- Each shop's own aisles, as the shop draws them.
--
-- Additive on top of V45 (ADR-003). Nothing here replaces `store_product.category`, and that is the
-- point: there are two vocabularies and they answer different questions.
--
--   store_product.category  -- one of OUR six aisles. It is what the shopping list groups by, it is
--                              the same words whichever shop a product came from, and a product
--                              bought at a market stall still has one. An admin chooses it.
--   store_category          -- what the SHOP calls that shelf, copied verbatim, hierarchy and all.
--                              Nobody chooses it; it is imported, and it changes when the shop
--                              changes it.
--
-- Mapping the second onto the first automatically was rejected when the import was written
-- (ImportableProduct: "never mapped automatically onto our own closed set") and this does not
-- reopen it. The shop's aisle becomes something we can browse and filter by; it does not become a
-- decision.
--
-- Until now the shop's aisle was shown to the admin as a hint and then thrown away — no column held
-- it. This is that column, plus the tree it belongs to.
--
-- HIERARCHY. Mercadona publishes three levels (26 roots -> 151 subcategories -> shelves) and the
-- crawler already walks all three, keeping only the middle name. A self-referencing parent_id
-- mirrors that without pinning the depth, so a chain with two levels or four needs no migration.
--
-- IDENTITY is (store_id, external_id): the shop's own id for that aisle. NOT the slug, and that is
-- deliberate — two shelves under different parents may well read alike ("Otros"), and a unique
-- index on (store_id, parent_id, slug) would not protect the roots anyway, since SQL compares NULLs
-- as distinct on both H2 and Postgres (ADR-011). The slug is for reading and routing, not identity.

CREATE TABLE store_category (
    id          VARCHAR(64)  PRIMARY KEY,
    store_id    VARCHAR(32)  NOT NULL REFERENCES store (id),
    parent_id   VARCHAR(64)  REFERENCES store_category (id),
    external_id VARCHAR(64)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(200) NOT NULL,
    level       INTEGER      NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX ux_store_category_external ON store_category (store_id, external_id);

CREATE INDEX ix_store_category_parent ON store_category (parent_id);

-- `level` is derivable by walking parent_id, and is stored anyway: every screen that renders a tree
-- wants to indent by it, and every query that wants "just the top shelves" would otherwise need a
-- recursive CTE to ask a question the row already knows the answer to.
--
-- Storing it means it can disagree with the parent chain, so the invariant is pinned as far as a
-- portable CHECK reaches: a row has a parent exactly when it is not a root. The rest of the rule
-- (level = parent.level + 1) is enforced where the tree is written, since a CHECK cannot read
-- another row.
ALTER TABLE store_category ADD CONSTRAINT chk_store_category_root CHECK (
  (parent_id IS NULL AND level = 0)
  OR (parent_id IS NOT NULL AND level > 0)
);

-- No seed. The aisles of a shop are the shop's to state, and there is no offline copy of them —
-- writing out Mercadona's 26 roots from memory would be inventing data that goes stale the day they
-- reorganise (FOR-134). The table fills on the first catalogue crawl.

-- Where a product sits in ITS shop's tree. Nullable, and it will stay null for a long time: every
-- product that predates this column, every product added by hand, and every product filed under
-- OTRAS, which has no catalogue and therefore no aisles.
ALTER TABLE store_product ADD COLUMN store_category_id VARCHAR(64);

ALTER TABLE store_product ADD CONSTRAINT fk_store_product_store_category
  FOREIGN KEY (store_category_id) REFERENCES store_category (id);
