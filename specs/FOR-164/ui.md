# FOR-164 UI Spec

## Screens

- No feature screens. Review surface = `DesignSystemExamples.tsx` in both themes.

## Components

- Restyled: `Card`, `MetricCard`, `Badge`, `StatusPill`, `Button`, `ChartContainer`, `LineChart`,
  `MacroRing` (and FOR-60 states / `NotificationProvider` checked for consistency).
- All consume FOR-163 tokens via their CSS modules.

## States

- Each component's existing states (default/hover/focus/disabled/loading/error) restyled to the template
  look; behaviour unchanged.
- Both `data-theme` values render correctly.

## Interactions

- Interaction behaviour (clicks, focus order, dialogs) unchanged — visual only.
- New variants (if added) exposed via props with safe defaults.

## Accessibility

- Visible focus-visible outlines; contrast preserved; roles/semantics intact (FOR-61).
- Status conveyed by text/icon + tone, not color alone (Badge/StatusPill).

## Responsive Behavior

- Components stay fluid (max-width, wrapping) per current behaviour; the per-view stories handle page layout.
