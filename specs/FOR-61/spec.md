# FOR-61: Implement accessible interaction patterns

Jira: https://dbhlab.atlassian.net/browse/FOR-61
Epic: FOR-47 UI & UX

## Summary

Make the MVP UI accessible: keyboard navigation for core flows, visible focus,
semantic headings/landmarks, accessible form labels/errors, accessible buttons/
links, reasonable color contrast, and screen-reader-friendly status messages.
WCAG-oriented for MVP scope; semantic HTML before ARIA. A cross-cutting pass over
the shell (FOR-49) and all feature screens.

## User/System Flow

No direct user flow. Applies accessible patterns across existing components and
screens.

## Functional Requirements

- **Keyboard navigation** for core flows (nav, forms, actions) without a mouse.
- **Visible focus** states everywhere (theme tokens).
- **Semantic structure**: headings hierarchy, `<nav>`/`<main>`/`<header>`
  landmarks.
- **Forms**: labels tied to inputs; validation errors associated + announced.
- **Buttons/links**: native semantics, discernible names.
- **Color contrast**: verify the dark (and light) palette meets contrast for
  normal use; status not conveyed by color alone.
- **Status messages**: `aria-live` for loading/empty/error and action feedback
  (works with FOR-60/FOR-63).

## Non-Functional Requirements

- Prefer semantic HTML before ARIA; avoid inaccessible custom controls.
- Accessibility checks included in UI review.

## Data Model Notes

None — cross-cutting interaction/markup concern.

## Edge Cases

- Custom controls (selectors, toggles, tabs) must be keyboard + screen-reader
  usable or replaced with accessible equivalents.
- Focus management on route changes, modal open/close (`Modal.tsx`), and
  multi-step flows (FOR-59).

## Open Questions

- Contrast of the neon accent `#9dff57` on dark surfaces for text vs decorative
  use — verify and restrict low-contrast accent to non-text where needed;
  document.
- Automated a11y checks (axe/jest-axe) vs manual review — recommend adding a
  lightweight automated check where practical; document.
