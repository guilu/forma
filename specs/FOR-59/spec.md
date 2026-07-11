# FOR-59: Create first-run onboarding flow

Jira: https://dbhlab.atlassian.net/browse/FOR-59
Epic: FOR-47 UI & UX

## Summary

Build the first-run onboarding: a short, skippable, resumable multi-step flow that
collects the minimum to generate useful guidance — profile confirmation, current
body metrics (entry or import), goal selection, training availability, equipment,
nutrition basics and an integration prompt. No dedicated mockup; goal selection
mirrors `docs/7-objetivos.png` (Objetivos). No medical/diagnosis language.

## User/System Flow

1. A new user (no data) is routed into onboarding.
2. Steps: Perfil → Métricas actuales (entry or Withings import) → Objetivo →
   Disponibilidad de entrenamiento → Equipamiento → Preferencias de nutrición →
   Conectar integración.
3. On finish (or skip), the user lands on the dashboard with a clear next action.

## Functional Requirements

- **Multi-step flow** with progress indication, back/next, and skip for
  non-critical steps; resumable later.
- **Profile confirmation**: basic personal fields.
- **Body metrics**: manual entry (reuse `MeasurementForm`, FOR-52) or import
  prompt (FOR-57).
- **Goal selection**: choose a main objective (composición/rendimiento/hábito),
  aligned with the Objetivos concept (`docs/7-objetivos.png`).
- **Training availability + equipment**: capture days/equipment for plans.
- **Nutrition basics**: minimal preferences.
- **Integration prompt**: optional connect (FOR-57).
- Per-step validation with understandable errors; mobile-friendly.

## Non-Functional Requirements

- Small, easy-to-validate steps; skippable where safe; calm copy.
- Design for future persistence if no backend exists yet.

## Data Model Notes

**Repository state**: no onboarding/profile/goals backend exists yet (bootstrap).
Steps that lack a backend must be designed for future persistence — store
progress locally/in-memory and document. Reuse FOR-52 body entry + FOR-57
integration prompt where they exist. Goals have no backend/dedicated story —
capture the selection but document it as not yet persisted.

## Edge Cases

- User skips non-critical steps → still reaches a usable dashboard.
- Resume after leaving → progress restored where persistence exists.
- Validation error on a step → blocks advance with a clear message.

## Open Questions

- Persistence of onboarding answers needs backend — recommend local/deferred
  storage for the MVP and document.
- Entry trigger for "first run" with no auth/user backend — recommend a local
  flag until an auth/profile story exists; document.
