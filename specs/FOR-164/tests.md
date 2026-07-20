# FOR-164 Test Plan

Strict TDD where practical. Mostly component tests + intentional snapshot re-baselining. No backend.

## Scope

Shared components in `frontend/src/components/`. Feature pages out of scope (FOR-165..168).

## Component behaviour (must not regress)

- `Card` still honours `headingLevel` (FOR-112) and renders children/title as before.
- Button variants render their expected roles/labels; disabled/loading states intact.
- Badge/StatusPill render the correct tone per status prop.
- ChartContainer/LineChart/MacroRing render with the same data contracts.
- FOR-60 states + NotificationProvider unchanged in behaviour.

## Visual / tokens

- Components use tokens (assert no hardcoded hex in the changed modules where practical, or review).
- `DesignSystemExamples` snapshot re-baselined intentionally (diff reviewed).
- Both themes render (light/dark) without missing tokens.

## Accessibility

- Visible focus states on Button/Card actions; contrast acceptable; semantics preserved (roles/headings).
- axe assertions on `DesignSystemExamples` (and any component with a11y tests) still pass.

## Fixtures

- Existing component test fixtures; add variant fixtures where a new prop/variant is introduced.
