# Codex prompts

Use these prompts as task starters for Codex or any coding agent. Each prompt should produce a small, reviewable pull request.

## Prompt 1 — Bootstrap monorepo

```txt
Create the initial Forma monorepo.

Read the docs first:
- README.md
- docs/vision.md
- docs/architecture.md
- docs/domain-model.md
- docs/ui-guidelines.md
- docs/roadmap.md

Goal:
Create a runnable monorepo skeleton for the Forma app.

Stack:
- Backend: Java 21, Spring Boot 3, PostgreSQL, Flyway
- Frontend: React, TypeScript, Vite, TailwindCSS, Recharts, React Router, TanStack Query
- Local dev: Docker Compose

Tasks:
1. Create backend/ Spring Boot project.
2. Create frontend/ Vite React TypeScript project.
3. Create infra/docker-compose.yml with PostgreSQL.
4. Add root .gitignore and .editorconfig.
5. Add backend health endpoint GET /api/health.
6. Add frontend landing/dashboard placeholder following docs/ui-guidelines.md.
7. Update README with local development instructions.
8. Add basic GitHub Actions workflow for backend and frontend builds.

Constraints:
- Keep the PR small enough to review.
- Do not implement Withings yet.
- Do not implement authentication yet.
- Use clean names and domain language from docs/domain-model.md.

Acceptance criteria:
- Backend starts locally.
- Frontend starts locally.
- PostgreSQL starts with Docker Compose.
- CI runs backend and frontend checks.
```

## Prompt 2 — Body measurements MVP

```txt
Implement the Body measurements MVP.

Read:
- docs/domain-model.md
- docs/roadmap.md

Tasks:
1. Add BodyMeasurement entity/table with Flyway migration.
2. Add repository and application service.
3. Add REST endpoints:
   - GET /api/body/measurements
   - POST /api/body/measurements
   - GET /api/dashboard
4. Calculate fatMassKg and leanMassKg from weight and body fat.
5. Add frontend body measurement form.
6. Add dashboard cards for weight, body fat, lean mass and BMI.
7. Add trend chart for weight and body fat.

Acceptance criteria:
- User can manually add a measurement.
- Latest measurement appears in dashboard.
- Dashboard works with seed data.
```

## Prompt 3 — Training plan MVP

```txt
Implement the Training MVP.

Tasks:
1. Add running plan model and seed a 16-week progression from 4 km to 10 km.
2. Add strength plan model and seed Push/Pull/Legs workouts.
3. Add API endpoints for current week training plan.
4. Add frontend Training page with running and strength sections.
5. Allow marking a planned session as completed.

Constraints:
- Equipment must match: dumbbells, bench, resistance bands, doorway pull-up bar.
- Avoid gym-machine exercises.
```

## Prompt 4 — Nutrition templates MVP

```txt
Implement nutrition day templates.

Tasks:
1. Add FoodItem, NutritionDayTemplate, MealTemplate and MealItem tables.
2. Seed running, strength and rest day templates.
3. Running day must support late summer runs:
   - carbs earlier in the day
   - pre-run snack
   - optional whey post-run
   - light dinner
4. Add macro calculations.
5. Add frontend Nutrition page.

Acceptance criteria:
- User can select RUNNING, STRENGTH or REST day.
- App shows meals and total kcal/protein/carbs/fat.
```

## Prompt 5 — Shopping budget MVP

```txt
Implement shopping budget MVP.

Tasks:
1. Add MercadonaProduct and ShoppingItem tables.
2. Seed editable Mercadona products with estimated prices and URLs where known.
3. Add API endpoints to list and update products.
4. Add Shopping page with weekly and monthly budget.
5. Add checklist behaviour.

Constraints:
- Do not scrape Mercadona.
- Treat prices as editable estimates.
```
