import type { RouteObject } from 'react-router-dom';
import { AppShell } from '../layout/AppShell';
import { RootLayout } from '../layout/RootLayout';
import { DashboardPage } from '../pages/DashboardPage';
import { MeasurementsPage } from '../pages/MeasurementsPage';
import { TrainingPage } from '../pages/TrainingPage';
import { NutritionPage } from '../pages/NutritionPage';
import { ShoppingPage } from '../pages/ShoppingPage';
import { ProgressPage } from '../pages/ProgressPage';
import { GoalsPage } from '../pages/GoalsPage';
import { SettingsPage } from '../pages/SettingsPage';
import { IntegrationsPage } from '../pages/IntegrationsPage';
import { OnboardingPage } from '../pages/onboarding/OnboardingPage';
import { AuthCallbackPage } from '../pages/AuthCallbackPage';
import { NotFoundPage, PublicNotFoundPage } from '../pages/NotFoundPage';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { LandingPage } from '../pages/LandingPage';
import { RequireAuth } from '../auth/RequireAuth';

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
          { path: 'shopping-list', element: <ShoppingPage /> },
          { path: 'progress', element: <ProgressPage /> },
          { path: 'goals', element: <GoalsPage /> },
          { path: 'settings', element: <SettingsPage /> },
          // FOR-57: standalone sub-route (FOR-58's Ajustes shell isn't built yet —
          // see IntegrationsPage.tsx doc comment).
          { path: 'settings/integrations', element: <IntegrationsPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
      { path: '*', element: <PublicNotFoundPage /> },
    ],
  },
];
