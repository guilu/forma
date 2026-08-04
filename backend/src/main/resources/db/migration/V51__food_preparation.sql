-- What state a food's numbers describe.
--
-- Additive on top of V50 (ADR-003). food_catalog has held macros since V25 with no word about
-- whether they were measured before or after cooking, and the two differ enough to make the numbers
-- unusable without it: rice is 360 kcal/100 g dry and about 130 cooked. Nobody could tell whether to
-- weigh it dry or cooked.
--
-- THIS IS NOT HYPOTHETICAL. The source document for this whole redesign states
--
--     100 g arroz = 250 g patata
--
-- and computed from this catalog the answer is 465 g. The gap is exactly this column: the document
-- was thinking of cooked rice, food_catalog holds it dry, and nothing recorded the difference. That
-- discrepancy is what V47 pointed at when it refused to store the grams, and this is the hole it
-- pointed into.
--
-- THREE STATES, AND ABSENT IS NOT ONE OF THEM
--
-- A food goes into the kitchen, comes out of it, or never passes through. That is a small closed
-- idea, so this is a CHECK rather than a table — unlike the food groups of V43, no amount of
-- curating produces a fourth. "Congelado" is not a fourth either: it is a tag (V50), because it
-- describes how something was kept rather than what its numbers measure.
--
-- NULL means nobody has decided, which is a different thing from TAL_CUAL. The question not
-- applying to olive oil is an answer; not having been asked about chicken is not.

ALTER TABLE food_catalog ADD COLUMN preparation VARCHAR(16);

ALTER TABLE food_catalog ADD CONSTRAINT chk_food_catalog_preparation CHECK (
  preparation IS NULL
  OR preparation IN ('CRUDO', 'COCINADO', 'TAL_CUAL')
);

-- NO BACKFILL, and that is the whole discipline of this redesign applied to itself.
--
-- Saying the chicken is raw is a claim about that food, and only two of the twenty-three seeded
-- foods can be settled without guessing: rice and dry pasta sit at roughly 2.7x their cooked
-- selves, which leaves no room for doubt. For chicken, potato, hake, salmon and eggs the difference
-- between raw and cooked is 10-20 %, well inside the noise of a spreadsheet somebody typed by hand
-- — and inferring from external reference values I would be asserting from memory is precisely the
-- move that produced the 250-versus-465 error in the first place (FOR-134).
--
-- The column starts empty and a person fills it, the same way food_tag does.
