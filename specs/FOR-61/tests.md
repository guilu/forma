# FOR-61 Test Plan

## Scope

Verify core flows are keyboard-usable, structure is semantic, forms/controls are
accessible, and status messages are announced.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A.

## UI Tests

- Core navigation is operable by keyboard; the active item exposes
  `aria-current`.
- Landmarks (`nav`/`main`/`header`) and a sensible heading order exist.
- Form inputs have associated labels; validation errors are associated and
  announced.
- Icon-only buttons have accessible names; links/buttons use native semantics.
- Loading/empty/error and action feedback use `aria-live`/`role="status"`.
- Focus is visible and managed on modal open/close and route changes.

## Edge Cases

- Custom controls (tabs, selectors, toggles) are keyboard + SR usable.
- Accent-on-dark text meets contrast or is restricted to non-text.

## Fixtures

- Existing screens/components; optional jest-axe checks on key components.
