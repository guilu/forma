# FOR-54: Create nutrition planner screens

Jira: https://dbhlab.atlassian.net/browse/FOR-54
Epic: FOR-47 UI & UX

## Summary

Build the nutrition (Nutrición) screens: day-type selector (running/strength/
rest), daily meal plan, meal detail cards, macro summary, optional recovery
recommendation, late-running-day light-dinner guidance and a link to shopping
list generation. Mockup: `docs/4-nutricion.png`. `NutritionPage.tsx` exists
(FOR-33/34); this story aligns it to the mockup. UI must not do nutrition math.

## User/System Flow

1. User opens Nutrición (`/nutricion`).
2. The day plan (meals + macros) loads from the Nutrition read models
   (FOR-32 macros, FOR-33 templates, FOR-34 running-day flow).
3. User reviews meals/macros; running days highlight the carbs-early / lighter-
   dinner flow; a shortcut leads to the shopping list.

## Functional Requirements

- **Day type selector**: running, strength, rest (FOR-29 day templates).
- **Daily meal plan view**: "COMIDAS DEL DÍA" (Desayuno → Cena) with per-meal
  macro chips (P/C/G) and kcal.
- **Meal detail cards**: name, description, macros, calories, done indicator.
- **Macro summary**: calories vs target, macro ring (Proteínas/Carbohidratos/
  Grasas), objetivo-vs-actual comparison (FOR-32).
- **Running-day guidance**: the FOR-34 late-run flow (Breakfast → Lunch → Pre-run
  snack → Run → Light recovery → Light dinner); make carbs-early / lighter-dinner
  obvious.
- **Recovery/whey recommendation**: shown when provided by the read model.
- **Shopping shortcut**: navigate to the shopping list (FOR-55).
- Empty and error states (FOR-60).

## Non-Functional Requirements

- No nutrition calculations in the UI (ADR-001) — read models only.
- Reusable macro-summary components; supports future meal-substitution flows.

## Data Model Notes

Consumes FOR-32 macro calc, FOR-33 seeded day templates, FOR-34 running-day flow.
**Mockup extras not yet backed**: meal logging/checkmarks ("Registrar comida"),
water/hydration tracking + "Añadir agua", "NUTRIENTES CLAVE" (fibra/azúcares/
sodio/grasas saturadas), objetivo-vs-actual macro bar chart — render only what an
API supports; otherwise placeholder/omit (repository priority, AGENTS.md).

## Edge Cases

- Rest day → no running-day flow shown.
- No plan/template for the day → empty state.
- Missing recovery recommendation → section hidden, not empty-broken.

## Open Questions

- Meal logging, hydration and key-nutrient tracking exceed current backend —
  recommend read-only plan display for the MVP and document the gaps.
- Day-type selection source (auto from training day vs manual) — document.
