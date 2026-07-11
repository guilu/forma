# FOR-57: Create integrations management screens

Jira: https://dbhlab.atlassian.net/browse/FOR-57
Epic: FOR-47 UI & UX

## Summary

Build the integrations management UI: connected providers, available providers,
connect/disconnect actions, last-sync timestamp, sync status, manual-sync action
and error display. Mockup: the "CONEXIONES E INTEGRACIONES" section of
`docs/8-configuracion.png` (Withings connected; Google Fit / Apple Health not
connected). Provider status must never leak tokens/sensitive details.

## User/System Flow

1. User opens integrations (within Ajustes, `/ajustes`, or a sub-route).
2. Connected + available providers load from the External Integrations endpoints
   (future backend); each shows status + last sync.
3. User connects/disconnects or triggers a manual sync where supported; OAuth
   redirects are handled outside this story but entry points exist.

## Functional Requirements

- **Connected providers list**: provider, status ("Conectado"), last-sync
  timestamp, manual-sync action.
- **Available providers list**: provider, "No conectado", connect action.
- **Connect / disconnect actions**: explicit; connect may hand off to an OAuth
  flow (redirect handled elsewhere).
- **Sync status + manual sync**: status display + a "sync now" action where
  supported.
- **Error display**: sync/connection errors shown clearly without sensitive data.
- Loading and empty states (FOR-60).

## Non-Functional Requirements

- Never render provider tokens or sensitive fields.
- Clear provider-status components; deterministic rendering.

## Data Model Notes

Consumes the External Integrations endpoints (Withings sync, connection status).
**Repository state**: the integrations backend does not exist yet (bootstrap; see
AGENTS.md) — this UI provides the entry points + status display and must degrade
gracefully / use a documented mock until the backend lands. Do not invent backend
behavior; document the dependency on the External Integrations epic.

## Edge Cases

- No providers connected → available list only, empty connected state.
- Sync error → error surfaced, no token/PII leakage.
- Backend not available yet → entry points visible but clearly non-functional /
  mocked; documented.

## Open Questions

- The integrations backend (OAuth, sync) is a separate epic not yet built —
  recommend building the UI against a typed client with a mock/placeholder and
  wiring it when the backend exists; document.
- Whether integrations live inside Ajustes (FOR-58) or a dedicated route —
  recommend a section within Ajustes matching the mockup.
