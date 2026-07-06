# FORMA Frontend

Frontend application skeleton for FORMA (story **FOR-81**). It provides the
application shell — project structure, routing, layout, navigation, theme and a
reusable UI foundation — with **no product functionality**. Every section renders
a placeholder until its owning story is implemented.

## Stack

- [Vite](https://vitejs.dev/) 6 + React 19 + TypeScript
- [React Router](https://reactrouter.com/) 7 for routing
- [Vitest](https://vitest.dev/) + Testing Library for tests
- Package manager: **npm** (selected by FOR-81)

## Requirements

- Node.js 20+ (developed on Node 24). npm 10+.

## Commands

```bash
npm install       # install dependencies
npm run dev       # start the dev server (http://localhost:5173)
npm run build     # type-check and produce a production build in dist/
npm run preview   # preview the production build
npm run typecheck # type-check only
npm test          # run the test suite once
npm run test:watch
```

## Configuration

The app calls the API using **relative `/api/...` paths (same-origin)** through
the centralized API client (`src/api/client.ts`), so no backend host is baked
into the production bundle:

- **Production (Docker):** the frontend's nginx reverse-proxies `/api/` to the
  backend container (`frontend/nginx.conf`).
- **Dev (`npm run dev`):** the Vite dev server proxies `/api` to the local
  backend (`vite.config.ts`, `http://localhost:8080`). Change the target there
  if the backend runs elsewhere.

Only set `VITE_API_BASE_URL` (an absolute URL) if the API is served from a
genuinely different origin than the SPA; otherwise leave it empty.

## Structure

```text
src/
  app/          # centralized navigation model + route table
  layout/       # AppShell, Sidebar, Topbar, MobileNav
  components/   # reusable UI foundation (Card, Brand, Icon, PagePlaceholder)
  pages/        # one placeholder page per navigation section
  api/          # centralized API client boundary (placeholder)
  styles/       # design tokens (theme.css) + global baseline
  test/         # test setup
```

## Adding a screen (later stories)

1. Add a `NavItem` to `src/app/navigation.ts`.
2. Add a route to `src/app/routes.tsx`.
3. Replace the placeholder page with the real one, composing shared components.

The frontend renders read models and collects commands only; domain rules stay
in the backend (see `docs/adr/ADR-006-frontend.md`).
