-- Shopping: shopping_products table (FOR-36).
--
-- First user-editable, persisted Shopping entity (CRUD via FOR-36 API), unlike
-- the in-code reference catalogs (exercises, foods). Additive migration on top of
-- V3 (ADR-003). Prices use NUMERIC to avoid floating-point money loss. The link to
-- a nutrition food (linked_food_item_id) is a soft reference to the in-code FOR-30
-- catalog, so it is a plain column with no FK.
CREATE TABLE shopping_products (
    id                   UUID PRIMARY KEY,
    name                 VARCHAR(200) NOT NULL,
    url                  TEXT,
    package_size         VARCHAR(100),
    estimated_price_eur  NUMERIC(8, 2) NOT NULL,
    price_per_unit_eur   NUMERIC(8, 2),
    linked_food_item_id  VARCHAR(64),
    last_checked_at      TIMESTAMP WITH TIME ZONE,
    notes                TEXT
);
