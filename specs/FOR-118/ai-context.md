# FOR-118 AI Context

## Story

FOR-118 — Shopping list: interactive actions UI (regenerate, qty edit,
link-out) (https://dbhlab.atlassian.net/browse/FOR-118)

## Intent

`ShoppingPage.tsx`'s doc comment documents this exact gap: "'Generar nueva
lista', per-item Mercadona link-out/add-to-cart icons and +/- quantity
editing... omitted entirely rather than shown inactive." FOR-109 ships the
three backend commands this story wires up.

## Blocked by

FOR-109 (backend: regenerate, quantity-edit, product link-out commands).
Do not start implementation until FOR-109's endpoints exist — this story
has nothing to call otherwise.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md` (commands trigger backend logic, no
  client-side recomputation)
- `specs/FOR-109/spec.md`, `specs/FOR-109/ui.md` (API surface this story
  consumes)
- `specs/FOR-63/` (destructive-confirmation + success/error feedback
  patterns — `useNotify`, `ConfirmDialog`/`Modal`)
- `specs/FOR-61/` (accessible interaction patterns)
- Jira: https://dbhlab.atlassian.net/browse/FOR-118

## Domain Notes

- `frontend/src/pages/ShoppingPage.tsx` — read the file-level doc comment
  first; it lists this exact gap. The existing `toggle()` handler (lines
  ~97–124) is the pattern to mirror for the new quantity-edit handler:
  `pendingId` state, `try/catch` with `ApiRequestError` message
  extraction, `notify.success(...)` on success.
- `frontend/src/components/NotificationProvider.tsx` (`useNotify` hook,
  FOR-63) — reuse directly, already imported in `ShoppingPage.tsx`.
- `frontend/src/pages/integrations/IntegrationsSection.tsx`'s use of
  `ConfirmDialog` for the disconnect flow (FOR-63) is the closest existing
  precedent for the regenerate confirm dialog.

## Architectural Constraints

- No client-side cost recalculation — always render the backend's returned
  `estimatedCostEur` after a quantity edit (ADR-006).
- Destructive-confirmation pattern (FOR-63) required for regenerate since
  it discards current checked state.

## Common Pitfalls

- Computing the new estimated cost client-side "for responsiveness"
  instead of waiting for/trusting the backend response — this duplicates
  domain logic in the UI (forbidden by ADR-006).
- Leaving a quantity-edit row in a disabled/pending state forever on
  failure — always resolve to either the reverted value or the new value,
  never stuck (FOR-63 edge case).
- Showing the link-out control as a disabled icon when there's no URL
  instead of omitting it — this page's established precedent is "omit,
  don't show inactive."

## Suggested Implementation Order

1. Confirm FOR-109 has shipped; add the three command functions to
   `frontend/src/api/shopping.ts`.
2. Regenerate button + `ConfirmDialog` + success/error feedback.
3. Quantity +/- controls with pending-state guard and cost update.
4. Link-out control (conditional render on `productUrl`).
5. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Exercise regenerate (confirm + cancel paths), quantity +/-
(success + failure paths) and link-out against a local/dev backend once
FOR-109 is available.
