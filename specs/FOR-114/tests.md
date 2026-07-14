# FOR-114 Test Plan

## Scope

Verify the axe integration itself works (catches a known violation, passes
on compliant markup) and that representative screens pass with no
violations.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A.

## UI Tests

- The shared axe test helper flags a deliberately-broken fixture (e.g. an
  `<img>` with no `alt`) as a violation — proves the wiring actually runs
  axe rules, not a no-op.
- The shared axe test helper passes on a minimal compliant fixture.
- Dashboard, Mediciones (or Progreso), Lista de compra, Ajustes, Onboarding
  and Integraciones each pass their axe scan with zero violations in their
  primary rendered state.
- `Card`/`MetricCard`/`ChartContainer` heading-order fix (FOR-112) is
  covered by the axe "heading-order" rule on at least one multi-card
  screen.

## Edge Cases

- A screen with an inert "Próximamente" control does not produce a false-
  positive violation (or the suppression is explicitly documented if one is
  needed).
- Toast content (FOR-63, `aria-live`) is scanned after the relevant async
  state settles (`findBy*`/`waitFor`), not mid-render.

## Fixtures

- One deliberately-inaccessible fixture and one minimal-compliant fixture
  to prove the axe wiring itself (not just app screens).
