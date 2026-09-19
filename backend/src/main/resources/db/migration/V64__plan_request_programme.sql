-- The twelve-week programme is three four-week blocks, and each block is its own plan_request
-- (ADR-015 decision 14, amendment 2026-09-19). Until this decision was written down, nothing in this
-- repository said whether FORMA generates a plan once for twelve weeks or block by block -- the only
-- trace was a schema-foresight comment in docs/FORMA_Spec_Modelo_Datos_Plan_Alimentacion.md, keeping
-- nutrition_plan_day.week_number around "para soportar planes de cuatro, ocho o doce semanas" (V53).
-- Foresight that a column would be needed is not the same as deciding what FORMA generates, and this
-- migration is that decision, recorded rather than merely made possible.
--
-- FORMA generates block by block, on purpose: later blocks adapt to how the user actually did on the
-- one before, which is how a human coach's follow-up already works. Generating 84 days once, against
-- a guess about weeks 5-12 that has not happened yet, would be worse than what this table already
-- refuses to do for a single day -- store a number nothing has confirmed.
--
-- ONE plan_request PER BLOCK, NOT ONE PER PROGRAMME -- NO NEW TABLE
--
-- No programme table is added here, and none is planned by this decision. The programme is the
-- succession of three plan_request rows, each with its own capture, its own outbound message to the
-- agent, its own section-11 tolerance audit (decision 9) and its own activation -- the existing
-- one-open-request-per-user (decision 4) and one-active-plan-per-user (nutrition_plan.active_marker,
-- V53) invariants, applied three times in sequence rather than bent to cover a bigger unit. A
-- programme table today would duplicate what those two already enforce and would need its own status
-- lifecycle to stay in sync with three children it does not itself own -- the same "two doors into one
-- rule" failure decision 8 already refuses for a different endpoint.
--
-- block_length_weeks IS NOT A FREE PARAMETER, AND THE CHECK IS WRITTEN AS RIGID AS THE DECISION
--
-- The decision fixes the shape at three blocks of exactly four weeks; it does not propose a
-- configurable block length, so CHECK (block_length_weeks = 4) is exact rather than merely positive.
-- This mirrors how meals_per_day and training_days_per_week (V63) already encode bean-validated
-- ranges as CHECKs rather than leaving them to application code alone. A future decision that varies
-- block length needs its own migration to loosen this CHECK -- that is the correct order: relax a
-- constraint once the decision that justifies it exists, not ahead of it on a guess.
--
-- block_number DEFAULTS TO 1 BECAUSE NOTHING YET DECIDES BLOCK 2 OR 3
--
-- Every plan_request PlanRequestService writes today is somebody's first request in a fresh
-- open-request slot (decision 4 forbids a second open one regardless of programme position), and
-- there is no successor-block logic anywhere in this codebase yet -- verified: no reference to a
-- previous block or a programme id in PlanRequestService. DEFAULT 1 is therefore not a guess dressed
-- up as a default; it is what every row this slice can produce actually is. Deciding how a later
-- request learns it is block 2 or 3 of an existing programme (from the account's history? from an
-- explicit successor endpoint?) is exactly the "regenerating the next block" work this amendment
-- defers.
--
-- next_review_date IS NULLABLE, AND NOTHING WRITES A REAL VALUE YET
--
-- A review, at this stage, is a date and nothing else (ADR-015 decision 14): no alert, no measurement
-- capture, no regeneration reads or writes this column in this slice. PlanRequestService leaves it
-- NULL, the same way it already leaves catalog_version and request_payload NULL -- all three describe
-- something that has not happened yet (dispatch, in those two cases) and all three stay NULL until the
-- slice that owns that moment writes them. The natural value -- two weeks after the block's own start
-- date, the midpoint of a four-week block and the only review point that falls strictly inside one --
-- needs a start date to compute from, and capture does not fix one yet: plan.startDate stays optional
-- and unset at this stage (docs/FORMA_Contrato_Agente_Plan.md). No CHECK constrains it for the same
-- reason nothing constrains a column with no writer yet: constraining a value before its only writer
-- exists risks locking in a guess (the same posture ADR-015 already takes for plan_request.
-- failure_code, V63). Whichever later slice adds alerting or regeneration has to decide, and record,
-- what this date is measured from and what becomes of it once its block ends.
ALTER TABLE plan_request ADD COLUMN block_length_weeks INTEGER NOT NULL DEFAULT 4;
ALTER TABLE plan_request ADD COLUMN block_number INTEGER NOT NULL DEFAULT 1;
ALTER TABLE plan_request ADD COLUMN next_review_date DATE;

ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_block_length CHECK (
  block_length_weeks = 4
);

ALTER TABLE plan_request ADD CONSTRAINT chk_plan_request_block_number CHECK (
  block_number BETWEEN 1 AND 3
);
