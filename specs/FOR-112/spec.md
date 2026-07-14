# FOR-112: Threaded heading hierarchy across Card/MetricCard/ChartContainer

Jira: https://dbhlab.atlassian.net/browse/FOR-112
Epic: FOR-47 UI & UX

## Summary

`Card` (`frontend/src/components/Card.tsx`) always renders its `title` as a
hardcoded `<h3>`. `MetricCard` and `ChartContainer` both wrap `Card` and
pass `title` straight through, so every metric tile and chart card on every
page is an `<h3>`, regardless of whether the page's own heading structure
actually has two levels above it. Across roughly 22 call sites (pages +
sections that render `Card`/`MetricCard`/`ChartContainer`, excluding
tests), this produces an inconsistent or skipped heading order (WCAG 2.4.6,
2.4.10) — e.g. a page with only an `<h1>` page title followed directly by
`<h3>` card titles, skipping `<h2>`. This story adds a `headingLevel` prop
(default preserves current `<h3>` behavior) and audits call sites to set it
correctly.

## User/System Flow

No standalone screen — a structural change to shared components consumed
across Dashboard, Mediciones, Entrenamiento, Nutrición, Lista de compra,
Progreso, Ajustes and Integraciones.

## Functional Requirements

- Add an optional `headingLevel` prop to `Card` (e.g. `2 | 3 | 4 | 5 | 6`,
  default `3` to preserve current behavior with zero call-site changes
  required for correctness-neutral cases). Render the corresponding
  `h2`–`h6` element instead of the hardcoded `<h3>`.
- Thread `headingLevel` through `MetricCard` and `ChartContainer` (both
  already forward `title` to `Card`; add the same pass-through prop).
- Audit all ~22 call sites across pages/sections (`DashboardPage`,
  `MeasurementsPage`, `TrainingPage`, `NutritionPage`, `ShoppingPage`,
  `ProgressPage`, `SettingsPage` sections, `IntegrationsSection`, etc.) and
  set an explicit `headingLevel` wherever the page's actual heading order
  requires something other than the default `3` — i.e. wherever a section
  heading (`<h2>`) sits between the page `<h1>` and the card title, `3` is
  correct and needs no change; wherever cards are direct siblings of the
  page `<h1>` with no intervening `<h2>`, set `headingLevel={2}`.
- No visual change — `headingLevel` controls the semantic tag only; styling
  stays on the existing CSS classes (`styles.title`) regardless of level.

## Non-Functional Requirements

- Zero behavioral regression for call sites that don't pass
  `headingLevel` — default must reproduce today's `<h3>` output exactly.
- Accessible: correct, non-skipping heading order per screen (WCAG 2.4.6
  Headings and Labels, 2.4.10 Section Headings), verifiable with an
  accessibility tree / axe scan (pairs with FOR-114).

## Data Model Notes

None — purely presentational/structural. `Card`, `MetricCard`,
`ChartContainer` carry no domain data (Card.tsx doc comment: "Carries no
domain data itself").

## Edge Cases

- A page that renders both `MetricCard` and `ChartContainer` side by side
  under the same `<h2>` section — both need the same `headingLevel` to stay
  siblings in the outline, not accidentally different levels.
- A `Card` used without a `title` (no header renders at all today) —
  `headingLevel` is a no-op when there's no title to render.
- Nested cards (a `Card` containing another `Card`, if any exist) — verify
  during the audit that nesting doesn't produce a heading level that skips
  (e.g. `<h2>` directly followed by `<h4>`).

## Open Questions

- Whether to enforce a lint rule (e.g. a custom eslint check or a
  jest-axe assertion per FOR-114) preventing a future new call site from
  silently reintroducing a skipped heading level — recommend deferring to
  FOR-114's axe-core integration rather than building a bespoke lint rule
  now; document the decision either way.
