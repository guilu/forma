# FOR-135 Test Plan

Strict TDD: failing tests first (rule evaluation → idempotent award → persistence → API), then implement.

## Scope

The achievement catalog rules, evaluation, idempotent award + persistence, and the read endpoint. Streaks/muscle-map/photos out of scope.

## Rule / Domain Tests

- Each catalog rule fires exactly when its deterministic condition over the (fixture) data is met, and not before.
- Count-based rules (e.g. 10 meals) fire at the threshold, not below; all-time vs window per the documented decision.
- No rule depends on per-date training completion history (assert the catalog contains none — guards the documented gap).

## Application Tests

- Evaluation awards a newly-met rule (persists it with earnedAt).
- Re-evaluation of an already-earned achievement is a no-op (no duplicate, earnedAt unchanged) — idempotency.
- Response separates earned (with earnedAt) from available.
- Rules reuse existing repositories (BodyMeasurement, Goals, MealLog, hydration); no duplicated query logic.
- Owner-scoping: only the owner's data counts / only the owner's earned achievements return.

## Persistence Tests

- Round-trip an earned achievement through the JDBC adapter against H2-in-PostgreSQL-mode with Flyway (V18).
- PK (owner_id, achievement_id) prevents a duplicate insert (idempotent award).
- Empty DB → nothing earned, no error.

## API Tests

- `GET /progress/achievements` on empty data → `earned: []`, full `available`, never 404.
- After the data that satisfies a rule exists → that achievement appears in `earned` with earnedAt; a second GET does not duplicate it.
- Response shape matches `api.md`.

## Edge Cases

- Source data deleted after earning → achievement stays earned (not revoked), per the documented rule.
- Threshold boundary (exactly N) fires.

## Fixtures

- Data sets that satisfy a couple of rules and leave others unmet, to exercise earned/available split.
- H2-in-PostgreSQL-mode with Flyway (V18) for persistence/API integration, matching FOR-127/129 style.
