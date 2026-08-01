-- Admin role + food category (FOR-190, slice 1 of the food/store catalog admin).
--
-- Additive on top of V34 (ADR-003). Two independent additions that ship together because the
-- admin screen needs both: a role to gate it, and the column its first tab edits.
--
-- 1) users.role
--
-- FORMA had no roles at all: FormaUserPrincipal handed every authenticated account a hardcoded
-- ROLE_USER. The column defaults to 'USER' so every existing account keeps exactly the authority it
-- had, and an admin is granted deliberately with an UPDATE (see below) rather than by any implicit
-- rule -- there is no "first user is admin" magic, which would make the first registration on a
-- fresh deployment a privilege escalation.
--
-- Deliberately a VARCHAR + CHECK rather than a join table: FORMA has two roles, not a permission
-- system, and a nullable-free single column cannot drift out of sync with itself. Revisit if roles
-- ever need to be composed.
ALTER TABLE users ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

-- 2) food_catalog.category
--
-- The one field the Macros sheet (docs/fitness_os.xlsm) carries that V25 did not transcribe.
-- NULLABLE on purpose: it is descriptive, not structural, and a food whose category nobody has
-- decided yet is a real state -- better than defaulting every unclassified food into some bucket it
-- may not belong to (FOR-134 "never fabricate").
ALTER TABLE food_catalog ADD COLUMN category VARCHAR(32);

-- Backfilled verbatim from the Macros sheet's own "Categoría" column, for the 23 rows V25 seeded
-- from that same sheet. Values are the sheet's Spanish terms, uppercased and accent-stripped for a
-- stable identifier -- the UI renders its own labels.
UPDATE food_catalog SET category = 'CARBOHIDRATO' WHERE id IN
  ('oats', 'rice', 'whole-wheat-pasta', 'potato', 'sweet-potato', 'whole-wheat-bread');
UPDATE food_catalog SET category = 'PROTEINA' WHERE id IN
  ('whey-protein', 'eggs', 'egg-whites', 'fresh-cheese', 'yogurt', 'chicken', 'turkey', 'tuna',
   'fish', 'salmon');
UPDATE food_catalog SET category = 'FRUTA' WHERE id IN ('banana', 'berries');
UPDATE food_catalog SET category = 'VERDURA' WHERE id IN ('vegetables', 'salad');
UPDATE food_catalog SET category = 'GRASA' WHERE id IN ('olive-oil', 'almonds-walnuts');
UPDATE food_catalog SET category = 'LACTEO' WHERE id IN ('skim-milk');

ALTER TABLE food_catalog ADD CONSTRAINT chk_food_catalog_category CHECK (
  category IS NULL
  OR category IN ('CARBOHIDRATO', 'PROTEINA', 'FRUTA', 'VERDURA', 'GRASA', 'LACTEO')
);
