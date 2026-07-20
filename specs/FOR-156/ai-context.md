# FOR-156 AI Context

## Story

FOR-156 — Ajustes: editor de targets personales (consume FOR-149). Frontend-only. Surface the
profile's `personalTargets` in `ObjectivesSection`, with editing only if the section pattern supports it.

## Intent

The backend (FOR-149) already knows the user's personal targets (base kcal, body-fat range,
weight range, macros). The settings screen still shows inert placeholder objectives. Make the
real targets visible — and editable where the existing UI pattern allows — so settings reflect
the actual personalization the backend drives.

## Relevant Documents

- `specs/FOR-149/` — the profile `personalTargets` read model + update contract.
- `specs/FOR-58/`, `specs/FOR-119/` — settings screen and profile/preferences editing patterns.
- `AGENTS.md` — frontend consumes read models/commands; no domain logic in UI.
- `docs/adr/ADR-001-domain-first.md`, `ADR-006-frontend.md`, `docs/8-configuracion.png` (mockup).
- Jira: https://dbhlab.atlassian.net/browse/FOR-156

## Repo Notes (verify)

- `frontend/src/pages/settings/ObjectivesSection.tsx` — currently static from `profileData.ts`.
- `frontend/src/api/profile.ts` — profile client; confirm/extend the `personalTargets` type.
- `GET /api/v1/profile` returns `personalTargets` (FOR-149). Confirm field names against the payload.
- Reuse shared state components (FOR-60), `Card`/`headingLevel` (FOR-112), `useNotify` (FOR-63) for save feedback.

## Architectural Constraints

- Frontend-only; consume the profile read model + existing update command. No new backend.
- No client-side recomputation of targets (ADR-001) — pure render.
- Server-authoritative validation (ADR-006); show returned field errors.
- Accessible states; no color-only meaning.

## Common Pitfalls

- Recomputing macro/kcal targets in the UI instead of rendering backend values.
- Adding an edit flow when the section is display-only today — check first.
- Assuming `personalTargets` is always fully populated (older profiles may lack fields).
- Hardcoding the sample numbers (2300 kcal, 73–75 kg) instead of reading them from the profile.

## Suggested Implementation Order

1. Confirm the `personalTargets` type in `profile.ts`; extend if missing (+ client test).
2. Render the targets read-only in `ObjectivesSection` with FOR-60 states (+ test).
3. If (and only if) the section has an edit pattern, wire an edit→save flow through the profile
   update endpoint with server-side validation + `useNotify` (+ test).

## Validation

Run frontend checks (`npm run test`, `typecheck`, `lint`, `format:check`, `build`). Confirm the
targets render from the live profile, missing fields degrade gracefully, and (if edit is in scope)
a save persists and surfaces validation errors. No target math in the UI.
