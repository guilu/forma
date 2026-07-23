# FOR-169 UI Spec

## Screens

- Dashboard (`/`) — empty or guided first-run state.
- Mediciones (`/mediciones`).
- Lista de compra (`/lista-compra`).
- Nutrición (`/nutricion`).
- Entrenamiento (`/entrenamiento`).

All screens, on an empty/pre-onboarding API, must render an empty state + a
first-run CTA instead of any seeded personal data.

## Components

- Reuse the FOR-60 shared `EmptyState` (and `LoadingState`/`ErrorState`) already
  wired across these screens — do not build new one-off empty components.
- CTA: primary action linking to onboarding / the relevant configuration entry
  point (e.g. "Registrar primera medición", "Generar lista", "Configurar plan").
- Optional: a guided dashboard "empty" composition pointing the user to onboarding.

## States

- Loading — while first-run detection / reads resolve.
- Empty (first run) — no active user data; show CTA. **Primary state for this
  story.**
- Empty (configured, no data yet) — onboarding done but e.g. no measurements;
  add-data CTA (distinct copy from pre-onboarding).
- Error — read failure; existing error state, retry.

## Interactions

- CTA click → onboarding flow (`frontend/src/pages/onboarding/*`) or the screen's
  own "add/configure" action.
- No seeded data is ever shown or actionable before configuration.

## Accessibility

- Empty states use the existing accessible `role="status"` pattern (FOR-60).
- CTA is a real button/link with a clear, non-guilt label (ui-guidelines.md).
- Keyboard-reachable CTA; visible focus.

## Responsive Behavior

- Empty states + CTAs follow the existing responsive layout of each screen
  (desktop/tablet/mobile) — no new layout work beyond swapping seeded content for
  the empty/CTA composition.

## Copy Notes

- Warm, non-guilt, forward-looking ("Empecemos", "Registra tu primera medición")
  — no shaming for empty data.
- Distinguish "aún no has configurado esto" (pre-onboarding) from "todavía no hay
  datos" (configured, empty) so the CTA target is correct.
</content>
