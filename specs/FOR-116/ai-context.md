# FOR-116 AI Context

## Story

FOR-116 — Integrations provider brand logos
(https://dbhlab.atlassian.net/browse/FOR-116)

## Intent

`IntegrationsSection.tsx` currently uses placeholder generic line icons
(`heart`/`activity`/`cross`) for Withings/Google Fit/Apple Health because no
brand asset work has been done yet. This story swaps in the real brand
marks, respecting each provider's trademark guidelines, purely as a visual
polish pass — no functional change to the (currently non-functional,
FOR-103-blocked) connect/sync/disconnect flow.

## Blocked by

None.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-004-integrations.md` (external providers as adapters —
  relevant context even though this story is cosmetic only)
- `docs/adr/ADR-006-frontend.md` (token-driven styling, theme support)
- `specs/FOR-57/` (the integrations connection story this section
  implements)
- `frontend/src/theme/themedRendering.test.tsx` (existing pattern for
  testing token-driven components across both themes — follow the same
  approach for logo contrast)
- Jira: https://dbhlab.atlassian.net/browse/FOR-116

## Domain Notes

- `frontend/src/pages/integrations/IntegrationsSection.tsx` lines 60–64:
  `PROVIDER_ICONS: Record<IntegrationProviderId, IconName>` — the exact
  mapping to replace.
- Same file, lines ~213 and ~255: the two render sites (`<Icon
  name={PROVIDER_ICONS[provider.providerId]} />`) that must both be
  updated consistently.
- `IntegrationProviderId` type (in `frontend/src/api/integrations.ts`) —
  the closed set of provider ids this story covers (`WITHINGS`,
  `GOOGLE_FIT`, `APPLE_HEALTH`); no new providers introduced.

## Architectural Constraints

- Presentation-only change — no `IntegrationConnection`/
  `IntegrationProviderId` type change, no API call change (ADR-006).
- Vendored or referenced brand assets must respect each provider's
  trademark/brand usage guidelines — do not alter, recolor, or recompose
  official marks outside permitted variants.

## Common Pitfalls

- Updating only one of the two render sites and leaving the other on the
  old generic icon.
- Using a brand mark that fails contrast/legibility in dark mode without a
  documented reversed/alternate variant.
- Removing the provider name text and relying on the logo alone for
  identification — keep an accessible name independent of the image.

## Suggested Implementation Order

1. Source each provider's official brand asset (SVG preferred) and confirm
   license/usage terms.
2. Replace `PROVIDER_ICONS` (or restructure it to hold logo references)
   with the new assets; keep a fallback for missing providers.
3. Update both render sites.
4. Verify light/dark theme legibility.
5. Tests per `tests.md`.

## Validation

`npm run test`, `npm run lint`, `npm run typecheck`, `npm run build` from
`frontend/`. Visually inspect the Conexiones section in both light and dark
theme.
