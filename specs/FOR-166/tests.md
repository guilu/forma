# FOR-166 Test Plan

Strict TDD where practical. Visual refactor — preserve behaviour; re-baseline snapshots intentionally.

## Scope

`MeasurementsPage` + `MeasurementForm` presentation. Data/read-write unchanged, out of scope.

## Behaviour (must not regress)

- Latest metrics, historical list and trend chart still render from their data.
- Manual-entry form still validates; errors render adjacent to fields.
- Manual vs imported source indicator still distinguishes values (text/icon, not color alone).
- FOR-60 empty/loading/error states preserved (empty-before-first-measurement).

## Visual / layout

- Page matches the template; snapshot re-baselined (diff reviewed).
- Uses FOR-164 components + tokens (no hardcoded hex where practical).
- Responsive single-column on mobile.

## Accessibility

- Form labels associated; validation errors announced/adjacent; contrast both themes; axe passes.

## Fixtures

- Existing measurement mocks (list, latest, empty, validation-error), reused.
