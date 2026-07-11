# FOR-54 UI Spec

## Screens

- Nutrición (`frontend/src/pages/NutritionPage.tsx`) at `/nutricion`. Mockup:
  `docs/4-nutricion.png`.

## Components

- Day-type selector (running/strength/rest).
- Macro summary: calories vs target, macro ring, objetivo-vs-actual.
- Meal list "COMIDAS DEL DÍA": per-meal card (name, description, P/C/G, kcal).
- Running-day flow strip (Breakfast → … → Light dinner) emphasizing carb timing.
- Recovery/whey recommendation block (when present).
- Shopping shortcut card ("LISTA DE LA COMPRA · N productos").
- Meal-logging / hydration / key-nutrient panels deferred unless backed.

## States

- Loading: plan/macro skeletons (FOR-60).
- Empty: no plan for the day → clear empty state.
- Error: load failure → error + retry.
- Success: meals + macros + (running-day guidance) rendered.

## Interactions

- Switch day type → plan/macros update.
- Tap a meal → detail (macros/description).
- Shopping shortcut → navigates to Lista de compra (FOR-55).
- "Registrar comida"/"Añadir agua" only if backed; otherwise not shown as active.

## Accessibility

- Day-type selector is a labelled control group; macro values are text with
  units; ring has an accessible summary.
- Running-day flow order is conveyed textually, not by color/position alone.

## Responsive Behavior

- Desktop: meal list + macro/nutrient side column.
- Mobile: Hoy/Semana/Mes tabs; calories/macros/water summary first, then meals;
  no horizontal scroll.
