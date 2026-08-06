import { lazy, type ComponentType } from 'react';
import type { RouteObject } from 'react-router-dom';
import { AppShell } from '../layout/AppShell';
import { RootLayout } from '../layout/RootLayout';
import { PublicNotFoundPage } from '../pages/NotFoundPage';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { LandingPage } from '../pages/LandingPage';
import { RequireAdmin } from '../auth/RequireAdmin';
import { RequireAuth } from '../auth/RequireAuth';

/**
 * Wraps a page module in `React.lazy`. The pages export by name, and `lazy`
 * wants a module whose `default` is the component, so the name is passed
 * separately rather than adding a default export to every page purely to
 * satisfy the loader.
 *
 * <p>The `import()` argument stays a literal at each call site: the bundler
 * has to read it statically to know there is a chunk to split off at all.
 */
function lazyPage<Name extends string>(
  load: () => Promise<Record<Name, ComponentType>>,
  name: Name,
) {
  return lazy(() => load().then((module) => ({ default: module[name] })));
}

/*
 * Split by first paint, not by page count.
 *
 * The public entry points (landing, login, register) stay in the initial
 * bundle: they are what an anonymous visitor loads first, and making them a
 * second round trip would trade bytes they *do* need for a slower first paint
 * and a flash of the loading fallback.
 *
 * Everything behind `/app`, plus the two mid-flow screens, is loaded on
 * demand. That is where the weight is — the charting library alone is ~110 kB
 * gzipped (ADR-013) and is reachable only from the dashboard, measurements and
 * progress pages — and it is code an anonymous visitor never runs. Rollup
 * hoists what those chunks share (Recharts included) into a chunk of its own,
 * so it is fetched once for whichever of them is opened first.
 */
const DashboardPage = lazyPage(() => import('../pages/DashboardPage'), 'DashboardPage');
const MeasurementsPage = lazyPage(() => import('../pages/MeasurementsPage'), 'MeasurementsPage');
const TrainingPage = lazyPage(() => import('../pages/TrainingPage'), 'TrainingPage');
const NutritionPage = lazyPage(() => import('../pages/NutritionPage'), 'NutritionPage');
const PlansPage = lazyPage(() => import('../pages/PlansPage'), 'PlansPage');
const ShoppingPage = lazyPage(() => import('../pages/ShoppingPage'), 'ShoppingPage');
const ProgressPage = lazyPage(() => import('../pages/ProgressPage'), 'ProgressPage');
const SettingsPage = lazyPage(() => import('../pages/SettingsPage'), 'SettingsPage');
const IntegrationsPage = lazyPage(() => import('../pages/IntegrationsPage'), 'IntegrationsPage');
const AdminPage = lazyPage(() => import('../pages/AdminPage'), 'AdminPage');
const NotFoundPage = lazyPage(() => import('../pages/NotFoundPage'), 'NotFoundPage');
const OnboardingPage = lazyPage(
  () => import('../pages/onboarding/OnboardingPage'),
  'OnboardingPage',
);
const AuthCallbackPage = lazyPage(() => import('../pages/AuthCallbackPage'), 'AuthCallbackPage');
// Perezosa, como todo lo que cuelga de /app: la portada la ve todo el mundo y el
// generador solo quien pulsa el CTA, así que su código no tiene por qué viajar con ella.
const PlanGeneratorPage = lazyPage(
  () => import('../pages/generator/PlanGeneratorPage'),
  'PlanGeneratorPage',
);

/**
 * Route table (FOR-81). Paths mirror the centralized NAV_ITEMS model.
 *
 * FOR-185 made the layout two-tier. `RootLayout` is the outer element for
 * every user-facing route: it renders the global navigation bar and, below it,
 * the routed view. `AppShell` is nested inside it for `/app`, contributing the
 * second tier (sidebar + main, or the fixed bottom bar on small screens). The
 * public landing and the auth pages render directly under `RootLayout`, which
 * is what finally gives `/login` and `/register` a navigation bar.
 *
 * `/onboarding` (FOR-59) is a deliberate exception: it is a sibling of the
 * `RootLayout` route, not a child. It is a first-run flow, not a navigation
 * section (absent from `app/navigation.ts` on purpose), and rendering it
 * outside the frame keeps a mid-flow user from wandering off via navigation.
 *
 * `/auth` (FOR-133) is the same kind of exception: the registered Withings
 * OAuth2 redirect URL (spec FOR-131) lands here mid-flow, so it renders
 * outside the frame too — no persistent navigation while the OAuth callback
 * is still resolving.
 */
export const routes: RouteObject[] = [
  {
    path: '/onboarding',
    element: (
      <RequireAuth>
        <OnboardingPage />
      </RequireAuth>
    ),
  },
  { path: '/auth', element: <AuthCallbackPage /> },
  {
    element: <RootLayout />,
    children: [
      { path: '/', element: <LandingPage /> },
      // El generador es PÚBLICO a propósito: es el embudo de la portada, y pedir
      // cuenta antes de enseñar nada lo vaciaría de sentido. Los dos endpoints que
      // hay detrás son los únicos de la API que responden sin sesión.
      { path: '/plan', element: <PlanGeneratorPage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      {
        path: '/app',
        element: (
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        ),
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'measurements', element: <MeasurementsPage /> },
          { path: 'training', element: <TrainingPage /> },
          { path: 'nutrition', element: <NutritionPage /> },
          // V53/V54: the user's own plans. NOT under /app/admin, though every
          // other editing screen is: that page is for catalogs shared by the
          // whole application, and a plan is one account's own diet.
          { path: 'nutrition/plans', element: <PlansPage /> },
          { path: 'shopping-list', element: <ShoppingPage /> },
          { path: 'progress', element: <ProgressPage /> },
          { path: 'settings', element: <SettingsPage /> },
          // FOR-57: standalone sub-route (FOR-58's Ajustes shell isn't built yet —
          // see IntegrationsPage.tsx doc comment).
          { path: 'settings/integrations', element: <IntegrationsPage /> },
          // FOR-190: admin-only catalog panel. The guard is nested here rather
          // than in the page so the chunk still loads lazily for everyone.
          {
            path: 'admin',
            element: (
              <RequireAdmin>
                <AdminPage />
              </RequireAdmin>
            ),
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
      { path: '*', element: <PublicNotFoundPage /> },
    ],
  },
];
