# FOR-112 AI Context

## Story

FOR-112 — Threaded heading hierarchy across Card/MetricCard/ChartContainer
(https://dbhlab.atlassian.net/browse/FOR-112)

## Intent

`Card.tsx` line 26 hardcodes `<h3>` for its title
(`{title && <h3 className={styles.title}>{title}</h3>}`), and both
`MetricCard` and `ChartContainer` wrap `Card` without any way to override
that level. Every card-based title on every page is therefore an `<h3>`
regardless of the page's actual outline, which can skip `<h2>` and fails
WCAG 2.4.6/2.4.10. This story makes the level configurable and fixes the
real call sites.

## Blocked by

None.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md` ("accessibility... first-class MVP
  concerns")
- `specs/FOR-61/` (accessible interaction patterns, prior a11y work in this
  epic)
- `specs/FOR-50/` (design system primitives — `Card` originates here)
- Jira: https://dbhlab.atlassian.net/browse/FOR-112

## Domain Notes

- `frontend/src/components/Card.tsx` — line 26, the hardcoded `<h3>` to
  parameterize.
- `frontend/src/components/MetricCard.tsx` — wraps `Card`, passes `title`
  as `Card`'s `title` prop (line 26: `<Card title={label}>`).
- `frontend/src/components/ChartContainer.tsx` — wraps `Card` the same way
  (line 32: `<Card title={title} action={action}>`).
- Roughly 22 non-test files render `Card`/`MetricCard`/`ChartContainer`
  (verified via `grep -rl "<Card\|<MetricCard\|<ChartContainer"
  frontend/src --include="*.tsx" | grep -v .test.tsx`) — that count is the
  audit surface for this story.

## Architectural Constraints

- Keep `Card` presentational and domain-agnostic (ADR-006) — `headingLevel`
  is a structural prop only, no behavior change.
- Default value must exactly reproduce today's output for any call site
  that doesn't pass the new prop — this is a non-breaking addition.

## Common Pitfalls

- Changing the CSS class/visual styling instead of just the tag — the
  visual size of card titles must not change, only the semantic level.
  Consider a shared class applied regardless of tag (e.g. keep
  `className={styles.title}` on whichever tag is rendered) rather than
  per-level CSS.
- Auditing pages superficially and leaving a skipped level in a
  less-visited screen (e.g. a settings section) — check every page/section
  in the ~22-site list, not just the obvious dashboard/progress pages.
- Forgetting `ChartContainer`'s `action` slot interacts with the same
  header row as the title — don't disturb that layout while changing the
  tag.

## Suggested Implementation Order

1. Add `headingLevel` prop to `Card`, default `3`, render `h2`–`h6`
   dynamically (e.g. via a small tag-lookup, not a large conditional).
2. Forward `headingLevel` through `MetricCard` and `ChartContainer`.
3. Audit all call sites; set explicit `headingLevel` where the page outline
   requires it.
4. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Inspect the accessibility tree (or run axe once FOR-114 lands)
on each audited page to confirm a non-skipping heading order.
