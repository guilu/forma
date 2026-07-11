# FOR-57 AI Context

## Story

FOR-57 — Create integrations management screens
(https://dbhlab.atlassian.net/browse/FOR-57)

## Intent

Let users connect, review and manage external providers and know whether sync is
working. Success is a provider list (connected/available) with connect/disconnect,
last-sync, status, manual sync and safe error display.

## Relevant Documents

- `AGENTS.md` (bootstrap status — integrations backend not built yet)
- `docs/ui-guidelines.md`, `docs/8-configuracion.png` (Conexiones e integraciones)
- `docs/adr/ADR-006-frontend.md`, `docs/adr/ADR-002-*` (single-user MVP, no auth),
  `docs/adr/ADR-007-testing.md`
- Jira: https://dbhlab.atlassian.net/browse/FOR-57

## Domain Notes

- Providers in the mockup: Withings (Conectado), Google Fit, Apple Health (No
  conectado). The sidebar footer already shows "Withings · Conectado".
- No integrations backend exists yet — build against a typed client + mock; never
  expose tokens.

## Architectural Constraints

- Consume External Integrations endpoints via a `frontend/src/api/integrations.ts`
  client (add). Presentational status components; no token/PII in the UI.

## Common Pitfalls

- Leaking provider tokens/sensitive details.
- Wiring OAuth redirect logic into this story (out of scope — entry points only).
- Presenting a non-existent backend as fully functional.

## Suggested Implementation Order

1. Typed integrations client + provider status model (mock until backend exists).
2. Connected + available provider lists with status + last sync.
3. Connect/disconnect + manual-sync entry points; safe error display.
4. Loading/empty states; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Confirm no sensitive fields render. Compare against the mockup's
integrations section.
