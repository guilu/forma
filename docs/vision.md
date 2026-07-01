# Forma product vision

## One sentence

Forma is a personal fitness operating system that connects body composition, training, nutrition and shopping cost into one weekly decision loop.

## Why it exists

The initial problem is practical: Diego trains late in summer to avoid Alicante heat and humidity, which makes dinner timing, recovery and nutrition harder to organize. A static weekly diet spreadsheet helps, but it cannot adapt to body metrics, training load, late running sessions or changing shopping prices.

Forma should become the system that answers: what should I do this week based on what happened last week?

## Product principles

1. **Evidence over vibes**
   - Body weight alone is not enough.
   - Track weight, body fat, lean mass, training load and running pace together.

2. **Small weekly adjustments**
   - Prefer +100/-100 kcal decisions over aggressive diet changes.
   - Avoid turning good habits into punishment.

3. **Late-training friendly**
   - Running days should move carbohydrates earlier in the day.
   - Post-run recovery should be light and practical.
   - Dinner should not be huge when training finishes late.

4. **Food-first, whey-as-tool**
   - Protein target matters more than exact timing.
   - Whey is a convenient fallback, not a magic ritual.

5. **Personal first, SaaS-ready later**
   - Build for Diego initially.
   - Keep architecture clean enough to support multiple users later.

## Current known baseline

- Weight: 73.6 kg
- Body fat: 14.7 %
- BMI: 22.7
- Usual running: 4 km in ~23 minutes
- Equipment: dumbbells, bench, resistance bands, doorway pull-up bar
- Nutrition goal: recomposition, not aggressive weight loss
- Target: maintain 73-75 kg, move gradually towards 12-13 % body fat, improve strength and 10 km capacity

## Key jobs to be done

### Weekly check-in

As a user, I want to enter or import my latest body metrics so that I can see whether my plan is working.

### Training planning

As a user, I want a running and strength plan adapted to my equipment and schedule so that I can progress without overthinking.

### Nutrition planning

As a user, I want day-type nutrition templates so that running, strength and rest days are handled differently.

### Shopping budget

As a user, I want the weekly shopping list to estimate budget so that the plan is realistic and editable.

### Insights

As a user, I want the app to suggest small changes based on trends so that I do not have to interpret all metrics manually.

## Non-goals for MVP

- No full calorie tracker like MyFitnessPal.
- No social network.
- No complex AI coach in the first version.
- No automatic Mercadona scraping in MVP.
- No Withings dependency for first release.
