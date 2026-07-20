# FOR-143 UI Spec

**SHIPPED** in commit `7163fcd`. Documents the delivered UI.

## Screens

- Progreso / Entrenamiento (`frontend/src/pages/progress/`, within `ProgressPage`; mockup
  `docs/3-entrenamiento.png`).

## Components

- Streak widget ("RACHA ACTUAL") — reuses `StatusPill` / `Card`.
- Weekly-history bars — reuses `ChartContainer`.
- FOR-60 `LoadingState` / `EmptyState` / `ErrorState`.

## States

- **Loading**: each widget fetches independently (`LoadingState`).
- **Success**: streak shows current + longest; weekly-history shows one bar per week.
- **Empty**: streak 0/0, zeroed weekly buckets (bars still present) — a normal state.
- **Error**: `ErrorState` per widget.

## Interactions

- Read-only widgets. No progression logic in the UI.

## Accessibility

- Streak and bar values conveyed as text, not color alone; states announced (FOR-60 patterns).

## Responsive Behavior

- Mobile single-column, matching the Progreso page pattern.
