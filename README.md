<p align="center">
  <img src="docs/assets/forma-header.png" alt="forma — Medidas. Entrenos. Nutrición." width="100%">
</p>
<p align="center">
  <img src="docs/screenshots/dashboard-light.png" alt="Dashboard (light)" width="49%">
  <img src="docs/screenshots/dashboard-dark.png" alt="Dashboard (dark)" width="49%">
</p>
<p align="center">
  A personal operating system for training and nutrition: body composition, running, strength, meal planning and the weekly shopping bill — in one place, with the numbers actually connected to each other.
</p>

<p align="center">
  <a href="https://forma.backendtothefuture.com">▶️ Live app</a>
  •
  <a href="https://github.com/guilu/forma">📦 Repository</a>
  •
  <a href="AGENTS.md">🧭 State of the repo</a>
</p>

<p align="center">
  <a href="https://github.com/guilu/forma/stargazers"><img src="https://img.shields.io/github/stars/guilu/forma?style=flat&color=yellow" alt="Stars"></a>
  <a href="https://github.com/guilu/forma/commits/main"><img src="https://img.shields.io/github/last-commit/guilu/forma?color=blue" alt="Last commit"></a>
  <img src="https://img.shields.io/badge/java-21-red" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F" alt="Spring Boot 3.3.5">
  <img src="https://img.shields.io/badge/postgres-17-4169E1" alt="PostgreSQL 17">
  <img src="https://img.shields.io/badge/react-19-61DAFB" alt="React 19">
  <img src="https://img.shields.io/badge/vite-6-646CFF" alt="Vite 6">
</p>

---

## ✨ Features

- 📊 **Dashboard** that puts body composition, the day's training, macros and the shopping preview on one screen
- ⚖️ **Body measurements** — weight, fat, muscle and water, entered by hand or pulled from a Withings scale
- 🏃 **Training** — running and strength weeks, with a session detail that shows which muscles each exercise actually works, primary and secondary
- 🥗 **Nutrition** — daily macros, meals, and your own diet plans
- 🛒 **Shopping list** generated from the active week, priced against a Mercadona product catalogue
- 📈 **Progress** — evolution charts, photos and rule-based weekly recommendations
- 🧭 **Public plan generator** — a four-step funnel on the landing page that works without an account
- 🔐 **Auth** — email and password, or Google OAuth2
- 🔌 **Withings integration** over OAuth2, with tokens encrypted at rest
- 🌗 Light and dark themes, and a layout that works on a phone
- 🛡️ **Admin panel** for the shared catalogues, gated by role

---

## 📸 Screenshots

### Landing and plan generator

<p align="center">
  <img src="docs/screenshots/landing-light.png" width="49%">
  <img src="docs/screenshots/landing-dark.png" width="49%">
</p>
<p align="center">
  <img src="docs/screenshots/plan-generator-step-1-light.png" width="49%">
  <img src="docs/screenshots/plan-generator-step-1-dark.png" width="49%">
</p>

### Dashboard

<p align="center">
  <img src="docs/screenshots/dashboard-light.png" width="49%">
  <img src="docs/screenshots/dashboard-dark.png" width="49%">
</p>

### Body measurements

<p align="center">
  <img src="docs/screenshots/measurements-light.png" width="49%">
  <img src="docs/screenshots/measurements-dark.png" width="49%">
</p>

### Training

<p align="center">
  <img src="docs/screenshots/training-light.png" width="49%">
  <img src="docs/screenshots/training-dark.png" width="49%">
</p>

The session detail is the screen the redesign was for — two columns, the real exercise breakdown, and a muscle map that distinguishes the muscles doing the work from the ones assisting:

<p align="center">
  <img src="docs/screenshots/training-session-detail-light.png" width="49%">
  <img src="docs/screenshots/training-session-detail-dark.png" width="49%">
</p>

### Nutrition and shopping

<p align="center">
  <img src="docs/screenshots/nutrition-dark.png" width="49%">
  <img src="docs/screenshots/nutrition-plans-dark.png" width="49%">
</p>

![Shopping list](docs/screenshots/shopping-list-dark.png)

### Progress and settings

<p align="center">
  <img src="docs/screenshots/progress-light.png" width="49%">
  <img src="docs/screenshots/progress-dark.png" width="49%">
</p>

![Settings](docs/screenshots/settings-dark.png)

### Mobile

<p align="center">
  <img src="docs/screenshots/mobile-dashboard-light.png" width="24%">
  <img src="docs/screenshots/mobile-dashboard-dark.png" width="24%">
  <img src="docs/screenshots/mobile-training-dark.png" width="24%">
</p>

---

## 🏗️ Tech Stack

### Backend

| Piece | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.5 |
| Gradle | 8.14.1 (wrapper, at `backend/gradlew`) |
| PostgreSQL | 17 |
| Flyway | migrations `V1` → `V65` |
| Spotless | google-java-format 1.23.0 |

### Frontend

| Piece | Version |
|---|---|
| React | 19.1.0 |
| React Router | 7.1.1 |
| Vite | 6.0.5 |
| TypeScript | 5.7.2 |
| Recharts | 3.10.1 |
| Vitest | 3.0.0 |
| Playwright | 1.62.0 |

> [!NOTE]
> **The styling is CSS Modules, not Tailwind.** The HTML mockups under `docs/` carry a `tailwind.config` token block, but those are a reference to translate, not a dependency to adopt — the tokens were folded into `frontend/src/styles/theme.css`, which is the single source of truth. Fonts are self-hosted via `@fontsource`. No Tailwind, no CDN fonts.

### Infrastructure

- Docker Compose — Postgres, backend and frontend
- nginx in front of the frontend image, reverse-proxying the API
- GitHub Actions, four jobs

---

## 🚀 Quick Start

```bash
cp env.example .env     # then fill in what you need
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | <http://localhost:3000> |
| API | <http://localhost:8080> |
| Postgres | `localhost:5432` |

### Frontend without Docker

The signed-in app needs an API, so a plain `npm run dev` only gets you the landing page. For everything behind the login there is a fixtures mode that serves the whole API from the dev server:

```bash
cd frontend
npm run dev:fixtures    # http://localhost:5173, no backend, no Docker
```

That is also how the screenshots in this README were taken.

---

## 🧱 Backend Architecture

Hexagonal, and the package names say so:

```text
backend/src/main/java/dev/diegobarrioh/forma/
├── domain/        entities and value objects, framework-free
├── application/   use cases (*Service) and the ports they need (*Repository)
├── adapter/
│   ├── persistence/   JDBC implementations of the ports
│   ├── withings/      OAuth2 + measurement gateway
│   ├── mercadona/     product catalogue import
│   ├── planagent/     external AI plan generation (see Status below)
│   ├── storage/, link/, scheduling/, logging/
├── delivery/      32 REST controllers across 20 feature packages
├── bootstrap/     pre-auth placeholder account
└── config/        security, CORS, crypto, scheduling
```

The dependency rule is enforced by review, not by tooling: `domain` imports nothing from Spring, `application` depends on ports it declares itself, and only `adapter` knows what a database or an HTTP client is.

Frontend routing is in `frontend/src/app/routes.tsx`, with two shells — a public one and the signed-in `AppShell` — and lazy loading per route.

---

## 🔐 Security

- Session authentication with CSRF protection; the client seeds the token from `/actuator/health` before any unsafe method
- Google OAuth2 login, enabled only when both client credentials are present
- Withings tokens encrypted at rest (`WITHINGS_TOKEN_ENC_KEY`)
- CORS origins listed explicitly, never `*`
- Role-gated admin routes, guarded in the route table so the chunk still loads lazily for everyone
- Forwarded headers handled for a double reverse proxy — there is a dedicated CI job that proves it

---

## ⚙️ Environment Variables

```env
# Database
POSTGRES_DB=
POSTGRES_USER=
POSTGRES_PASSWORD=

# API
FORMA_CORS_ALLOWED_ORIGINS=

# Withings (OAuth2 — the redirect URI must match the one registered)
WITHINGS_CLIENT_ID=
WITHINGS_CLIENT_SECRET=
WITHINGS_REDIRECT_URI=
WITHINGS_TOKEN_ENC_KEY=

# Login with Google
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# External plan agent (optional — empty means inert, not broken)
FORMA_PLAN_AGENT_BASE_URL=
FORMA_PLAN_AGENT_API_KEY=
FORMA_PLAN_AGENT_TIMEOUT_SECONDS=120
```

Full table with defaults and which service reads each one: [`docs/configuration.md`](docs/configuration.md).

> [!WARNING]
> Setting a variable in `.env` is only half the job. Every one of these must also appear under the backend service's `environment:` in `compose.yaml` — Compose reads `.env` to interpolate into that file, it does not forward the file into the container. A variable present in `.env` but missing from that block simply does not exist inside the JVM.

---

## 🧪 Tests

```bash
cd backend  && ./gradlew check      # unit + integration + spotless
cd frontend && npm test             # vitest
cd frontend && npm run test:layout  # Playwright layout checks
```

| Suite | Tests |
|---|---|
| Backend (`./gradlew test`) | 1676 |
| Frontend unit (`vitest`) | 1229 |
| Frontend layout (Playwright) | 3 spec files |

Two things worth knowing:

**`./gradlew test` is not what CI runs.** CI runs the full build, which includes `spotlessJavaCheck`. Use `./gradlew check` locally or you will discover formatting failures on the PR instead of on your machine.

**The layout checks exist because jsdom performs no layout.** It cannot tell you that an element overflows its container, that two scrollbars appeared, or that a fill is not actually translucent. Each of those shipped as a visual bug that only a real browser could have caught. `RealPostgresMigrationTest` is tagged `postgres` and excluded from the default task; a separate CI job runs it against a real Postgres 17.

---

## 🚚 Deployment

Deployed at [forma.backendtothefuture.com](https://forma.backendtothefuture.com) behind a public nginx that proxies to the Compose stack.

```bash
docker compose up -d --build
```

Both images are multi-stage and run as a non-root user: `eclipse-temurin:21-jre-alpine` for the backend, `nginxinc/nginx-unprivileged:alpine` for the frontend.

---

## 📋 Status

What is honest about the current state, so you do not go looking for it:

| Thing | State |
|---|---|
| Withings | The only integration that exists. Body composition measurements only |
| Strava, Garmin, Apple Health | **Not built.** Not planned as of today |
| Goals | Backend is complete and tested; the frontend was withdrawn in #181. The API is there if it comes back |
| AI plan agent | Wired end to end, but inert unless `FORMA_PLAN_AGENT_BASE_URL` is set |
| Demo data | The app starts empty by design (`FOR-169`). `npm run dev:fixtures` is the nearest thing to a demo, and it is development-only |

The landing page keeps a written record, in a comment at the top of `LandingPage.tsx`, of every claim that used to be on it and was removed for not being true. It is worth reading if you ever write marketing copy for something you also have to maintain.

---

## 📚 Documentation

| Doc | What it covers |
|---|---|
| [`AGENTS.md`](AGENTS.md) | The state of the repo. Start here |
| [`docs/local-development.md`](docs/local-development.md) | Running the stack locally |
| [`docs/configuration.md`](docs/configuration.md) | Every environment variable and where it is read |
| [`docs/architecture.md`](docs/architecture.md) | Layers, dependency rule, topology |
| [`docs/domain-model.md`](docs/domain-model.md) | Entities and their relationships |
| [`docs/api-conventions.md`](docs/api-conventions.md) | REST conventions |
| [`docs/api/`](docs/api/) | Per-resource API references |
| [`docs/adr/`](docs/adr/) | 16 architecture decision records |
| [`docs/ui-guidelines.md`](docs/ui-guidelines.md) | Design tokens and component rules |
| [`docs/roadmap.md`](docs/roadmap.md) | What was built, what was withdrawn, what is not built |

---

## 💖 Apoyar el proyecto

Forma es un proyecto personal que mantengo en mi tiempo libre. Si te resulta útil o te ha enseñado algo:

- ⭐ Dale una estrella al repo — es gratis y ayuda muchísimo a la visibilidad
- 💛 [Conviérteme en sponsor en GitHub](https://github.com/sponsors/guilu) — soporte recurrente
- ☕ [Invítame a un café](https://buymeacoffee.com/diegobarrioh) — donación puntual
- ₿ Bitcoin on-chain (SegWit):

  ```text
  bc1qeezmht3rweypgk7a5n9uz52j52r6snfzq8e2ml
  ```

- 🐛 Abre issues o PRs con bugs, ideas o mejoras

---

## 👤 Autor

**Diego Barrio** · [diegobarrioh.dev](https://diegobarrioh.dev) · [LinkedIn](https://www.linkedin.com/in/diegobarrioh) · [GitHub](https://github.com/guilu)
