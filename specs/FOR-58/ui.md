# FOR-58 UI Spec

## Screens

- Configuración / Ajustes (`frontend/src/pages/SettingsPage.tsx`) at `/ajustes`.
  Mockup: `docs/8-configuracion.png`.

## Components

- Profile card: avatar, name, email, birthdate, sex, height, activity, main goal,
  "Editar perfil".
- Units section: peso/altura/distancia/energía selectors.
- Default objectives section: déficit calórico, proteínas, agua diaria.
- Connections section → FOR-57 provider list.
- Notifications section → FOR-63 toggles.
- Security & data: change password, 2FA, delete account, export/import (inert if
  unsupported).
- About: version, terms, privacy.
- Reuse FOR-50 section/card + toggle + badge primitives.

## States

- Loading: section skeletons (FOR-60).
- Empty: N/A (sections are static scaffolding).
- Error: a failing preference save (if backed) → inline error (FOR-63).
- Success: grouped sections rendered; supported edits persist.

## Interactions

- Toggle a supported preference → persists (if backed) with feedback (FOR-63).
- Open a sub-section (integrations, theme) → its screen.
- Destructive actions (delete account) → explicit confirmation (FOR-63).

## Accessibility

- Sections are landmarks/headings; toggles are labelled switches; read-only
  fields marked as such.
- Keyboard-operable controls with visible focus.

## Responsive Behavior

- Desktop: multi-column section grid (mockup).
- Mobile: single-column scrollable list of sections with chevrons; no horizontal
  scroll.
