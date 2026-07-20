# FOR-163 Test Plan

Strict TDD where practical. Token changes are mostly CSS; assert via theme/token tests and existing snapshots.

## Scope

`theme.css` (+ `theme.ts` if it mirrors tokens) and font wiring. No feature-page restyle.

## Token / theme

- `theme.ts`/`ThemeContext` tests still pass; if `theme.ts` enumerates tokens, assert the reconciled set
  (new/renamed vars present, both themes covered).
- Dark and light both define every touched token (no `undefined`/empty var in `[data-theme='light']`).
- Reconciled accent/surface values equal the template values (guard against silent drift back).

## Components (regression)

- `DesignSystemExamples` and component snapshots re-baseline intentionally (review the diff — colors/fonts
  change on purpose); no structural regressions.
- No component references a removed variable (build/type check passes; aliases where kept).

## Fonts

- Template fonts are loaded via the bundled strategy (no CDN `<link>`); `--font-*` resolve to them.

## Accessibility

- Contrast of text-on-surface / accent-on-surface remains acceptable in both themes (axe/manual check).

## Fixtures

- Token reference table extracted from `docs/*.html` for the expected-value assertions.
