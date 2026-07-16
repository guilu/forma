# FOR-132 AI Context

## Story

FOR-132 — Withings measures sync into `BodyMeasurement`. Slice 3 (of 3) of FOR-103 [STUB] Integrations backend. Completes real Withings scale sync. Builds on FOR-126 (connection) + FOR-131 (OAuth + encrypted tokens).

## Intent

Make `POST /api/v1/integrations/withings/sync` actually import the user's Withings body-composition measures (weight, body fat, muscle, water) into `BodyMeasurement`, using the FOR-131 encrypted tokens. Idempotent with duplicate detection. Success = the user's scale data appears in FORMA's body-composition views.

## Relevant Documents

- `specs/FOR-103/`, `specs/FOR-126/`, `specs/FOR-131/` — the integrations vertical this completes.
- `AGENTS.md` — never log/commit tokens; provider details stay in adapters.
- `docs/adr/ADR-004-integrations.md` — normalize at the adapter boundary; **idempotent sync + mandatory duplicate detection**; respect rate limits; failures observable without leaking secrets.
- `docs/adr/ADR-002-authentication.md`, `ADR-003-persistence.md`, `ADR-008-observability.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-132

## Domain / Repo Notes

- `BodyMeasurement` fields: `measuredAt`, `source`, `weightKg`, `bodyFatPercentage`, `bmi`, `muscleMassKg`, `waterPercentage`, `notes`. Provider-clean (ADR-004) — do NOT add an external id.
- `MeasurementSource` = only `MANUAL` today → **add `WITHINGS`** (enum doc anticipates it; no migration, `source` column is VARCHAR).
- `BodyMeasurementRepository` = `save` + `list` only (no dedup query) → dedup via a NEW integrations-side markers table (grpid), NOT via the Body domain.
- FOR-131 provides: Withings OAuth adapter, encrypted token store, `refreshTokenIfNeeded`, `ProviderOAuthGateway`/`WithingsHttpTransport` seam — reuse them; add a Getmeas call on the same transport seam.
- FOR-126 `IntegrationService.sync` currently returns the stub outcome — replace with the real import.

## Architectural Constraints

- Withings Getmeas call + payload→`BodyMeasurement` mapping live in the Withings adapter; the application port stays token-free.
- Idempotent + duplicate detection mandatory: imported `grpid`s in a new markers table (migration **V16**, head is V15).
- Owner-scoped (ADR-002); never log tokens or measure payloads containing secrets; sync failures user-readable (ADR-008).
- Tests use recorded Getmeas fixtures + mocked transport — never the live API.
- Measure-type mapping: weight=1→weightKg, fat%=6→bodyFatPercentage, muscle=76→muscleMassKg, hydration=77→waterPercentage (mass→% needs group weight; null when not derivable), bmi=null (not in Getmeas). Skip unmodeled types.

## Common Pitfalls

- Adding a Withings/external id to `BodyMeasurement` — keep it provider-clean; dedup lives in the integrations markers table.
- Non-idempotent sync creating duplicate measurements on re-sync.
- Fabricating fat/muscle/water when a group only has weight — leave null.
- Converting hydration mass to % without the group weight — null instead.
- Leaking a token / measure secret into a log or response.
- Calling the live Withings API in tests.
- Building scheduled/automatic sync or Google Fit/Apple Health — out of scope.

## Suggested Implementation Order

1. Add `WITHINGS` to `MeasurementSource` (+ test).
2. Withings Getmeas call on the FOR-131 transport seam + payload→`BodyMeasurement` mapping (recorded fixtures, mapping tests incl. partial/unmodeled/hydration cases).
3. Imported-grpid markers store + `V16` migration (+ dedup round-trip test).
4. Wire into `IntegrationService.sync`: refresh token → Getmeas → map → skip dup grpids → save new via `BodyMeasurementRepository` → real `lastSyncOutcome` (importedCount + duplicatesSkipped). Handle refresh failure (NEEDS_REAUTH), provider error/rate limit (readable ERROR). API tests + leak assertions.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: sync imports mapped measures with `source=WITHINGS`; re-sync creates no duplicates (grpid dedup); outcome reports real importedCount + duplicatesSkipped; refresh-failure → NEEDS_REAUTH; provider error → readable ERROR, no secret leak; `BodyMeasurement` has no external id; no live API in tests; migration V16 unique. Live Withings E2E is out of automated scope (needs real credentials + a real scale) — document.
