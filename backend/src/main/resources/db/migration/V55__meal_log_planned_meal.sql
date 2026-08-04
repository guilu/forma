-- What was eaten, pointed at what was planned.
--
-- Additive on top of V54 (ADR-003). The last piece of section 10 of
-- docs/FORMA_Spec_Modelo_Datos_Plan_Alimentacion.md.
--
-- NO SECOND LOG TABLE, though the document asks for nutrition_meal_logs and nutrition_meal_log_items.
-- meal_log_entry (V13/V17) is already a dated, append-only record of what somebody consumed, with
-- its macros snapshotted at the moment of logging. Building the document's pair beside it would give
-- the app two answers to "what did I eat on tuesday" and no rule about which one the dashboard
-- reads. ADR-011 reached the same conclusion on its own: "meal completion — chosen: evolve
-- meal_log_entry, no new table. Rejected alternative: a dedicated meal_completion table — it would
-- duplicate the existing dated log and split meal history across two tables."
--
-- ONE COLUMN, NOT TWO. ADR-011 proposes plan_id beside nutrition_plan_meal_id. The plan is reachable
-- from the meal through its day, so storing it as well would be the same fact in two places, free to
-- disagree the first time a meal is moved between plans.
--
-- NO STATUS COLUMN, though the document lists PENDING / COMPLETED / PARTIALLY_COMPLETED / SKIPPED /
-- REPLACED. Every one of those is a question this table can already answer:
--
--   PENDING    a planned meal for today with no entry pointing at it, and today is not over
--   COMPLETED  a planned meal with entries pointing at it
--   SKIPPED    a planned meal for a past day with no entry pointing at it
--   REPLACED   entries for that day whose food is not the planned one
--
-- Storing it would freeze an answer that changes by itself as the day goes on and as things get
-- logged, and would need somebody to remember to update it. This is the same rule V44, V47, V52 and
-- V53 follow: what can be computed is not stored.
--
-- NULL IS THE ORDINARY CASE and means "I ate this, and no plan said to". Most entries are that: a
-- free entry, an unplanned snack, or anything logged by an account with no plan at all.

ALTER TABLE meal_log_entry ADD COLUMN nutrition_plan_meal_id UUID;

-- ON DELETE SET NULL, and it is a statement about what these two tables are. A log entry is HISTORY:
-- it says somebody ate 60 g of oats on the fourth of August and that stays true forever. A plan is an
-- INTENTION, and intentions get thrown away. Deleting the plan must not delete the record of what was
-- eaten under it; it only makes the entry an unplanned one, which is exactly what it has become.
--
-- The alternative — having the plan's own delete null these out first — would put the rule in a
-- repository that has no other reason to know this table exists, and would leave it unenforced for
-- anybody deleting rows any other way.
ALTER TABLE meal_log_entry
  ADD CONSTRAINT fk_meal_log_entry_planned_meal
  FOREIGN KEY (nutrition_plan_meal_id) REFERENCES nutrition_plan_meal (id) ON DELETE SET NULL;

-- For the read that matters: given a day, which of its planned meals have been logged.
CREATE INDEX ix_meal_log_entry_planned_meal ON meal_log_entry (nutrition_plan_meal_id);
