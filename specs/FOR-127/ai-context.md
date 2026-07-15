# FOR-127 AI Context

## Story

FOR-127 — Meal consumption logging: domain, persistence and day consumption vs target API. First implementable slice of FOR-102 [STUB] Nutrition consumption logging + hydration. Macros only.

## Intent

Move the nutrition domain from plan-only to plan + actual consumption for meals: record what the user ate and expose consumed-vs-target macros. Success = FOR-54 "Registrar comida" and the dashboard "calorías consumidas vs objetivo" have a real backend. Hydration (slice 2) and key-nutrient tracking (later) are deferred.

## Relevant Documents

- `specs/FOR-102/` — full nutrition-consumption scope, slicing, and open questions (this story is slice 1).
- `AGENTS.md` — hexagonal boundaries, owner-scoping, never log sensitive health data.
- `docs/adr/ADR-001-architecture.md`, `ADR-002-authentication.md`, `ADR-003-persistence.md`, `ADR-005-api-design.md`.
- Jira: https://dbhlab.atlassian.net/browse/FOR-127

## Domain Notes

- Existing plan-side domain (do NOT duplicate): `NutritionDay`, `NutritionDayTemplate`, `NutritionDayCatalog`, `NutritionTotals`, `NutritionCalculator`, `TargetComparison`, `FoodItem`, `FoodCatalog`, `MealTemplate`, `MealItem`, `MealType`.
- FOR-105 already exposes macro totals + target comparison over HTTP — reuse that read-model style.
- There is **no** consumption concept in the repo today — this slice introduces `MealLogEntry` + a per-day aggregate.
- Plan-side target comes from the existing `NutritionDay(Template)`/calculators; consumption is a separate additive concept.

## Architectural Constraints

- Domain framework-free; application port + service; thin controller under `delivery/nutrition`; JDBC adapter under `adapter/persistence`.
- Follow FOR-107/110/125/126 structure exactly (they are the merged reference implementations).
- Logging is additive — never mutate plan templates.
- Reuse `NutritionCalculator`/`NutritionTotals`/`TargetComparison` for all macro math; the frontend must not compute it.
- New migration is **V13** (current head V12); one column per statement.
- Owner-scoped (ADR-002); never log entry contents at INFO.

## Common Pitfalls

- Duplicating macro math instead of reusing `NutritionCalculator`.
- Storing a denormalized consumed-total that can drift from the entries — derive on read, or maintain consistently and test it.
- Returning 404 for an empty day instead of a zeroed read model.
- Building hydration or key-nutrient tracking here — those are separate slices.
- Mutating plan templates to represent consumption.
- Bypassing the owner boundary "because there's only one user".

## Suggested Implementation Order

1. `MealLogEntry` + per-day meal-log aggregate domain (+ tests) — accumulation, free vs catalog entries, no plan mutation.
2. Application port + service (log entry, day consumption read model reusing calculators), owner-scoped.
3. JDBC adapter + `V13` migration (+ persistence round-trip test).
4. `delivery/nutrition` endpoints `POST /nutrition/log` + `GET /nutrition/consumption` + DTOs (+ API tests) per `api.md`.

## Validation

Run backend build + tests (`./gradlew build`). Confirm: logging updates day consumed totals; consumption compares against plan target reusing existing calculators; day without target returns consumed-only; empty day returns zeroed read model (not 404); invalid input → 400; plan templates never mutated. Then FOR-54 (frontend) and the dashboard widget can consume the endpoints.
