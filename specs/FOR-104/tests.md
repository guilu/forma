# FOR-104 Test Plan

Strict TDD: failing tests first at each layer. Privacy assertions for photos are first-class.

## Scope

Goals + milestones, adherence read model, streak, achievements, muscle-worked mapping, progress-photo private storage. Each slice tested independently.

## Domain / Application Tests

- Goal progress derives from real source data (measurement/training/nutrition) where linked; 0/undefined when unlinked, not an error.
- Milestone completion state transitions correctly.
- Adherence: planned vs completed per category over a window; empty window → zeros, no division-by-zero; reuses `TrainingAdherenceRules` where applicable.
- Streak: consecutive-period rule; gap resets per the documented rule; longest vs current tracked.
- Achievement rules award from source data; already-earned → not duplicated (idempotent).
- Muscle map derives from `Exercise`/`MovementPattern`; exercise without mapping data omitted, not fabricated.

## API Tests

- `GET /goals` empty → 200 empty list; `POST /goals` then `GET` reflects it with milestones.
- `POST /goals` invalid metric/target → 400 `VALIDATION_ERROR`.
- `GET /progress/adherence?days=30` → per-category planned/completed/rate; `days` out of range → 400.
- `GET /progress/streak` and `/weekly-history` → shapes per `api.md`; empty history → zeros.
- `GET /progress/achievements` → earned + available.
- `GET /training/sessions/{id}/muscle-map` unknown id → 404.
- Progress photos: upload → private reference; `GET /photos/{id}` returns binary only to owner; access as non-owner → 403; content never appears in logs (assert).

## Edge Cases

- Goal with no linked data → progress 0/undefined.
- Adherence with zero planned items → zeros, not error.
- Streak across a gap day → reset per rule.
- Duplicate achievement → not re-awarded.
- Photo delete then get → 404.

## Fixtures

- Measurement/training/nutrition history to derive adherence, streaks, and goal progress.
- An `Exercise` with muscle-target data and one without, for the muscle-map null path.
- A small test image for photo upload/retrieval; assert stored reference is private (no public URL).
- H2-in-PostgreSQL-mode with Flyway migrations for persistence tests.
