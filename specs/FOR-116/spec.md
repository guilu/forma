# FOR-116: Integrations provider brand logos

Jira: https://dbhlab.atlassian.net/browse/FOR-116
Epic: FOR-47 UI & UX

## Summary

`IntegrationsSection.tsx` currently maps each provider to a generic line
icon from the shared `Icon` set (`PROVIDER_ICONS`: `WITHINGS: 'heart'`,
`GOOGLE_FIT: 'activity'`, `APPLE_HEALTH: 'cross'` — verified against the
source). Replace these with the providers' actual brand marks, respecting
each provider's trademark/brand guidelines, while keeping a fallback for
any provider without an available mark. Purely cosmetic — no behavior
change.

## User/System Flow

No new flow — the existing Conexiones section (`/ajustes/integraciones`
and embedded in Ajustes) renders provider rows; this story only changes
what renders in the icon slot.

## Functional Requirements

- Replace the `PROVIDER_ICONS` generic-icon mapping in
  `IntegrationsSection.tsx` with each provider's brand logo (Google Fit,
  Apple Health, Withings) for the three `IntegrationProviderId` values
  already defined.
- Respect each provider's brand guidelines (Google Fit / Google branding
  guidelines, Apple's Human Interface trademark guidelines for Apple
  Health, Withings' brand usage) — do not distort, recolor outside
  permitted variants, or combine the mark with unrelated elements.
- Keep a documented fallback (e.g. the current generic `Icon` mapping) for
  any provider added later without an available brand asset, so a missing
  logo never breaks the row.
- No change to `IntegrationConnection`, `IntegrationProviderId`, or any
  API/read-model shape — this is a rendering-only change (ADR-006: frontend
  renders read models, doesn't need new data for a cosmetic swap).

## Non-Functional Requirements

- Assets sized/optimized appropriately for the existing icon slot (avoid
  unnecessarily large image files bloating the bundle); prefer SVG where
  the provider offers one.
- Legible in both light and dark themes (FOR-62) — verify contrast/
  background compatibility for each mark, since `IntegrationsSection`
  renders inside theme-aware `Card`s.

## Data Model Notes

None — `PROVIDER_ICONS` (or its replacement) is a presentation-layer
constant local to `IntegrationsSection.tsx`; no domain or API change.

## Edge Cases

- A brand mark that doesn't render well on a dark background (many brand
  logos are designed for light backgrounds only) → use the provider's
  documented dark-mode/reversed variant if one exists, or a neutral
  container behind the mark; do not stretch/recolor the logo itself to
  compensate.
- Provider connected vs. not-connected state — the same logo renders in
  both `StatusPill` variants (`IntegrationsSection.tsx` lines ~213, 255);
  confirm the swap covers both render sites.

## Open Questions

- Whether brand assets are vendored locally (SVG files under
  `frontend/src/assets/` or similar) vs. referenced from a CDN — recommend
  vendoring for offline/dev reliability and to avoid an external network
  dependency in the UI; document the final choice and license/attribution
  notes if the provider's brand guidelines require them.
