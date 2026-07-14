# FOR-124 UI Spec

## Screens

- Progreso (`frontend/src/pages/progress/InsightsSection.tsx`, within
  `ProgressPage`) at `/progreso`.

## Components

- `InsightsSection` — related-signals rendering extended to show WoW
  deltas alongside absolute values.
- New history list/view (component TBD — likely a sibling component
  within `frontend/src/pages/progress/`, e.g. `InsightsHistorySection`),
  consuming FOR-110's history endpoint; reuses `Card`, `StatusPill`.

## States

- Current-week section: unchanged loading/empty/error/success states, plus
  deltas on signals when available.
- History view: independent loading/`EmptyState`/`ErrorState`/success
  states (FOR-60) for its own fetch.

## Interactions

- Selecting a history entry shows that period's full insights (reusing the
  current-week rendering pattern for a historical `WeeklyInsights`
  payload).

## Accessibility

- Delta values are conveyed with text (not color alone) — e.g. an explicit
  "−0.4 kg" or "↓ 0.4 kg" with accessible text equivalent, not a bare
  colored arrow icon (ui-guidelines.md, FOR-61 pattern).
- History list items keyboard-operable, with clear accessible labels
  identifying each period.

## Responsive Behavior

- History view follows the same Progreso page responsive pattern (mobile
  single-column, matching `docs/6-progreso.png`).
