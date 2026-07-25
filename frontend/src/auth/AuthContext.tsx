import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  type AuthCredentials,
  type AuthUser,
} from '../api/auth';
import { ApiRequestError } from '../api/client';

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

interface AuthState {
  readonly status: AuthStatus;
  readonly user: AuthUser | null;
  readonly bootstrapError: boolean;
  readonly login: (credentials: AuthCredentials) => Promise<void>;
  readonly register: (credentials: AuthCredentials) => Promise<void>;
  readonly logout: () => Promise<void>;
  readonly refreshCurrentUser: () => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { readonly children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<AuthUser | null>(null);
  const [bootstrapError, setBootstrapError] = useState(false);
  const bootstrapStarted = useRef(false);
  const authGeneration = useRef(0);

  const refreshCurrentUser = useCallback(async () => {
    const generation = authGeneration.current;
    setStatus('loading');
    setBootstrapError(false);
    try {
      const currentUser = await getCurrentUser();
      if (generation !== authGeneration.current) return;
      setUser(currentUser);
      setStatus('authenticated');
    } catch (error) {
      if (generation !== authGeneration.current) return;
      setUser(null);
      if (error instanceof ApiRequestError && error.status === 401) {
        setStatus('anonymous');
      } else {
        setStatus('loading');
        setBootstrapError(true);
      }
    }
  }, []);

  useEffect(() => {
    if (bootstrapStarted.current) return;
    bootstrapStarted.current = true;
    void refreshCurrentUser();
  }, [refreshCurrentUser]);

  const login = useCallback(async (credentials: AuthCredentials) => {
    const currentUser = await loginRequest(credentials);
    authGeneration.current += 1;
    setUser(currentUser);
    setBootstrapError(false);
    setStatus('authenticated');
  }, []);

  const register = useCallback(async (credentials: AuthCredentials) => {
    await registerRequest(credentials);
    const currentUser = await loginRequest(credentials);
    authGeneration.current += 1;
    setUser(currentUser);
    setBootstrapError(false);
    setStatus('authenticated');
  }, []);

  const logout = useCallback(async () => {
    authGeneration.current += 1;
    await logoutRequest();
    setUser(null);
    setBootstrapError(false);
    setStatus('anonymous');
  }, []);

  const value = useMemo(
    () => ({ status, user, bootstrapError, login, register, logout, refreshCurrentUser }),
    [status, user, bootstrapError, login, register, logout, refreshCurrentUser],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
}
