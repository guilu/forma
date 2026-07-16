# FOR-132 Test Plan

Strict TDD: failing tests first (mapping/dedup → adapter with fixtures → application → API). Never call the live Withings API — use recorded Getmeas fixtures + mocked transport. Token/secret-leak assertions are first-class.

## Scope

Real Withings Getmeas → `BodyMeasurement` import: mapping, idempotent duplicate detection, sync outcome, error handling. No new endpoints; extends the FOR-126 sync.

## Mapping Tests (recorded fixtures)

- A Getmeas group with weight/fat%/muscle/hydration → a `BodyMeasurement` with `weightKg`, `bodyFatPercentage`, `muscleMassKg`, `waterPercentage`, `source=WITHINGS`, `measuredAt` from the group date, `bmi=null`.
- Partial group (weight only) → weight set, others null; no fabrication.
- Hydration mass without group weight → `waterPercentage` null (documented conversion rule).
- Unmodeled measure type (e.g. bone mass) → ignored, no crash.

## Dedup Tests

- First sync imports all groups; second sync of the same fixture imports 0, `duplicatesSkipped` = N — NO duplicate `BodyMeasurement`s created.
- Dedup keyed on Withings `grpid` in the integrations-side markers table (V16); `BodyMeasurement` carries no external id.

## Application Tests

- `sync` refreshes the token when expired (reuses FOR-131), then imports.
- Refresh failure → `NEEDS_REAUTH` outcome, nothing imported, no crash.
- Withings error / rate limit → readable `ERROR` outcome, connection not corrupted, no secret leak.
- Sync on DISCONNECTED provider → `NOT_CONNECTED` (unchanged from FOR-126).
- `lastSyncOutcome` records real `importedCount` + `duplicatesSkipped`; `lastSyncAt` updated.

## API Tests

- `POST /{provider}/sync` (connected, fixture) → 200 with real counts; subsequent `BodyMeasurement` list reflects imported rows.
- Second `POST /{provider}/sync` → importedCount 0, duplicatesSkipped N.
- **No token/secret/measure-secret in any response or log** (assert).

## Edge Cases

- Empty Getmeas response → importedCount 0, OK outcome.
- Large first-sync history → documented cap/paging behavior exercised if implemented.
- Timezone of `measuredAt` preserved.

## Fixtures

- Recorded Withings Getmeas JSON: a multi-type group, a partial group, an unmodeled-type group, and an empty response — never the live API.
- H2-in-PostgreSQL-mode with Flyway (through V16) for the markers table + BodyMeasurement persistence integration.
- A test OAuth token in the FOR-131 encrypted store (via its test helpers) so sync has a token to use.
