# ADR-013: Charting library

## Status

Accepted — supersedes [ADR-010](ADR-010-charts.md)

## Context

ADR-010 chose plain inline SVG for the MVP's body-progress graphs and said so
conditionally: "if a later story needs richer, interactive charts, revisit this
decision and introduce a vetted library then (superseding this ADR)". That point
has arrived.

The approved mockup (`docs/2-mediciones.html`) asks for a chart the in-house
component does not attempt: an area fill under the line, a horizontal grid,
several dated x-axis ticks, y-axis value ticks, and a hover tooltip reading out
the value and date of the point under the cursor.

The in-house component had also started to show its seams. It drew its SVG in a
fixed 320x120 viewBox stretched with `preserveAspectRatio="none"`, so in a
full-width card every label was scaled up with the plot — axis text rendered
several times its intended size, and the point markers came out as ellipses.
Fixing that alone means implementing responsive measurement, tick selection and
a hover layer: the feature set of a charting library.

## Decision

Render charts with **Recharts** (`recharts`, 3.x).

`LineChart` and `MultiLineChart` keep their existing props, so feature pages are
unchanged; only their internals now delegate to the library. `LineChart` gains a
`variant` prop: `detail` (axes, grid, tooltip) and `spark` (a bare trend line
for metric tiles).

Recharts was chosen over the alternatives considered because it is
React-idiomatic (composition of components, no imperative canvas handle), it
renders **SVG** — so the marks inherit design tokens through `var(--…)` and
`currentColor` exactly as the in-house component did — and it supports React 19.
A canvas library (uPlot, Chart.js) would be smaller but would put the visuals
behind an imperative theming API instead of the token system.

## Consequences

- A runtime dependency, and roughly 100 kB gzipped of it. Accepted for charts
  that are a primary surface of this product rather than a decoration.
- Responsive sizing, tick selection, tooltips and hit-testing stop being our
  code to maintain and get right.
- Charts are measured, not stretched: `ResponsiveContainer` needs a parent with
  a definite height, which is why every chart wrapper sets one (overridable via
  `--chart-height`).
- jsdom performs no layout, so component tests need a size shim for the
  container plus a `ResizeObserver` polyfill (`src/test/setup.ts`). Without
  them, charts measure zero and draw nothing.
- ADR-010's *rules* survive this change intact; they are restated below because
  they constrain how the library is used, not which library it is.

## Rules

- Charts render real API data only — no hardcoded or sample series in shipped
  components (FOR-20 DoD).
- Every chart has a text alternative (`role="img"` plus a describing label) so
  the trend is not communicated by colour alone (ADR-006 accessibility).
- Keep charts simple and readable on mobile (`docs/ui-guidelines.md`: avoid
  noisy charts); prefer honest rendering of sparse data over dense visuals. In
  particular the x-axis stays a **time** scale, so gaps between distant
  measurements are visible rather than evenly spaced, and axis ticks are taken
  from real measurement timestamps rather than invented round dates.
- Do not duplicate backend-derived values (e.g. lean mass) client-side; plot
  what the API returns.
- Never give one chart two live y-scales. `MultiLineChart` overlays series of
  different units (kg and %) by normalising each to its own range for a
  shape-only comparison, and therefore shows no numeric y-axis at all.
- Style through design tokens, not literal colours: the library accepts
  `var(--…)` in its `stroke`/`fill` props.
