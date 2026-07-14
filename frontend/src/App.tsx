import { useRoutes } from 'react-router-dom';
import { routes } from './app/routes';
import { NotificationProvider } from './components/NotificationProvider';
import { ThemeProvider } from './theme/ThemeContext';

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
      <NotificationProvider>{element}</NotificationProvider>
    </ThemeProvider>
  );
}
