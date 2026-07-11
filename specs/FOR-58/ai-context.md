# FOR-58 AI Context

## Story

FOR-58 — Create user profile and settings screens
(https://dbhlab.atlassian.net/browse/FOR-58)

## Intent

Let users adjust basic personal + app preferences affecting training, nutrition
and display. Success is a clear, grouped, section-based settings screen that
distinguishes editable vs read-only and never fakes unsupported features.

## Relevant Documents

- `AGENTS.md` (bootstrap: no profile/preferences backend yet)
- `docs/ui-guidelines.md`, `docs/8-configuracion.png` (mockup)
- `docs/adr/ADR-002-*` (single-user MVP), `docs/adr/ADR-006-frontend.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-57/` (integrations), `specs/FOR-62/` (theme), `specs/FOR-63/`
  (notifications)
- Jira: https://dbhlab.atlassian.net/browse/FOR-58

## Domain Notes

- `frontend/src/pages/SettingsPage.tsx` is a placeholder — build it out.
- No profile/preferences backend exists; values are display + entry points until
  a backend story. Units preference can drive local display formatting.
- Integrations section links to FOR-57; theme toggle to FOR-62; notifications to
  FOR-63.

## Architectural Constraints

- Section-based layout; grouped by domain (ADR-006). Editable vs read-only clearly
  distinguished. No unsupported backend flows built in the UI.

## Common Pitfalls

- Presenting unsupported options (2FA, export/import) as working.
- Building profile persistence with no backend.
- Duplicating goals ownership (no dedicated goals story/backend).

## Suggested Implementation Order

1. Section scaffold: Perfil, Unidades, Conexiones, Objetivos, Notificaciones,
   Seguridad y datos, Acerca de.
2. Profile summary + units (local formatting); entry points for the rest.
3. Wire integrations (FOR-57), theme (FOR-62), notifications (FOR-63) entry
   points; mark unsupported items inert.
4. Empty/loading/error states; tests.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Compare against `docs/8-configuracion.png`.
