# FOR-56 UI Spec

## Screens

- Insights surface: the dashboard insight widget (FOR-51) + a fuller view
  (within Progreso or a dedicated route — see Open Questions). Related mockup:
  `docs/6-progreso.png`.

## Components

- Insight card: message + severity badge (`INFO`/`WARNING`/`ACTION`) + reason.
- Related-signals block: body deltas / training completion from the check-in.
- Secondary recommendations list.
- Non-medical disclaimer note.
- Reuse FOR-50 severity badge + `Card`; shared with the dashboard widget.

## States

- Loading: insight skeleton (FOR-60).
- Empty: FOR-45 insufficient-data `INFO` main rendered as a calm "need more data"
  message.
- Error: load failure → error + retry.
- Success: main + optional secondaries + reason + signals.

## Interactions

- Read-only in the MVP (recommendations are computed, not editable).
- Optional link from a signal to its feature page (body/training/nutrition).

## Accessibility

- Severity conveyed by text + badge, not color alone.
- Reason/disclaimer are readable text; card is a labelled region.

## Responsive Behavior

- Desktop: insight card + evidence side by side.
- Mobile: stacked; the main recommendation is prominent and calm; no horizontal
  scroll.
