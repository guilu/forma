# FOR-15: Create body measurement domain model

Jira: https://dbhlab.atlassian.net/browse/FOR-15
Epic: FOR-2 Body Composition

## Summary

Create the `BodyMeasurement` domain model: a framework-free type representing one
measurement event, with weight/body-fat-derived mass values calculated
consistently. `source` supports `MANUAL` now and must not block later external
imports (e.g. Withings, FOR-2 later stories). This story is domain-only — no
persistence, API or UI.

## User/System Flow

This story has no direct user-facing flow. It defines the domain type consumed
by later stories:

1. FOR-16 maps `BodyMeasurement` to a persisted row.
2. FOR-17 exposes it through the API (request/response DTOs, not this type
   directly).
3. A later import story (Withings) will produce `BodyMeasurement` instances
   with `source != MANUAL` without changing this model.

## Functional Requirements

- Add `BodyMeasurement` under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/` (see existing
  `domain/package-info.java`).
- Fields: `measuredAt`, `source`, `weightKg`, `bodyFatPercentage`, `bmi`,
  `fatMassKg`, `leanMassKg`, `notes`.
- `source` distinguishes `MANUAL` from future external sources. Model it so a
  new source can be added later without breaking this contract (docs/domain-model.md).
- Derived values, per docs/domain-model.md:
  - `fatMassKg = weightKg * bodyFatPercentage / 100`
  - `leanMassKg = weightKg - fatMassKg`
- Do not add provider-specific fields (tokens, external ids, sync metadata) to
  this model — those stay in Integrations adapters (docs/architecture-overview.md).
- The domain package must not depend on Spring, JPA/JDBC or HTTP types
  (ADR-001).

## Non-Functional Requirements

- Performance: calculation is in-memory and O(1); no external calls.
- Security: no provider tokens or credentials pass through this type.
- Observability: none required at this layer — do not log measurement values
  here (AGENTS.md forbidden shortcuts: no logging sensitive health data).

## Data Model Notes

docs/domain-model.md lists `BodyMeasurement` with additional optional fields
(`muscleMassKg`, `waterPercentage`) beyond this story's scope. Do not add them
now — the Jira summary only lists `measuredAt, source, weightKg,
bodyFatPercentage, bmi, fatMassKg, leanMassKg, notes`. Add the remaining
optional fields only when a future story explicitly asks for them.

## Edge Cases

- `bodyFatPercentage` missing/null: `fatMassKg`/`leanMassKg` cannot be derived.
  Define and test explicit behavior (e.g. both derived values absent) instead
  of silently returning `0`.
- `bodyFatPercentage` at boundary values (`0`, `100`).
- `weightKg` zero or negative: decide whether the domain type rejects this at
  construction or leaves validation to the application/API layer (see Open
  Questions).

## Open Questions

- Should `BodyMeasurement` validate/reject invalid values (e.g. non-positive
  weight) at construction, or stay a plain data holder and leave validation to
  the API layer (FOR-17)? Not specified by Jira; recommend construction-time
  validation for internal consistency, with API-level validation in FOR-17
  providing user-facing error messages either way.
- Should `source` be a Java `enum` with only `MANUAL` for now, or a value type
  designed to hold future external sources? No ADR mandates either; an `enum`
  extended later (non-breaking, since the type is internal) is the simplest
  option consistent with docs/domain-model.md's `MANUAL | WITHINGS` note.
