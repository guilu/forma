import type { RouteObject } from 'react-router-dom';
import { AppShell } from '../layout/AppShell';
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
import { NotFoundPage } from '../pages/NotFoundPage';

/**
 * Route table (FOR-81). Paths mirror the centralized NAV_ITEMS model and all
 * render inside the AppShell layout. Every element is a placeholder page; adding
 * a real screen later means swapping the element, not restructuring routing.
 *
 * `/onboarding` (FOR-59) is a deliberate exception: it is a sibling of the
 * `AppShell` route, not a child. It is a first-run flow, not a navigation
 * section (absent from `app/navigation.ts` on purpose), and rendering it
 * outside the shell keeps a mid-flow user from wandering off via the sidebar.
 *
 * `/auth` (FOR-133) is the same kind of exception: the registered Withings
 * OAuth2 redirect URL (spec FOR-131) lands here mid-flow, so it renders
 * outside `AppShell` too — no persistent sidebar/nav while the OAuth
 * callback is still resolving.
 */
export const routes: RouteObject[] = [
  { path: '/onboarding', element: <OnboardingPage /> },
  { path: '/auth', element: <AuthCallbackPage /> },
  {
    path: '/',
    element: <AppShell />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'mediciones', element: <MeasurementsPage /> },
      { path: 'entrenamiento', element: <TrainingPage /> },
      { path: 'nutricion', element: <NutritionPage /> },
      { path: 'lista-compra', element: <ShoppingPage /> },
      { path: 'progreso', element: <ProgressPage /> },
      { path: 'objetivos', element: <GoalsPage /> },
      { path: 'ajustes', element: <SettingsPage /> },
      // FOR-57: standalone sub-route (FOR-58's Ajustes shell isn't built yet —
      // see IntegrationsPage.tsx doc comment).
      { path: 'ajustes/integraciones', element: <IntegrationsPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
