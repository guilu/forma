# FOR-123 AI Context

## Story

FOR-123 — Integrations: success feedback on connect/sync/disconnect
(https://dbhlab.atlassian.net/browse/FOR-123)

## Intent

`IntegrationsSection.tsx`'s own doc comment names this exact follow-up:
"It deliberately does not add a success-toast branch for connect/sync/
disconnect: `connectIntegration`, `syncIntegration` and
`disconnectIntegration`... are typed `Promise<never>` and always reject by
design until FOR-103 ships a backend, so a success path here would be
unreachable dead code today... Wiring `useNotify()` success feedback here
is follow-up work for whenever FOR-103 lands." This story is that
follow-up, verbatim.

## Blocked by

FOR-103 (External Integrations epic backend — real OAuth connect/
disconnect, sync). Without FOR-103, `connectIntegration`/
`syncIntegration`/`disconnectIntegration` cannot resolve, so this story's
change is untestable and effectively unreachable.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-004-integrations.md` (external providers as adapters)
- `specs/FOR-57/` (the original integrations story this section
  implements)
- `specs/FOR-63/` (the notification/feedback pattern this story wires in —
  `useNotify`, dedupe/limit behavior)
- Jira: https://dbhlab.atlassian.net/browse/FOR-123, and
  https://dbhlab.atlassian.net/browse/FOR-103 for the blocking backend

## Domain Notes

- `frontend/src/pages/integrations/IntegrationsSection.tsx` lines 114–152
  — `handleConnect`, `handleSync`, `handleDisconnectConfirm`; each already
  has the full try/catch/finally shape, missing only the success branch.
- `frontend/src/api/integrations.ts` — `connectIntegration`,
  `syncIntegration`, `disconnectIntegration` currently typed
  `Promise<never>` with an explicit doc comment: "When FOR-103 lands, swap
  each function body for an `apiClient.request()` call against the path
  already documented on it below." This story's precondition is that swap
  having happened.
- `ShoppingPage.tsx`'s `toggle()` handler (lines 97–124) is the direct
  precedent to mirror: `notify.success('Artículo actualizado.')` inside
  the try branch, after the awaited call succeeds.

## Architectural Constraints

- Success messages come from the frontend (UI copy), not the backend —
  consistent with how `ShoppingPage.toggle()` already does it, since the
  backend response for these commands isn't expected to carry
  user-facing copy.
- Reuse `useNotify()` exactly as imported/used elsewhere; no new
  notification variant needed.

## Common Pitfalls

- Attempting to implement or test this story before FOR-103 lands — the
  functions structurally cannot resolve, so there is nothing to verify
  yet. Confirm FOR-103's status first.
- Adding a success toast that duplicates or conflicts with any
  success-state UI FOR-103's backend integration might introduce
  elsewhere (e.g. an updated `lastSyncAt` display) — toast is additive
  feedback, not the only signal of success.

## Suggested Implementation Order

1. Confirm FOR-103 has shipped and `connectIntegration`/`syncIntegration`/
   `disconnectIntegration` can resolve.
2. Add `notify.success(...)` to each handler's try branch with a specific,
   calm message naming the provider.
3. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Exercise connect/sync/disconnect against a local/dev backend
once FOR-103 is available and confirm toasts fire on success only.
