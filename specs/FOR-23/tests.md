# FOR-23 Test Plan

## Scope

Validate the seeded 16-week running plan: session counts, weekly structure, and
the gradual long-run progression.

## Domain Tests

- The plan contains exactly 16 weeks.
- Each week has exactly 3 planned running sessions (one easy, one
  quality/controlled, one long run).
- Long-run distance is monotonically non-decreasing across weeks and lands near
  ~4 km at the start and ~10 km by the end.

## Application Tests

- The plan can be produced/read deterministically (same output each run).
- If persisted, seeding is idempotent (re-running does not duplicate the plan).

## API Tests

N/A — no HTTP endpoint is created by this story (the plan is only made
retrievable for a later endpoint).

## UI Tests

N/A — no frontend in this story.

## Edge Cases

- Total session count is exactly 48 (16 × 3).
- No week with fewer/more than 3 sessions.
- Re-run seed/migration is a no-op (Flyway history already applied), if
  persisted.

## Fixtures

- The generated/seeded 16-week plan itself is the primary fixture; assertions
  run against its structure.
