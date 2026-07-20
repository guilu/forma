# FOR-166 Spec

Jira: https://dbhlab.atlassian.net/browse/FOR-166
Epic: FOR-162 Design System v2. Blocked by FOR-164 (shared components).

## Summary

Refactor the mediciones / body-composition view to match `docs/2-mediciones.html`, using the reconciled
tokens (FOR-163) and refreshed shared components (FOR-164). Visual only — preserve data, validation and
states. Frontend-only.

## Repository baseline (verified)

- `frontend/src/pages/MeasurementsPage.tsx` (+ `.module.css`) with `MeasurementForm` (shared component)
  for manual entry; consumes measurement read/write, distinguishes manual vs imported, uses FOR-60 states.
- Chart via `LineChart`/`ChartContainer`; metric display via `MetricCard`.
- Template: `docs/2-mediciones.html`.

## Functional Requirements

- Align the latest-measurement cards, historical list, trend chart containers and the manual-entry form
  with the template, via FOR-164 components + FOR-163 tokens.
- Remove per-page visual overrides now covered by shared components/tokens.
- Preserve data wiring, manual/imported distinction, form validation and FOR-60 states.

## Non-Functional Requirements

- Responsive (mobile single-column) + both themes (FOR-62); a11y preserved (FOR-61), especially form labels/errors.
- Token/component-driven styling only.

## UI / States (see ui.md)

- Metric cards, history list, chart, entry form restyled; states preserved.

## Edge Cases

- Empty state before first measurement → template-consistent `EmptyState`.
- Validation errors stay adjacent to fields (FOR-52/FOR-61) after restyle.
- Manual vs imported source indicator remains visually distinct (not color alone).

## Open Questions

- If the template restructures the history list (e.g. table vs cards), follow it and document the change.
- Any new form-field or chart variant needed → raise in FOR-164.
