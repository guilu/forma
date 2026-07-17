-- Earned achievements ("logros", FOR-135, achievements slice of FOR-104).
--
-- Only EARNED achievements are persisted here; the catalog + rules themselves are in-code
-- (dev.diegobarrioh.forma.domain.AchievementCatalog, like FoodCatalog, FOR-30) — no catalog table
-- (spec FOR-135 Data Model Notes). Additive on top of V17 (ADR-003) — earlier migrations are
-- untouched.
--
-- The (owner_id, achievement_id) primary key IS the idempotency guarantee (spec FOR-135 hard
-- rule): a re-evaluated already-earned achievement can never be duplicated, and this holds even
-- under concurrent evaluation (two concurrent GETs racing to award the same rule), not just
-- application-level "check then insert" logic. Achievements are never revoked or re-awarded once
-- earned (spec FOR-135 Edge Cases: "the earned achievement stays earned" even if the source data
-- that earned it is later deleted) — there is deliberately no update/delete path, mirroring the
-- append-only shape of water_intake_entry (V14) / meal_log_entry (V13).
--
-- owner_id mirrors goal.owner_id/meal_log_entry.owner_id (V11/V13): account-scoped in shape even
-- though authorization is not enforced yet (ADR-002 single-user MVP).
CREATE TABLE earned_achievement (
    owner_id       VARCHAR(64) NOT NULL,
    achievement_id VARCHAR(64) NOT NULL,
    earned_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (owner_id, achievement_id)
);
