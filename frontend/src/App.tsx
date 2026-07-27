import { Suspense } from 'react';
import { useRoutes } from 'react-router-dom';
import { routes } from './app/routes';
import { LoadingState } from './components/LoadingState';
import { NotificationProvider } from './components/NotificationProvider';
import { ThemeProvider } from './theme/ThemeContext';
import { AuthProvider } from './auth/AuthContext';

/**
 * Root application component (FOR-81). Resolves the route table into the current
 * view. Router provider lives in main.tsx so tests can mount App inside their
 * own memory router.
 *
 * <p>Wrapped in {@link ThemeProvider} (FOR-62) here — rather than in
 * `main.tsx` — so every test that renders `<App>` inside its own router
 * automatically gets a working `useTheme()` context too, with no extra test
 * setup. {@link NotificationProvider} (FOR-63) is wired the same way, so
 * every page can call `useNotify()` for feedback toasts with no extra
 * per-test setup either.
 */
export function App() {
  const element = useRoutes(routes);
  return (
    <ThemeProvider>
      <NotificationProvider>
        <AuthProvider>
          {/*
            Outermost boundary for the code-split routes (see app/routes.tsx).
            `AppShell` has its own, so this one only catches the routes that
            render outside the application frame — onboarding and the OAuth
            callback — and acts as the backstop if a new route is added without
            one.
          */}
          <Suspense fallback={<LoadingState />}>{element}</Suspense>
        </AuthProvider>
      </NotificationProvider>
    </ThemeProvider>
  );
}
