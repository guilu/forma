import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { useAuth } from './AuthContext';

export function RequireAuth({ children }: { readonly children: ReactNode }) {
  const auth = useAuth();
  const location = useLocation();

  if (auth.bootstrapError) {
    return (
      <ErrorState
        title="No pudimos comprobar tu sesión"
        message="Comprueba tu conexión e inténtalo de nuevo."
        onRetry={() => void auth.refreshCurrentUser()}
      />
    );
  }
  if (auth.status === 'loading') {
    return <LoadingState message="Comprobando tu sesión..." />;
  }
  if (auth.status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return children;
}
