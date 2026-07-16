# FOR-132 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-132
Epic: FOR-96 UI Backend Enablers — Foundations
Part of: FOR-103 [STUB] Integrations backend (slice 3 of 3). Builds on FOR-126 + FOR-131.

## Summary

Make a manual sync actually import Withings body-composition measures into
`BodyMeasurement`, using the encrypted OAuth tokens from FOR-131. Idempotent, with
duplicate detection and provider rate-limit respect. Replaces the FOR-126 stub sync
outcome with a real import. This completes the integrations vertical — the user's
Withings scale data flows into FORMA.

## Repository baseline

- `BodyMeasurement` (FOR-15/FOR-100): `measuredAt`, `source`, `weightKg`, `bodyFatPercentage`, `bmi`, `muscleMassKg`, `waterPercentage`, `notes`. Framework-free; provider-clean by design (ADR-004).
- `MeasurementSource` currently only `MANUAL` — its own doc anticipates "an external Withings import in a later story".
- `BodyMeasurementRepository`: only `save(measurement)` + `list()` — no dedup query.
- FOR-131 (merged): Withings OAuth adapter, encrypted token store, `refreshTokenIfNeeded`. FOR-126: `IntegrationService.sync` (currently the stub), connection status.

## User/System Flow

1. User taps manual sync → `POST /api/v1/integrations/withings/sync`.
2. Backend ensures a valid access token (refresh if expired — FOR-131); if refresh fails → NEEDS_REAUTH, readable outcome.
3. Backend calls Withings *Measure — Getmeas* (incremental via `lastupdate` where possible), maps each measure group to a normalized `BodyMeasurement` at the adapter boundary.
4. Groups whose Withings `grpid` was already imported are skipped (duplicate detection).
5. New measurements are saved via `BodyMeasurementRepository`; `lastSyncOutcome` records real `importedCount` + `duplicatesSkipped`; `lastSyncAt` updated.

## Functional Requirements

- Extend `MeasurementSource` with `WITHINGS` (no migration — `body_measurements.source` is VARCHAR).
- Call Withings Getmeas with the refreshed token; map provider payload → `BodyMeasurement` at the adapter boundary (ADR-004), never leaking Withings shapes into the Body domain.
- Measure-type mapping: weight (type 1) → `weightKg`; fat ratio % (type 6) → `bodyFatPercentage`; muscle mass (type 76) → `muscleMassKg`; hydration (type 77) → `waterPercentage` (Withings gives hydration as a mass; convert to % of weight where both are present, else null — document); BMI not in Getmeas → null. `measuredAt` from the group's date; `source = WITHINGS`.
- **Idempotent duplicate detection**: persist imported Withings `grpid`s in an integrations-side table (new migration **V16**); skip already-imported groups on re-sync. Do NOT add an external id to `BodyMeasurement` (ADR-004) — markers live in the Integrations adapter.
- Wire the real import into `IntegrationService.sync` (replace the stub); real `lastSyncOutcome`.
- Respect Withings rate limits; prefer incremental sync (`lastupdate`).

## Non-Functional Requirements

- **Security**: never log tokens/secrets; sync failures user-readable without leaking secrets (ADR-004, ADR-008). Application port stays token-free.
- **Idempotency + duplicate detection are mandatory** (ADR-004).
- **Isolation**: Withings payload mapping stays in the adapter.
- Owner-scoped per ADR-002.

## Data Model Notes

- New table (V16): imported Withings measure-group markers keyed by `(owner_id, provider, grpid)` (+ imported-at). Small; enables idempotent skip.
- `BodyMeasurement` unchanged in shape; only `MeasurementSource` gains `WITHINGS`.
- No provider id/token on the Body side.

## Edge Cases

- Re-sync with all groups already imported → importedCount 0, duplicatesSkipped = N, no duplicates created.
- Partial group (weight only, no fat/muscle) → import weight, leave the rest null; never fabricate.
- Hydration mass but no weight in the group → waterPercentage null (can't convert), documented.
- Withings rate limit / 5xx / unreachable → readable failure outcome, connection not corrupted, no secret leak, no crash.
- Token expired → refreshed first; refresh failure → NEEDS_REAUTH.
- Sync on a DISCONNECTED provider → the FOR-126 NOT_CONNECTED outcome (unchanged).
- Withings measure type FORMA doesn't model (e.g. bone mass 88) → skip, no crash.

## Open Questions

- Hydration (type 77) unit handling: Withings reports it as a mass (kg); converting to `waterPercentage` needs the group weight — document the conversion and the null-when-unavailable rule. (Alternatively expose only when Withings provides a percentage.)
- Incremental sync window: first sync (no `lastupdate`) may return a large history — document any cap/paging; Getmeas paging via `more`/`offset` if needed.
- Whether to store the raw Withings measured-at as the BodyMeasurement `measuredAt` (yes) and timezone handling.
