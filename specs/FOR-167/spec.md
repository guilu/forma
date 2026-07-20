# FOR-167 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-167
Epic: FOR-162 Design System v2. Blocked by FOR-164 (shared components).

## Summary

Refactor the entrenamiento / training view to match `docs/3-entrenamiento.html`, using the reconciled
tokens (FOR-163) and refreshed shared components (FOR-164). Visual only — preserve data, completion actions
and states. Frontend-only.

## Repository baseline (verified)

- `frontend/src/pages/TrainingPage.tsx` (+ `.module.css`) — weekly calendar, session detail, exercise
  breakdown, streak/weekly-history widgets (FOR-143), muscle map (`trainingMuscleLabels.ts`, FOR-160),
  completion actions, FOR-60 states.
- Uses `Card`/`StatusPill`/`ChartContainer` etc.
- Template: `docs/3-entrenamiento.html`.

## Coordination (important)

- In-flight training UI work touches the same page: **FOR-158** (strength exercise breakdown replacing a
  placeholder) and **FOR-161** (16-week running-plan view). Sequence to avoid conflicts — ideally land
  FOR-158/FOR-161 first, then restyle; or restyle the stable parts and coordinate the moving ones.

## Functional Requirements

- Align the weekly calendar, session detail, exercise breakdown, streak/weekly-history widgets and muscle
  map with the template, via FOR-164 components + FOR-163 tokens.
- Remove per-page visual overrides now covered by shared components/tokens.
- Preserve data wiring, completion actions and FOR-60 states.

## Non-Functional Requirements

- Responsive + both themes (FOR-62); a11y preserved (FOR-61).
- Token/component-driven styling only; muscle-map grouping logic (FOR-160) untouched.

## UI / States (see ui.md)

- Calendar, session detail, widgets, muscle map restyled; states preserved.

## Edge Cases

- Rest-day / running vs strength session detail all restyled consistently.
- Streak/weekly-history empty → zeroed state preserved (FOR-143).
- Muscle-map rendering unchanged in data (only visual framing via `ChartContainer`/tokens).

## Open Questions

- Merge order with FOR-158/FOR-161 (which lands first) — decide before implementation.
- If the template restructures the weekly calendar, follow it and document.
