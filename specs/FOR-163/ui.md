# FOR-163 UI Spec

## Screens

- No feature screen changes. Visual surface = `frontend/src/components/DesignSystemExamples.tsx` (the
  design-system showcase) + the ThemeToggle for light/dark verification.

## Components

- `frontend/src/styles/theme.css` (the changed unit) + `frontend/src/theme/theme.ts` / `ThemeContext`.
- `DesignSystemExamples.tsx` renders swatches/typography/components against the tokens — use it to review.

## States

- Dark (default) and light (`[data-theme='light']`) both render with the reconciled tokens.
- No loading/empty/error states — token layer only.

## Interactions

- ThemeToggle switches themes; both must be visually correct under the new tokens.

## Accessibility

- Contrast preserved in both themes (FOR-61); accent/text/surface combinations checked.
- No meaning conveyed by the palette change alone — this is a value remap, not a semantic change.

## Responsive Behavior

- Unchanged; tokens are viewport-independent. Spacing/radius scale updates apply everywhere via the vars.
