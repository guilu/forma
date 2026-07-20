# FOR-143 Test Plan

**SHIPPED** — tests delivered with commit `7163fcd`. Documented here for completeness.

## Scope

The streak and weekly-history widgets consuming FOR-139. Backend (FOR-139) out of scope.

## API client (progress)

- `GET /api/v1/progress/streak` → `{ currentStreakDays, longestStreakDays, asOf }`.
- `GET /api/v1/progress/weekly-history` → per-week series.

## Widgets

- Streak widget renders current + longest streak from the payload.
- Weekly-history bars render one bar per week from the series.
- Empty history → streak 0/0 and zeroed buckets (series still present), not an error.
- Loading → `LoadingState`; fetch error → `ErrorState` (FOR-60).

## Accessibility

- Values conveyed as text (not color alone); states announced; axe coverage.

## Fixtures

- Mocked streak + weekly-history payloads, including the empty/zeroed case.
