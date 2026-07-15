-- Insights: insight-history persistence + week-over-week deltas support (FOR-110).
--
-- Persists each generated WeeklyInsights (FOR-45) keyed by its period (the FOR-40 check-in's
-- week_start_date), so a history endpoint can list past weeks and week-over-week deltas can be
-- computed against the prior persisted period's checkIn snapshot. Additive migration on top of V9
-- (ADR-003) — earlier migrations are untouched.
--
-- insight_history is period-keyed (week_start_date is the primary key) rather than a generated id,
-- mirroring FOR-107's single-natural-key user_profile table: re-running generation for a week that
-- was already stored upserts in place instead of appending a duplicate row (spec FOR-110 Edge
-- Cases: "repeated generation within the same period" -> overwrite, the chosen/documented
-- behavior). The full FOR-40 checkIn snapshot is stored (not just the recommendation text) because
-- delta computation needs the raw signals (spec FOR-110 Data Model Notes).
--
-- insight_history_recommendation is a child table (FK to insight_history, containment pattern like
-- shopping_list_items -> shopping_lists) holding both the main and secondary recommendations for a
-- period. sort_order 0 is always the main recommendation (is_main = TRUE); sort_order >= 1 are the
-- secondaries in their original priority order (FOR-45 main/secondary split). The composite primary
-- key (week_start_date, sort_order) avoids a separate generated id since sort_order is already
-- unique per period.
--
-- Numeric column widths mirror the existing body_measurements types (V2) for the same units
-- (weight_kg NUMERIC(6,3), body_fat_percentage NUMERIC(5,2)); lean mass reuses the weight
-- precision. category/severity VARCHAR(16) matches the existing enum-backed columns elsewhere
-- (e.g. user_profile.activity_level).
CREATE TABLE insight_history (
    week_start_date              DATE PRIMARY KEY,
    latest_weight_kg              NUMERIC(6, 3),
    latest_body_fat_percentage    NUMERIC(5, 2),
    latest_lean_mass_kg           NUMERIC(6, 3),
    planned_running_sessions      INTEGER NOT NULL,
    completed_running_sessions    INTEGER NOT NULL,
    planned_strength_sessions     INTEGER NOT NULL,
    completed_strength_sessions   INTEGER NOT NULL,
    notes                         TEXT,
    generated_at                  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE insight_history_recommendation (
    week_start_date    DATE NOT NULL REFERENCES insight_history (week_start_date),
    sort_order          INTEGER NOT NULL,
    is_main             BOOLEAN NOT NULL,
    category            VARCHAR(16) NOT NULL,
    severity            VARCHAR(16) NOT NULL,
    message             TEXT NOT NULL,
    reason              TEXT NOT NULL,
    related_metric      VARCHAR(64),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (week_start_date, sort_order)
);
