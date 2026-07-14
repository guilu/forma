# FOR-115 AI Context

## Story

FOR-115 — Settings "Soporte y ayuda" section
(https://dbhlab.atlassian.net/browse/FOR-115)

## Intent

FOR-58 built out Ajustes into sections but left "Soporte y ayuda" (shown in
the mockup) unbuilt. This story fills that specific, narrow gap using the
exact pattern FOR-58 already established for static/link-only sections
(`AboutSection`), so it needs no new design system work.

## Blocked by

None.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-006-frontend.md` (section-based layout, no unsupported
  flows shown as active)
- `specs/FOR-58/spec.md`, `specs/FOR-58/ui.md` (the parent Ajustes story
  and its documented section list — "Soporte y ayuda" is the one section
  that story's own spec/build didn't cover)
- `docs/8-configuracion.png` (mockup — read closely for exact entries)
- Jira: https://dbhlab.atlassian.net/browse/FOR-115

## Domain Notes

- `frontend/src/pages/settings/AboutSection.tsx` is the direct pattern to
  copy: a `Card` with `SettingsRow` children, some `inert`.
- `frontend/src/pages/settings/SettingsRow.tsx` — the reusable row
  component (label/value/inert) already used by every settings section.
- `frontend/src/pages/SettingsPage.tsx` — the section list to extend
  (imports + mounts each section, lines 1–58).

## Architectural Constraints

- No backend call — this is a static-content section, same tier as
  `AboutSection` (not `ProfileSection`/`UnitsSection`, which at least
  display mock/local data pending FOR-107).
- Reuse `Card`/`SettingsRow` exactly; no new shared component needed.

## Common Pitfalls

- Inventing support channels (email addresses, chat links) that don't
  exist in the repository — follow `AboutSection`'s precedent of marking
  unbacked entries `inert` instead.
- Placing the section out of the mockup's visual order relative to the
  other Ajustes sections.

## Suggested Implementation Order

1. Read `docs/8-configuracion.png`'s "Soporte y ayuda" block closely.
2. Build `SupportSection.tsx` following `AboutSection.tsx`'s structure.
3. Mount it in `SettingsPage.tsx` in mockup order.
4. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Visually compare the rendered Ajustes screen against
`docs/8-configuracion.png`.
