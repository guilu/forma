# FOR-165 Test Plan

Strict TDD where practical. Visual refactor — preserve behaviour; re-baseline snapshots intentionally.

## Scope

`DashboardPage` + `dashboard/*Widget` presentation. Data/read models unchanged and out of scope.

## Behaviour (must not regress)

- Each widget still renders loading / empty / error / success from its data hook (FOR-60).
- Navigation from each widget to its feature page still works.
- No change to what data each widget requests.

## Visual / layout

- `DashboardPage` composes the template grid/sections; snapshot re-baselined (diff reviewed).
- Widgets use FOR-164 components + tokens (no hardcoded hex in the touched modules where practical).
- Responsive: single-column on mobile, grid on desktop (render at representative widths).

## Accessibility

- Headings/landmarks intact; focus order sane; contrast acceptable both themes; axe passes.

## Fixtures

- Existing widget mocks (data + loading/empty/error), reused unchanged.
