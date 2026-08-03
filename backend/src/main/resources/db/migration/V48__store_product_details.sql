-- What the shop says about a product beyond its name and price.
--
-- Additive on top of V47 (ADR-003). Most of this already arrives on every crawl and is thrown away
-- at the door, which is the same thing that had happened to the shop's own aisle before V46:
-- ImportableProduct carries an `ean`, and price_instructions carries the package size as a number
-- and a unit, and none of it had a column to land in.
--
-- WHY NOT "weight". The source document asks for `weight`, and the data will not support that name:
-- Mercadona answers `unit_size` 5.0 with `size_format` "l" for a bottle of oil, and "kg", "g" or
-- "ud" for other things. Five litres is not five of anything weighable without knowing what is in
-- the bottle, so the honest pair is the amount and the unit the shop stated. Converting to grams is
-- a separate problem that needs a density nobody has recorded yet, and it is not solved by naming a
-- column as though it had been.
--
-- The free-text `package_size` stays. It is what a person reads on the shelf ("Garrafa 5 l") and it
-- is the only form available for a product typed by hand; these two columns are the same fact in a
-- form arithmetic can use, not a replacement.

-- The barcode: the only key here that means the same thing in every shop on earth, which is what
-- makes it worth storing years before anything reads it. Nullable, because plenty of listings have
-- none.
ALTER TABLE store_product ADD COLUMN ean VARCHAR(14);

ALTER TABLE store_product ADD COLUMN package_amount NUMERIC(9,3);

ALTER TABLE store_product ADD COLUMN package_unit VARCHAR(8);

-- Either both or neither: an amount with no unit is a number nobody can read, and a unit with no
-- amount says nothing at all.
ALTER TABLE store_product ADD CONSTRAINT chk_store_product_package CHECK (
  (package_amount IS NULL AND package_unit IS NULL)
  OR (package_amount IS NOT NULL AND package_unit IS NOT NULL AND package_amount > 0)
);

-- Whether the shop still lists it. `published` is in every crawled product and has been ignored
-- until now, so a product Mercadona pulled from sale looks exactly like one it still sells.
-- Defaults to TRUE: every row that predates this column was listed when it was written, and saying
-- "unknown" for all of them would be less true than saying "as far as anyone knew, yes".
ALTER TABLE store_product ADD COLUMN available BOOLEAN NOT NULL DEFAULT TRUE;

-- When we last heard any of this from the shop. Distinct from created_at, which says when somebody
-- added the row: a product imported in January and refreshed last week has two different dates and
-- only one of them tells you whether the price is worth trusting.
--
-- NULL for a product nobody ever imported. That is not a missing value — a row typed by hand has
-- never been checked against a shop and never will be, which is the same reason its external_id is
-- null (FOR-195).
ALTER TABLE store_product ADD COLUMN last_synced_at TIMESTAMP WITH TIME ZONE;

-- The brand. No shop we read publishes it as a field of its own — Mercadona spells it inside the
-- display name ("Aceite de oliva 0,4º Hacendado") — so imports will never fill this in, and pulling
-- it out of the name with rules would be guessing where the product stops and the brand starts
-- (FOR-134). It is here for whoever curates the catalog to write, like `notes`.
ALTER TABLE store_product ADD COLUMN brand VARCHAR(100);

-- food_catalog is the only table of this redesign with no updated_at, and it is the one whose
-- values everything else is derived from: an equivalence computes its grams from these numbers, so
-- when they last changed is a question somebody will ask. Backfilled from created_at, which is the
-- truthful answer for a row nobody has edited since.
ALTER TABLE food_catalog ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

UPDATE food_catalog SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE food_catalog ALTER COLUMN updated_at SET NOT NULL;

-- Same default as created_at, for the same reason: a row being written now was updated now, and
-- making every INSERT say so by hand is how one of them eventually forgets.
ALTER TABLE food_catalog ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
