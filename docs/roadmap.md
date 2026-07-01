# Forma roadmap

## Sprint 0 — Product foundation

Goal: make the repository understandable before writing application code.

Deliverables:

- README
- Product vision
- Architecture notes
- Domain model
- UI guidelines
- Codex prompts
- Initial backlog issues

Status: in progress.

## Sprint 1 — Technical bootstrap

Goal: create the runnable monorepo skeleton.

Deliverables:

- `backend/` Spring Boot 3 project
- `frontend/` React + Vite + TypeScript project
- `infra/docker-compose.yml` with PostgreSQL
- `.gitignore`
- `.editorconfig`
- root development instructions
- GitHub Actions basic CI

Acceptance criteria:

- `docker compose up` starts PostgreSQL.
- Backend starts locally.
- Frontend starts locally.
- CI validates backend and frontend builds.

## Sprint 2 — Body dashboard MVP

Goal: enter body measurements manually and visualize current state.

Deliverables:

- BodyMeasurement database migration
- REST endpoints for measurements
- dashboard endpoint
- frontend dashboard cards
- body measurement form
- trend chart for weight and body fat

Acceptance criteria:

- User can add weight, body fat and BMI.
- App calculates fat mass and lean mass.
- Dashboard shows latest measurement and trends.

## Sprint 3 — Training MVP

Goal: provide running and strength plans.

Deliverables:

- Running plan seed data: 16-week 4 km to 10 km progression
- Strength plan seed data: Push/Pull/Legs
- Training UI pages
- Completed session logging

Acceptance criteria:

- User can view this week's running sessions.
- User can view this week's strength sessions.
- User can mark sessions completed.

## Sprint 4 — Nutrition MVP

Goal: model running, strength and rest day nutrition.

Deliverables:

- Nutrition day templates
- Meal templates
- Food item database
- Macro calculation
- UI for daily plan

Acceptance criteria:

- User can switch between running/strength/rest day.
- App displays meals and macro totals.
- Whey appears as optional/complementary, not mandatory every day.

## Sprint 5 — Shopping budget

Goal: connect nutrition plan to shopping cost.

Deliverables:

- Mercadona product catalog
- Editable prices and URLs
- Weekly shopping list
- Cost per week/month
- Cost per protein source / meal category later

Acceptance criteria:

- User can edit a product price.
- Dashboard shows weekly and monthly food budget estimate.
- Shopping list can be checked off.

## Sprint 6 — Insights rules

Goal: recommend small weekly adjustments.

Deliverables:

- Weekly check-in
- Rule-based insights
- Nutrition adjustment suggestions
- Recovery warning signals

Acceptance criteria:

- App generates simple recommendations from trends.
- Recommendations explain the reason, not just the action.

## Sprint 7 — Withings integration

Goal: import body measurements automatically.

Deliverables:

- Withings OAuth flow
- token storage
- measurement import adapter
- sync job
- manual sync button

Acceptance criteria:

- User can connect Withings.
- App imports latest weight/body fat data.
- Imported data is normalized into BodyMeasurement.

## Later ideas

- Strava/Garmin import
- Home Assistant sleep/environment data
- AI weekly planning assistant
- Multi-user SaaS mode
- Public demo mode with synthetic data
- Blog series for Backend to the Future
