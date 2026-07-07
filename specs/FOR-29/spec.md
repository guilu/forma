# FOR-29: Create nutrition day template model

Jira: https://dbhlab.atlassian.net/browse/FOR-29
Epic: FOR-4 Nutrition Planner

## Summary

Create the `NutritionDayTemplate` domain model: a framework-free type describing
the macro targets for a **day type** (running / strength / rest), so nutrition
adapts to the day instead of a rigid weekly diet. Domain-only — no persistence,
API or UI.

## User/System Flow

This story has no user-facing flow. It defines the type consumed by later
stories:

1. FOR-31 attaches `MealTemplate`s to a day template.
2. FOR-32 compares computed totals to this template's targets.
3. FOR-33 seeds running/strength/rest day templates.

## Functional Requirements

- Add `NutritionDayTemplate` under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`, framework-free
  (no Spring/JPA/JDBC/HTTP — ADR-001), following the FOR-22/FOR-24 precedents.
- Fields (docs/domain-model.md "NutritionDayTemplate"): `type`,
  `targetCalories`, `targetProteinG`, `targetCarbsG`, `targetFatG`, `notes`.
- `type` constrained to `RUNNING`, `STRENGTH`, `REST` (enum).
- Macro targets are numeric and storable; model supports future customization
  (values editable later — do not hardcode a single diet).
- Keep the model independent from specific food brands/products (nutrition
  targets only; foods are FOR-30).

## Non-Functional Requirements

- Performance: pure in-memory value; no external calls.
- Security: no user credentials/PII in this type.
- Observability: none at this layer.

## Data Model Notes

Mirrors docs/domain-model.md's `NutritionDayTemplate`. It carries **targets**,
not the actual meals (meals are FOR-31 `MealTemplate`, which reference a day
template). This story introduces no persisted entity.

## Edge Cases

- Negative or zero macro targets — decide whether the type rejects at
  construction (recommended, per FOR-15/FOR-22 precedent) or defers validation.
- `type` outside the known set — impossible if modelled as an `enum`.
- Macro targets that don't sum to the calorie target — out of scope to enforce
  here (document; a later calc/validation story may check consistency).

## Open Questions

- Validate macro targets at construction (positive values) vs. plain data
  holder — recommend construction-time validation for internal consistency
  (project precedent).
- Whether calorie/macro coherence (4/4/9 kcal per g) is enforced now — recommend
  no; targets are directional (FOR-33 notes they are not medical prescriptions).
