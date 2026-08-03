-- What a food is mostly made of, stored rather than recomputed on every read.
--
-- Additive on top of V43 (ADR-003). A food already carries its macros, so this column holds nothing
-- the numbers do not, and could be a query. It is stored because the answer is allowed to disagree
-- with the arithmetic: "yogur proteína" is sold and eaten as a protein, and whoever curates the
-- catalog gets to say so even when the label's carbohydrates edge it out. A computed column would
-- take that away.
--
-- Distinct from food_group_id, which V43 made a table. A group answers "what shelf is this on" and
-- is an editorial decision; this answers "what is it made of" and starts as arithmetic. Skimmed milk
-- is a LACTEO whose calories are mostly carbohydrate, and a planner swapping foods by macro needs
-- the second answer.
--
-- Closed set, and it stays closed: there are three macronutrients because there are three, not
-- because somebody drew the line there. Hence a CHECK here and a table in V43.

ALTER TABLE food_catalog ADD COLUMN primary_macro VARCHAR(16);

ALTER TABLE food_catalog ADD CONSTRAINT chk_food_catalog_primary_macro CHECK (
  primary_macro IS NULL
  OR primary_macro IN ('PROTEIN', 'CARBS', 'FAT')
);

-- Backfilled by calories, not by grams: the Atwater factors (4/4/9) are what make this worth
-- computing at all. A whole egg carries more protein than fat by weight (13 g vs 10 g) and is still
-- mostly fat on the plate (52 kcal vs 90 kcal). Domain PrimaryMacro.dominantOf applies exactly this
-- rule, and FoodPrimaryMacroMigrationTest pins the two against each other.
--
-- Strictly greater than BOTH others in all three statements, so a tie leaves the column null. Ties
-- and all-zero foods keep the same meaning every nullable column in this catalog has: nobody has
-- decided (FOR-134, never fabricated). Somebody who knows the food still can.
--
-- Some results read oddly and are right: eggs come out FAT, and skimmed milk comes out CARBS
-- because lactose outweighs its protein. That is the point of computing it.
UPDATE food_catalog SET primary_macro = 'PROTEIN'
 WHERE protein_g * 4 > carbs_g * 4 AND protein_g * 4 > fat_g * 9;

UPDATE food_catalog SET primary_macro = 'CARBS'
 WHERE carbs_g * 4 > protein_g * 4 AND carbs_g * 4 > fat_g * 9;

UPDATE food_catalog SET primary_macro = 'FAT'
 WHERE fat_g * 9 > protein_g * 4 AND fat_g * 9 > carbs_g * 4;
