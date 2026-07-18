# FOR-149 Test Plan

Strict TDD: failing tests first (domain targets → migration/persistence → seed → API), then implement.

## Scope

The new personal-target fields on the profile domain, their persistence, Diego's seed, and the
extended profile read model. Measurements are out of scope (stay empty).

## Domain Tests

- Target value object accepts the *Perfil* values; rejects negative targets; range min ≤ max.
- Nullable targets → an unseeded/partial profile is still a valid aggregate.
- `UserProfile.defaults(ownerId)` still yields empty targets (no fabrication).

## Application / Persistence Tests

- New target columns round-trip through the persistence adapter (write then read equal).
- Additive migration applies on top of V19 without touching earlier migrations; one column per statement.
- Seed inserts exactly one row under the fixed OWNER_ID with the *Perfil* values.

## API Tests

- `GET /api/v1/profile` returns Diego's seeded targets matching the *Perfil* sheet.
- Unseeded owner → targets null, 200 (not 404).
- Response shape matches `api.md` / existing profile DTO conventions.

## Edge Cases

- Partial targets (some null) read back consistently.
- Baseline weight/fat present on profile while `body_measurement` table is empty (SEGUIMIENTO not seeded).

## Fixtures

- Diego's *Perfil* row as the seed fixture (base kcal 2300; fat 12–13 %; weight 73–75 kg; P160/F70/C260; height 1.80 m; name Diego).
- H2-in-PostgreSQL-mode with Flyway running all migrations including the new `V<N>`, matching the repo's existing persistence-test style.
