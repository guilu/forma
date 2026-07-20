# FOR-161 UI Spec

## Screens

- Entrenamiento → running plan view (`frontend/src/pages/TrainingPage.tsx` or a dedicated section;
  mockup `docs/3-entrenamiento.png`).

## Components

- New: a 16-week plan view (table or list).
- Reuse `Card` (`headingLevel`, FOR-112), FOR-60 states, existing training components.

## States

- **Loading**: plan fetch in flight (`LoadingState`).
- **Success**: rows for every week — week number, weekly volume (km), the 3 sessions with distances;
  deload weeks visually + textually marked (e.g. a "Descarga" badge).
- **Error**: fetch failed → `ErrorState`; the rest of the training page still functions.
- **Empty**: no plan returned (unlikely) → `EmptyState`.

## Interactions

- Read-only view. Optionally the current week is highlighted (cross-referenced with `/week`) — a
  design decision, not required.

## Accessibility

- Semantic table/list; deload marked with text (not color alone).
- States announced (`role="status"` / `role="alert"`), consistent with FOR-60.

## Responsive Behavior

- Mobile single-column: weeks stack or the table scrolls horizontally within its own container;
  matches the training page responsive pattern.
