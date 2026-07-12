import { useEffect, useState } from 'react';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Icon, type IconName } from '../../components/Icon';
import { LoadingState } from '../../components/LoadingState';
import { Modal } from '../../components/Modal';
import { StatusPill } from '../../components/StatusPill';
import { ApiRequestError } from '../../api/client';
import {
  connectIntegration,
  disconnectIntegration,
  listIntegrations,
  syncIntegration,
  type IntegrationConnection,
  type IntegrationProviderId,
} from '../../api/integrations';
import styles from './IntegrationsSection.module.css';

/**
 * Integrations management section (FOR-57): connected + available provider
 * lists, connect/disconnect entry points, manual sync and safe error display.
 * Mockup: "CONEXIONES E INTEGRACIONES" in `docs/8-configuracion.png` (Withings
 * connected; Google Fit / Apple Health not connected).
 *
 * <p><b>No integrations backend exists yet</b> (verified: no controller under
 * `backend/src/main/java`, see `frontend/src/api/integrations.ts` doc
 * comment). This section reads a documented mock list, and every mutating
 * action (connect/disconnect/sync) surfaces a clear, safe "not available yet"
 * message instead of pretending to succeed — the entry points are real and
 * wired, but honestly non-functional until the External Integrations epic
 * (FOR-103) ships a backend. `StatusPill`'s `connection` kind and `Card` are
 * reused from FOR-50; this is a self-contained section so it does not depend
 * on FOR-58's (not-yet-built) Ajustes shell — it is mounted from its own
 * route, `/ajustes/integraciones`.
 *
 * <p>Never renders a token, secret or other credential field — the read model
 * itself ({@link IntegrationConnection}) carries none (ADR-004).
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly connections: IntegrationConnection[] };

const PROVIDER_ICONS: Record<IntegrationProviderId, IconName> = {
  WITHINGS: 'heart',
  GOOGLE_FIT: 'activity',
  APPLE_HEALTH: 'cross',
};

const LOAD_ERROR = 'No se pudieron cargar tus integraciones. Inténtalo de nuevo más tarde.';

const lastSyncFormatter = new Intl.DateTimeFormat('es-ES', {
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
});

function formatLastSync(iso: string | undefined): string | undefined {
  if (!iso) return undefined;
  return lastSyncFormatter.format(new Date(iso));
}

function messageFromError(error: unknown, fallback: string): string {
  return error instanceof ApiRequestError ? error.message : fallback;
}

export function IntegrationsSection() {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [retryToken, setRetryToken] = useState(0);
  const [actionError, setActionError] = useState<string | undefined>(undefined);
  const [pendingProviderId, setPendingProviderId] = useState<IntegrationProviderId | undefined>(
    undefined,
  );
  const [disconnectTarget, setDisconnectTarget] = useState<IntegrationConnection | undefined>(
    undefined,
  );

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    listIntegrations()
      .then((connections) => {
        if (active) {
          setState({ status: 'ready', connections });
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, [retryToken]);

  async function handleConnect(provider: IntegrationConnection) {
    setActionError(undefined);
    setPendingProviderId(provider.providerId);
    try {
      await connectIntegration(provider.providerId);
    } catch (error) {
      setActionError(messageFromError(error, `No se pudo conectar con ${provider.providerName}.`));
    } finally {
      setPendingProviderId(undefined);
    }
  }

  async function handleSync(provider: IntegrationConnection) {
    setActionError(undefined);
    setPendingProviderId(provider.providerId);
    try {
      await syncIntegration(provider.providerId);
    } catch (error) {
      setActionError(messageFromError(error, `No se pudo sincronizar ${provider.providerName}.`));
    } finally {
      setPendingProviderId(undefined);
    }
  }

  async function handleDisconnectConfirm() {
    if (!disconnectTarget) return;
    setActionError(undefined);
    setPendingProviderId(disconnectTarget.providerId);
    try {
      await disconnectIntegration(disconnectTarget.providerId);
    } catch (error) {
      setActionError(
        messageFromError(error, `No se pudo desconectar ${disconnectTarget.providerName}.`),
      );
    } finally {
      setPendingProviderId(undefined);
      setDisconnectTarget(undefined);
    }
  }

  return (
    <section className={styles.wrapper} aria-labelledby="integrations-section-title">
      <h2 id="integrations-section-title" className={styles.title}>
        Conexiones e integraciones
      </h2>

      {actionError && (
        <p className={styles.actionError} role="alert">
          {actionError}
        </p>
      )}

      {renderContent(state, pendingProviderId, handleConnect, handleSync, setDisconnectTarget, () =>
        setRetryToken((t) => t + 1),
      )}

      {disconnectTarget && (
        <Modal
          title={`Desconectar ${disconnectTarget.providerName}`}
          onClose={() => setDisconnectTarget(undefined)}
        >
          <p className={styles.confirmText}>
            ¿Seguro que quieres desconectar {disconnectTarget.providerName}? Podrás volver a
            conectarlo cuando quieras.
          </p>
          <div className={styles.confirmActions}>
            <Button
              variant="secondary"
              type="button"
              onClick={() => setDisconnectTarget(undefined)}
            >
              Cancelar
            </Button>
            <Button
              variant="destructive"
              type="button"
              loading={pendingProviderId === disconnectTarget.providerId}
              onClick={handleDisconnectConfirm}
            >
              Desconectar
            </Button>
          </div>
        </Modal>
      )}
    </section>
  );
}

function renderContent(
  state: State,
  pendingProviderId: IntegrationProviderId | undefined,
  onConnect: (provider: IntegrationConnection) => void,
  onSync: (provider: IntegrationConnection) => void,
  onRequestDisconnect: (provider: IntegrationConnection) => void,
  onRetry: () => void,
) {
  if (state.status === 'loading') {
    return <LoadingState message="Cargando tus integraciones…" />;
  }

  if (state.status === 'error') {
    return <ErrorState message={LOAD_ERROR} onRetry={onRetry} />;
  }

  const connected = state.connections.filter((c) => c.status === 'CONNECTED');
  const available = state.connections.filter((c) => c.status === 'NOT_CONNECTED');

  return (
    <div className={styles.content}>
      <Card title="Conectadas">
        {connected.length === 0 ? (
          <EmptyState variant="filtered" title="Aún no tienes integraciones conectadas." />
        ) : (
          <ul className={styles.list}>
            {connected.map((provider) => (
              <li key={provider.providerId} className={styles.row}>
                <Icon name={PROVIDER_ICONS[provider.providerId]} />
                <div className={styles.info}>
                  <span className={styles.name}>{provider.providerName}</span>
                  <span className={styles.description}>{provider.description}</span>
                  {provider.lastSyncAt && (
                    <span className={styles.lastSync}>
                      Última sincronización: {formatLastSync(provider.lastSyncAt)}
                    </span>
                  )}
                </div>
                <StatusPill kind="connection" value="Conectado" />
                <div className={styles.actions}>
                  <Button
                    variant="secondary"
                    type="button"
                    loading={pendingProviderId === provider.providerId}
                    onClick={() => onSync(provider)}
                  >
                    Sincronizar ahora
                  </Button>
                  <Button
                    variant="ghost"
                    type="button"
                    disabled={pendingProviderId === provider.providerId}
                    onClick={() => onRequestDisconnect(provider)}
                  >
                    Desconectar
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card title="Disponibles">
        {available.length === 0 ? (
          <EmptyState variant="filtered" title="No hay más integraciones disponibles." />
        ) : (
          <ul className={styles.list}>
            {available.map((provider) => (
              <li key={provider.providerId} className={styles.row}>
                <Icon name={PROVIDER_ICONS[provider.providerId]} />
                <div className={styles.info}>
                  <span className={styles.name}>{provider.providerName}</span>
                  <span className={styles.description}>{provider.description}</span>
                </div>
                <StatusPill kind="connection" value="No conectado" />
                <div className={styles.actions}>
                  <Button
                    variant="primary"
                    type="button"
                    loading={pendingProviderId === provider.providerId}
                    onClick={() => onConnect(provider)}
                  >
                    Conectar
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
