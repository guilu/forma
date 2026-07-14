# FOR-112 Test Plan

## Scope

Verify `Card`/`MetricCard`/`ChartContainer` render the correct heading tag
per `headingLevel`, default to today's `<h3>` behavior, and that audited
pages produce a non-skipping heading order.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A.

## UI Tests

- `Card` with no `headingLevel` passed renders `<h3>` (regression guard for
  default behavior).
- `Card` with `headingLevel={2}` renders `<h2>`; same for `4`, `5`, `6`.
- `MetricCard`/`ChartContainer` forward `headingLevel` to the underlying
  `Card` correctly.
- `Card` with no `title` renders no heading regardless of `headingLevel`.
- For each audited page (or a representative sample), the rendered heading
  sequence contains exactly one `<h1>` and no skipped level between
  consecutive headings.

## Edge Cases

- Two sibling `MetricCard`s under the same `<h2>` section both resolve to
  the same `headingLevel`.
- A `Card` nested inside another `Card` (if any exists) does not produce a
  skipped level.

## Fixtures

- Minimal render fixtures for `Card`/`MetricCard`/`ChartContainer` at each
  supported `headingLevel`.
- Full-page render fixtures (or axe scans, pairing with FOR-114) for at
  least Dashboard, Mediciones and Ajustes to validate the real heading
  outline end to end.
