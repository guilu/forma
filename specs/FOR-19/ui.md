# FOR-19 UI Spec

## Screens

- Dashboard page (`frontend/src/pages/DashboardPage.tsx`) — currently a
  `PagePlaceholder`; this story adds the real metric cards.

## Components

- Five metric cards (weight, body fat %, fat mass, lean mass, BMI), each
  built on `frontend/src/components/Card.tsx`.
- Empty-state component/message shown instead of the cards when there are no
  measurements.

## States

- Loading: cards area shows a loading indicator while the API call is
  in-flight.
- Empty: single clear message (e.g. "No measurements yet — add one to see
  your dashboard") replacing all five cards; no zero/placeholder numbers.
- Error: a clear error state if the API call fails (ADR-006 requires this
  for every major screen).
- Success: five cards populated from the latest measurement.

## Interactions

- No user input on this screen beyond navigation; data is read-only here.
- (Optional, if implementation wires it) a link/action to the Measurements
  page (FOR-18) from the empty state, since that is where a measurement is
  added — not mandated by Jira, implementer's call.

## Accessibility

- Each card's numeric value has a text label (e.g. "Weight", "Body fat %"),
  not color/icon alone.
- Empty and error states are announced to screen readers (not just visual).

## Responsive Behavior

- Desktop: full card grid as the primary dashboard view
  (docs/ui-guidelines.md: desktop can prioritize full dashboard).
- Mobile: docs/ui-guidelines.md ranks "today's plan" and "add measurement"
  above the full dashboard on mobile — cards should still render correctly
  on small screens (stacked, readable), but are not the top mobile
  priority.
