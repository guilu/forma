import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { useMountedRef } from '../hooks/useMountedRef';
import { listIntegrations, type IntegrationConnection } from '../api/integrations';

/**
 * One owner of the integration connection list, shared by every view of it
 * (FOR-189).
 *
 * <p>The sidebar's status card and the settings section each used to fetch the
 * list themselves, once, on mount. Two copies of one truth: disconnecting in
 * settings left the sidebar claiming the provider was still connected until a
 * full page reload — and the section did not re-read its own list after a
 * mutation either, so even alone it was showing a stale answer.
 *
 * <p>Mutations call {@link IntegrationsState.refresh} rather than patching a
 * local copy: connect, disconnect and sync all change server state in ways the
 * response does not fully describe (a sync updates `lastSyncAt`, a connect may
 * or may not complete), so re-reading is both simpler and honest.
 */
export interface IntegrationsState {
  readonly status: 'loading' | 'error' | 'ready';
  readonly connections: readonly IntegrationConnection[];
  /** Re-reads the list from the API. Safe to call from any consumer. */
  readonly refresh: () => void;
}

const IntegrationsContext = createContext<IntegrationsState | undefined>(undefined);

export function IntegrationsProvider({ children }: { readonly children: ReactNode }) {
  const [status, setStatus] = useState<IntegrationsState['status']>('loading');
  const [connections, setConnections] = useState<readonly IntegrationConnection[]>([]);
  const mountedRef = useMountedRef();

  const refresh = useCallback(() => {
    listIntegrations()
      .then((next) => {
        if (!mountedRef.current) return;
        setConnections(next);
        setStatus('ready');
      })
      .catch(() => {
        if (mountedRef.current) setStatus('error');
      });
  }, [mountedRef]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return (
    <IntegrationsContext.Provider value={{ status, connections, refresh }}>
      {children}
    </IntegrationsContext.Provider>
  );
}

/**
 * Outside a provider: a permanent "loading" with a no-op refresh rather than a
 * thrown error. The consumers are a status card and a settings section, and
 * neither is worth crashing a screen over — the card simply does not render.
 */
const FALLBACK: IntegrationsState = {
  status: 'loading',
  connections: [],
  refresh: () => {},
};

/** The shared list. See {@link IntegrationsProvider}. */
// eslint-disable-next-line react-refresh/only-export-components
export function useIntegrations(): IntegrationsState {
  return useContext(IntegrationsContext) ?? FALLBACK;
}
