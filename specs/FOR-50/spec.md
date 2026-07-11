# FOR-50: Define MVP design system

Jira: https://dbhlab.atlassian.net/browse/FOR-50
Epic: FOR-47 UI & UX

## Summary

Define a small, practical design system: tokens (color, typography, spacing,
radius, elevation) and the base reusable components (buttons, cards, inputs,
badges/status, chart container). FOR-81 already created the **token layer** in
`frontend/src/styles/theme.css` (dark default + light override) and a `Card`;
this story completes the **component layer** and documents usage so feature pages
stop hardcoding low-level visual rules.

## User/System Flow

No direct user flow. This story produces the shared building blocks every screen
(FOR-51..FOR-59) composes.

## Functional Requirements

- Confirm/extend token single-source-of-truth in `styles/theme.css`: color,
  typography scale, spacing scale, radius, elevation/shadow (most already exist —
  add only what components need).
- Provide reusable base components under `frontend/src/components/`:
  - Button variants (primary/accent, secondary, ghost, destructive).
  - Card (exists) + section/header patterns used by the pages.
  - Form field styles (input, select, error text) — align with the existing
    `MeasurementForm` field styling.
  - Badge / status styles (e.g. "Saludable", severity `INFO`/`WARNING`/`ACTION`,
    "Conectado"/"No conectado", plazo tags "Corto/Medio/Largo plazo").
  - Chart container style (wraps `LineChart`; consistent framing/padding).
- Components consume tokens only — no hardcoded colors/spacing.
- Must support future dark mode (FOR-62): tokens already theme-mapped.
- Practical, not ornamental (docs/ui-guidelines.md: restrained accent use).

## Non-Functional Requirements

- Reusable across all modules; no feature-specific logic in base components.
- Responsive-friendly primitives; accessible defaults (labels, focus) feeding
  FOR-61.

## Data Model Notes

None — presentational primitives.

## Edge Cases

- Status/badge for an unknown value → neutral fallback style, never a broken/
  unstyled badge.
- Buttons in loading/disabled state (feeds FOR-60/FOR-63).

## Open Questions

- How much to formalize as a living style guide: recommend a lightweight usage
  doc/examples page rather than a heavyweight Storybook for the MVP — document
  the choice.
- Exact button/badge variant set — derive from the mockups (accent primary,
  outline secondary, severity + connection + plazo badges) and document.
