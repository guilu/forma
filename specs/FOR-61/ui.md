# FOR-61 UI Spec

## Screens

- Cross-cutting — applies to the shell (FOR-49) and every feature screen.

## Components

- Focus-visible styles (tokens) on all interactive elements.
- Labelled form fields + associated error text (extends FOR-50 inputs).
- Accessible modal (`Modal.tsx`): focus trap, restore focus, `Esc` to close.
- `aria-live` region wired into FOR-60 states and FOR-63 feedback.

## States

- Loading/empty/error: announced via `role="status"`/`aria-live`.
- Success: action feedback announced (with FOR-63).

## Interactions

- Full keyboard operation of nav, forms, tabs, toggles, buttons.
- Logical tab order; focus moves to headings on step/route changes.

## Accessibility

- WCAG-oriented for MVP: labels, focus, semantics, contrast, SR status.
- Semantic HTML before ARIA; no color-only meaning.

## Responsive Behavior

- Focus and semantics consistent across breakpoints; touch targets adequately
  sized on mobile; no keyboard trap in mobile nav/overlays.
