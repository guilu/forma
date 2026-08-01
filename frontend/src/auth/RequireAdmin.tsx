import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { PagePlaceholder } from '../components/PagePlaceholder';
import { useAuth } from './AuthContext';

/**
 * Renders `children` only for users whose role is ADMIN (FOR-190).
 *
 * <p>Sits *inside* {@link RequireAuth}, which has already resolved the session —
 * by the time this runs there is a user, so there is no loading branch to
 * repeat. It shows a refusal instead of redirecting: a silent bounce to the
 * dashboard reads as a broken link to an admin whose role failed to load, while
 * this states plainly what happened.
 *
 * <p>Cosmetic only. Every admin endpoint is guarded by `@PreAuthorize` on the
 * backend, so a user who edits their client state still gets 403 from the API.
 */
export function RequireAdmin({ children }: { readonly children: ReactNode }) {
  const auth = useAuth();

  if (auth.user?.role !== 'ADMIN') {
    return (
      <div>
        <PagePlaceholder
          title="No tienes acceso a esta sección"
          description="El panel de administración está reservado a las cuentas con rol de administrador."
        />
        <p style={{ marginTop: 'var(--space-4)' }}>
          <Link to="/app">Volver al Dashboard</Link>
        </p>
      </div>
    );
  }
  return children;
}
