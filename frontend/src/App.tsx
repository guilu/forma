import { useRoutes } from 'react-router-dom';
import { routes } from './app/routes';

/**
 * Root application component (FOR-81). Resolves the route table into the current
 * view. Router provider lives in main.tsx so tests can mount App inside their
 * own memory router.
 */
export function App() {
  return useRoutes(routes);
}
