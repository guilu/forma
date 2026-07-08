-- Shopping: weekly shopping lists (FOR-39).
--
-- Persists a weekly ShoppingList (FOR-37) and its items so the checklist UI can
-- read them and persist the checked state. Additive on top of V4 (ADR-003).
-- shopping_list_id is a real containment FK; product_id is a soft reference to
-- shopping_products (consistent with the soft-link philosophy), so a product can
-- be edited without cascading here.
CREATE TABLE shopping_lists (
    id               UUID PRIMARY KEY,
    week_start_date  DATE NOT NULL,
    status           VARCHAR(16) NOT NULL,
    notes            TEXT
);

CREATE TABLE shopping_list_items (
    id                 UUID PRIMARY KEY,
    shopping_list_id   UUID NOT NULL REFERENCES shopping_lists (id),
    product_id         VARCHAR(64) NOT NULL,
    quantity           INTEGER NOT NULL,
    estimated_cost_eur NUMERIC(8, 2) NOT NULL,
    checked            BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_shopping_list_items_list ON shopping_list_items (shopping_list_id);

-- Starter data: a few products (linked to nutrition foods) plus one active weekly
-- list, so the checklist UI has content on a fresh install. Prices are editable
-- estimates (FOR-36); the user can change everything later.
INSERT INTO shopping_products (id, name, estimated_price_eur, linked_food_item_id)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'Avena 1 kg', 1.95, 'oats'),
  ('22222222-2222-2222-2222-222222222222', 'Pollo (pechuga) 1 kg', 5.50, 'chicken'),
  ('33333333-3333-3333-3333-333333333333', 'Arroz 1 kg', 1.20, 'rice'),
  ('44444444-4444-4444-4444-444444444444', 'Plátano (manojo)', 1.80, 'banana');

INSERT INTO shopping_lists (id, week_start_date, status, notes)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', DATE '2026-07-06', 'ACTIVE', NULL);

INSERT INTO shopping_list_items
  (id, shopping_list_id, product_id, quantity, estimated_cost_eur, checked)
VALUES
  ('b0000001-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
   '11111111-1111-1111-1111-111111111111', 2, 3.90, FALSE),
  ('b0000002-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
   '22222222-2222-2222-2222-222222222222', 3, 16.50, FALSE),
  ('b0000003-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
   '33333333-3333-3333-3333-333333333333', 2, 2.40, TRUE),
  ('b0000004-0000-0000-0000-000000000004', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
   '44444444-4444-4444-4444-444444444444', 1, 1.80, FALSE);
