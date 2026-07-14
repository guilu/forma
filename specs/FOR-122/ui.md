# FOR-122 UI Spec

## Screens

- Objetivos (`frontend/src/pages/GoalsPage.tsx`) at `/objetivos`, already
  registered in `app/routes.tsx` and `app/navigation.ts` (`{ path:
  '/objetivos', label: 'Objetivos', icon: 'goals', primary: false }`).
  Mockup: `docs/7-objetivos.png`.

## Components

- Goal list (cards or rows, `Card`-based per the app's existing patterns).
- Goal detail view (modal or dedicated sub-route, TBD by FOR-104's actual
  shape and this story's implementation).
- Edit form for a goal's editable fields (pending FOR-104's exposed
  fields).
- Update or link-through from `ProfileSection`'s "Objetivo principal" and
  `ObjectivesSection`'s rows (see `spec.md` Open Questions).

## States

- Loading: `LoadingState` while goals load (FOR-60).
- Empty: `EmptyState` when no goals exist.
- Error: `ErrorState` with retry on load failure (FOR-60).
- Success: goal list + detail/edit flow.

## Interactions

- Select a goal from the list → detail view.
- Edit a goal → save → feedback (FOR-63) → list/detail reflect the change.

## Accessibility

- List/detail navigation keyboard-operable; edit form fields labelled with
  errors close to fields (ADR-006, FOR-61).

## Responsive Behavior

- Mockup-consistent (`docs/7-objetivos.png`); list collapses to a single
  column on narrow viewports, matching the app's established mobile
  pattern (Dashboard/Progreso/Ajustes precedent).
