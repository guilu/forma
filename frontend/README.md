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

The backend base URL is read through the centralized API client
(`src/api/client.ts`) from the `VITE_API_BASE_URL` environment variable and
falls back to `http://localhost:8080` (the FOR-80 backend port).

Create a local `.env` to override it:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

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
