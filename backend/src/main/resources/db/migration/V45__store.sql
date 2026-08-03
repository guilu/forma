-- Supermarket chains become data instead of a compiled enum.
--
-- Additive on top of V44 (ADR-003). Same move V43 made for food groups, and for the same reason: a
-- foreign key protects the references in both directions, which is what the CHECK added by V36 and
-- redefined by V41 was approximating. Adding Lidl or Alcampo stops being a schema change.
--
-- WHAT THE ENUM CONFLATED. There are two sets here and they were one:
--
--   1) Which chains a product can be filed under. That is data: a chain we do not integrate with at
--      all is still somewhere a person buys food, and CARREFOUR has been in the enum since V36
--      with no adapter behind it, so the two already disagreed.
--   2) Which chains we can import a catalogue FROM. That is code — one StoreCatalogSource per
--      chain — and no row will ever conjure one.
--
-- This table is the first. A store with no source behind it simply cannot be imported from, and the
-- import path already says so ("No hay catálogo disponible para: X"). The OTRAS row inherits that
-- honestly instead of documenting it as a special case, which is what V41's enum comment did.
--
-- `store_product.store` already holds exactly these three tokens, so there is NO data migration
-- here: the column becomes a reference to values it was already carrying.

CREATE TABLE store (
    id         VARCHAR(32) PRIMARY KEY,
    name       VARCHAR(64) NOT NULL,
    logo_url   TEXT,
    website    TEXT,
    sort_order INTEGER     NOT NULL,
    enabled    BOOLEAN     NOT NULL DEFAULT TRUE
);

-- Names are what a person reads; the ids are the tokens already stored on every product row and are
-- kept verbatim. `logo_url` is left null for all three: nothing renders it yet and picking logos
-- nobody chose would be inventing (FOR-134). The websites are the public storefronts, which is the
-- one fact about a chain that is neither a guess nor a design decision.
INSERT INTO store (id, name, logo_url, website, sort_order) VALUES
  ('MERCADONA', 'Mercadona', NULL, 'https://tienda.mercadona.es', 1),
  ('CARREFOUR', 'Carrefour', NULL, 'https://www.carrefour.es', 2),
  -- Anything no chain we track sells: bought online, at a local shop, at a market stall (FOR-200).
  -- It has no storefront by definition, so it has no website either.
  ('OTRAS', 'Otras', NULL, NULL, 99);

ALTER TABLE store_product ADD CONSTRAINT fk_store_product_store
  FOREIGN KEY (store) REFERENCES store (id);

ALTER TABLE store_product DROP CONSTRAINT chk_store_product_store;
