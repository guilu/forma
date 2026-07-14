# FOR-115: Settings "Soporte y ayuda" section

Jira: https://dbhlab.atlassian.net/browse/FOR-115
Epic: FOR-47 UI & UX

## Summary

FOR-58 built out `SettingsPage.tsx` into sections following its own spec's
User/System Flow ("Perfil y preferencias, Unidades, Conexiones, Objetivos
por defecto, Notificaciones, Seguridad y datos, Acerca de"), but the
mockup (`docs/8-configuracion.png`) also shows a "Soporte y ayuda" section
that FOR-58 did not build. This story adds it, following the same
Card + `SettingsRow` pattern already used by `AboutSection.tsx` and its
siblings.

## User/System Flow

1. User opens Ajustes (`/ajustes`).
2. A new "Soporte y ayuda" section renders alongside the existing sections
   (Perfil, Unidades, Conexiones, Objetivos, Notificaciones, Seguridad,
   Acerca de).
3. Entries are static/link content (e.g. help center, contact/report an
   issue, FAQ) — no backend, matching the mockup's scope.

## Functional Requirements

- New `SupportSection.tsx` under `frontend/src/pages/settings/`, following
  the exact pattern `AboutSection.tsx` already establishes: a `Card` with
  title, composed of `SettingsRow` entries.
- Content per the mockup (`docs/8-configuracion.png`, "Soporte y ayuda"
  block) — read the mockup before implementing to match its exact entries;
  do not invent entries beyond what it shows. If the mockup entries can't
  be determined precisely, use the same conservative pattern as
  `AboutSection` ("Términos y condiciones", "Política de privacidad" as
  inert rows) and document the substitution.
- Any entry with no working destination (no help center page/route in this
  repository) renders `inert` (via `SettingsRow`'s existing `inert` prop,
  same as `AboutSection`'s terms/privacy rows) rather than a dead link.
- Mount the new section in `SettingsPage.tsx`'s section list, in the mockup
  order relative to the other sections.

## Non-Functional Requirements

- Section-based, grouped-by-domain layout consistent with the rest of
  Ajustes (ADR-006); no new visual pattern introduced.
- No unsupported backend flows presented as active (AGENTS.md, FOR-58's own
  rule: "never shows unsupported options as active").

## Data Model Notes

None — static/link content only, same as `AboutSection`. No backend story
is a prerequisite (unlike FOR-119/120/121, which depend on FOR-107).

## Edge Cases

- No help-center page exists in this repository → the corresponding row is
  `inert`, not a broken link, exactly mirroring `AboutSection`'s existing
  precedent for terms/privacy.
- Mobile viewport → the section collapses into the same scrollable list as
  every other Ajustes section (FOR-58's existing responsive pattern);
  no distinct behavior needed.

## Open Questions

- Exact entries and copy — depends on reading `docs/8-configuracion.png`
  closely during implementation; if the mockup is ambiguous about specific
  support channels (e.g. email vs. in-app contact form), default to
  inert/placeholder entries and document the gap rather than fabricating a
  contact channel that doesn't exist.
