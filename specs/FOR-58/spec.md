# FOR-58: Create user profile and settings screens

Jira: https://dbhlab.atlassian.net/browse/FOR-58
Epic: FOR-47 UI & UX

## Summary

Build the profile + settings (Configuración / Ajustes) screens: personal profile
summary, units/locale preferences, training/nutrition preference entry points,
integrations entry point (FOR-57), data export/import entry points and a theme
preference entry point (FOR-62). Mockup: `docs/8-configuracion.png`.
`SettingsPage.tsx` is currently a placeholder; this story builds it out with a
section-based layout that distinguishes editable vs read-only and never shows
unsupported options as active.

## User/System Flow

1. User opens Ajustes (`/ajustes`).
2. Section-based settings render: Perfil y preferencias, Unidades, Conexiones
   (FOR-57), Objetivos por defecto, Notificaciones (FOR-63), Seguridad y datos,
   Acerca de.
3. User edits supported preferences; unsupported options are visibly inert or
   hidden.

## Functional Requirements

- **Personal profile summary**: avatar, name, email, birthdate, sex, height,
  activity level, main goal; "Editar perfil" entry point.
- **Units & locale**: peso (kg), altura (cm), distancia (km), energía (kcal).
- **Default objectives**: déficit calórico, proteínas, agua diaria (entry points).
- **Training / nutrition preference entry points** (link to future flows).
- **Integrations entry point**: to FOR-57.
- **Data export/import entry points**: "Exportar mis datos" / "Importar datos".
- **Theme preference**: light/dark toggle entry point (FOR-62).
- **Security section**: change password, 2FA, delete account (entry points).
- Clearly distinguish editable vs read-only; do not present unsupported backend
  flows as active. Loading/empty/error states (FOR-60).

## Non-Functional Requirements

- Section-based, grouped-by-domain layout; mobile-friendly.
- No unsupported backend flows built in the UI (entry points only where backend
  is missing).

## Data Model Notes

**Repository state**: no user/profile/preferences backend exists yet (single-user
MVP, ADR-002). Profile/preferences are largely display + entry points until a
backend story lands — document which values are static/mock vs persisted. Units
preference may drive display formatting locally. The "Objetivo principal /
objetivos" here overlaps the Objetivos screen (`docs/7-objetivos.png`), which has
no dedicated FOR-47 child — treat goals as read-only summary here and flag the gap.

## Edge Cases

- Unsupported option (2FA, export) → shown as "próximamente"/inert, not a broken
  action.
- No profile backend → static/mock profile, clearly documented.
- Mobile → sections collapse into a scrollable list (matches phone mockup).

## Open Questions

- Profile/preferences persistence needs a backend — recommend display + entry
  points for the MVP and document the dependency.
- Goals ("Objetivos") ownership: no dedicated story/backend — document and keep a
  read-only summary here.
