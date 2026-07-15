-- Goals & milestones (FOR-125, first slice of FOR-104).
--
-- Two tables, mirroring the shopping_lists/shopping_list_items shape (V5, FOR-39) rather than
-- the single-row user_profile shape (V8, FOR-107): a goal is a per-owner *list* of aggregates
-- (unlike the one-row-per-owner profile), each with its own ordered child rows. Additive on top
-- of V10 (ADR-003) — earlier migrations are untouched.
--
-- owner_id exists even though authorization is not enforced yet (ADR-002 single-user MVP),
-- mirroring user_profile.owner_id: every row is account-scoped in shape, ready for a real
-- account id once authentication lands.
--
-- Progress is intentionally NOT a column here (spec FOR-125 Data Model Notes: "Progress is a
-- read-model concern (derived), not a stored column") — it is recomputed on every read from
-- BodyMeasurement/WeeklyBodySummary, so it can never drift out of sync with the source data.
CREATE TABLE goal (
    id       UUID PRIMARY KEY,
    owner_id VARCHAR(64)  NOT NULL,
    title    VARCHAR(200) NOT NULL,
    metric   VARCHAR(32)  NOT NULL,
    target   NUMERIC(8, 2) NOT NULL,
    due_date DATE,
    status   VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_goal_owner ON goal (owner_id);

-- goal_id is a real containment FK (a milestone never outlives its goal), like
-- shopping_list_items.shopping_list_id (V5). "position" preserves the milestone order the API
-- always returns (spec FOR-125 tests.md: "Milestones preserved in order").
CREATE TABLE goal_milestone (
    id        UUID PRIMARY KEY,
    goal_id   UUID NOT NULL REFERENCES goal (id),
    title     VARCHAR(200) NOT NULL,
    target    NUMERIC(8, 2) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    position  INTEGER NOT NULL
);

CREATE INDEX idx_goal_milestone_goal ON goal_milestone (goal_id);
