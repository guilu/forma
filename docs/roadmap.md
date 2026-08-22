# Forma roadmap

This file is a **record of what has been built**, not a plan of what is coming. When it disagrees with the repository, the repository is right — see `AGENTS.md`.

Last reviewed against the repository: 2026-08-21, at `main` = PR #240.

## How work is tracked

Two eras, and the boundary is sharp.

**Through PR #170 (2026-07-26) — Jira.** Stories carried a `FOR-XXX` key, branches and PR titles led with it, and each story had a spec folder. Those specs are still in `specs/` (125 of them, `FOR-15` .. `FOR-185`) and are still worth reading for the areas they cover: they explain decisions the code alone does not.

**From PR #171 (2026-07-27) onward — the pull request is the unit.** Keeping Jira in sync stopped paying for itself. There is no `FOR-XXX` key on this work and no new spec folder; the PR description carries the intent, the scope and what was deliberately left out, and the commit messages carry the reasoning.

This means: **do not go looking for a Jira story for recent work — there isn't one, and its absence is not a gap.** Do not create spec folders for new work unless asked. Write the reasoning into the PR and the commits instead.

## Sprints 0–7 — delivered

The original plan. All of it shipped; the evidence column is where to look if you need to confirm something rather than trust this table.

| Sprint | Goal | Evidence |
|---|---|---|
| 0 · Product foundation | Make the repository understandable | `README.md`, `docs/vision.md`, `docs/architecture-overview.md`, `docs/domain-model.md`, `docs/ui-guidelines.md`, `docs/prompts/`, `docs/backlog.md` |
| 1 · Technical bootstrap | Runnable skeleton | `backend/`, `frontend/`, `compose.yaml`, `.github/workflows/ci.yml` |
| 2 · Body dashboard | Manual measurements + trends | `frontend/src/pages/MeasurementsPage.tsx`, `frontend/src/pages/DashboardPage.tsx`, `backend/src/main/resources/db/migration/V2__body_measurements.sql` |
| 3 · Training | Running and strength plans | `frontend/src/pages/TrainingPage.tsx`, `backend/src/main/resources/db/migration/V3__training_session_status.sql` |
| 4 · Nutrition | Day templates, meals, macros | `frontend/src/pages/NutritionPage.tsx`, `frontend/src/pages/nutrition/` |
| 5 · Shopping budget | Plan to shopping cost | `frontend/src/pages/ShoppingPage.tsx`, `backend/src/main/resources/db/migration/V4__shopping_products.sql`, `backend/src/main/resources/db/migration/V5__shopping_lists.sql` |
| 6 · Insights | Rule-based weekly recommendations | `backend/src/main/java/dev/diegobarrioh/forma/delivery/insights/WeeklyInsightsResponse.java`, `backend/src/main/resources/db/migration/V10__insight_history.sql` |
| 7 · Withings | Automatic measurement import | `backend/src/main/java/dev/diegobarrioh/forma/adapter/withings/`, `frontend/src/pages/integrations/IntegrationsSection.tsx` |

Two corrections to what the plan said:

- Sprint 1 promised `infra/docker-compose.yml`. The file is **`compose.yaml`, at the repository root**; there is no `infra/` directory.
- Authentication and multi-user isolation were listed as a *later idea*. They shipped — `docs/adr/ADR-012-authentication-and-multi-user-isolation.md`, `frontend/src/auth/`, and seven migrations adding `user_id`. Do not build them again.

## After Sprint 7

The product kept going for three months past the last sprint in the plan. These are the areas that grew, with the PR ranges to read if you need the reasoning.

**Admin and the real catalog** (#183–#199, #206–#215) — the largest body of work here, and none of it was in the plan. An admin role; a global food catalog read from the database instead of compiled into Java; a store product catalog with barcodes, sizes and availability; Mercadona import, including its published aisles; food groups, tags, servings and equivalences as data; recipes.

**Nutrition, made real** (#192–#221, #226, #229) — meal logging, a plan-following model, a JSON plan format an LLM can write, the real diet imported from a spreadsheet, and a run of fixes replacing invented numbers with endpoints that already existed.

**Public funnel and plan activation** (#223–#225, #228) — a four-step plan generator on the landing page, an activation modal with empty states that lead somewhere, and a shopping list generated from the active week.

**Training** (#232–#239) — muscle silhouettes lit from the worked-muscle map, the week as the page with no scroll, and the session detail redesigned into two columns with the real exercise breakdown.

**Design system and app-wide UI** (#171–#182, #231, #238) — Playwright layout checks, on-demand route loading, routes renamed to English, unified buttons/icon buttons/chips, and errors moved into the same notification lane as successes.

**Accounts** (#222) — seeded data moved onto the real account.

One retreat worth knowing about: **goals** were withdrawn from the frontend in #181 and the **backend was kept**. `frontend/src/components/statusLabels.ts` still carries `goalStatus` tones. If you bring goals back, the API is there.

## Not built

- Strava / Garmin import. Note that `LandingPage.tsx:185` already promises wearable integration to visitors — marketing ahead of product.
- Home Assistant sleep/environment data.
- Public demo mode with synthetic data. `npm run dev:fixtures` is the nearest thing and is development-only.
- Blog series for Backend to the Future.
