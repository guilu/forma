-- Water-intake entries (FOR-130, hydration slice of FOR-102).
--
-- One row per logged entry (per-entry-rows aggregate shape, mirroring meal_log_entry, V13) — the
-- per-day total (HydrationLog#totalMl) is always derived on read by summing this table's rows,
-- never stored separately, so it can never drift out of sync with the entries (spec FOR-130 Open
-- Questions). Additive on top of V13 (ADR-003) — earlier migrations are untouched.
--
-- owner_id exists even though authorization is not enforced yet (ADR-002 single-user MVP),
-- mirroring meal_log_entry.owner_id/user_profile.owner_id: every row is account-scoped in shape,
-- ready for a real account id once authentication lands. Append-only for this slice (spec FOR-130
-- Open Questions: "Default append-only unless trivial") — no update/delete path exists yet.
CREATE TABLE water_intake_entry (
    id        UUID PRIMARY KEY,
    owner_id  VARCHAR(64)   NOT NULL,
    log_date  DATE          NOT NULL,
    volume_ml NUMERIC(8, 1) NOT NULL,
    logged_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_water_intake_entry_owner_date ON water_intake_entry (owner_id, log_date);
