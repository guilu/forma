# FOR-60 UI Spec

## Screens

- No screen of its own — reusable state components consumed by all feature
  screens. Supersedes ad-hoc states + `PagePlaceholder`.

## Components

- `LoadingState` (page) + `WidgetLoading` (skeleton/spinner).
- `EmptyState` (feature) + `EmptyFilteredState`.
- `ErrorState` (recoverable, with retry) + `PermissionErrorState`.
- Inline `ValidationError` (near fields).
- Token-driven (FOR-50); consistent icon + message + optional action layout.

## States

- Loading: skeleton for content areas, spinner for quick actions; no layout jump.
- Empty: calm, actionable "no data yet" with an optional primary action.
- Error: clear message + retry; permission variant distinct.
- Success: N/A (host content renders).

## Interactions

- Retry re-runs the failed load.
- Empty-state primary action (e.g. "Registrar medición") where relevant.

## Accessibility

- States announced (`role="status"` / `aria-live`) so screen readers hear
  loading/empty/error changes (feeds FOR-61).
- Retry is a labelled, keyboard-operable button with visible focus.

## Responsive Behavior

- Components are fluid and center within their container across breakpoints;
  messages wrap without overflow.
