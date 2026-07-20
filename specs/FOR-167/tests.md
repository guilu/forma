# FOR-167 Test Plan

Strict TDD where practical. Visual refactor — preserve behaviour; re-baseline snapshots intentionally.

## Scope

`TrainingPage` presentation (calendar, session detail, streak/history widgets, muscle map). Data/actions
unchanged, out of scope. Coordinate with FOR-158/FOR-161 tests.

## Behaviour (must not regress)

- Weekly calendar, running/strength/rest session detail render from data.
- Completion action still marks a session complete (behaviour unchanged).
- Streak/weekly-history widgets keep their FOR-143 states (incl. empty→zeroed).
- Muscle-map grouping (FOR-160) output unchanged (its test still passes).

## Visual / layout

- Page matches the template; snapshots re-baselined (diff reviewed).
- FOR-164 components + tokens (no hardcoded hex where practical).
- Responsive single-column on mobile.

## Accessibility

- Headings/landmarks; keyboard-operable completion + navigation; contrast both themes; axe passes.

## Fixtures

- Existing training mocks (week, session detail, streak/history, muscle map), reused.
