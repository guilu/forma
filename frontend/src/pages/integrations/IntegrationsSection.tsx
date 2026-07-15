import { useEffect, useState } from 'react';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { Icon, type IconName } from '../../components/Icon';
import { LoadingState } from '../../components/LoadingState';
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
 * <p>FOR-63 adds a shared {@link ConfirmDialog} for the disconnect
 * confirmation, replacing the ad-hoc modal markup this section used to build
 * directly on `Modal`. It deliberately does <b>not</b> add a
 * success-toast branch for connect/sync/disconnect: `connectIntegration`,
 * `syncIntegration` and `disconnectIntegration` (`frontend/src/api/
 * integrations.ts`) are typed `Promise&lt;never&gt;` and always reject by
 * design until FOR-103 ships a backend, so a success path here would be
 * unreachable dead code today (AGENTS.md: "document as planned instead of
 * creating it early"). The FOR-63 success-notification pattern is instead
 * demonstrated on flows with a real, currently-succeeding API — see
 * `TrainingPage` (mark completed) and `ShoppingPage` (toggle item). Wiring
 * `useNotify()` success feedback here is follow-up work for whenever
 * FOR-103 lands.
 *
 * <p>Never renders a token, secret or other credential field — the read model
 * itself ({@link IntegrationConnection}) carries none (ADR-004).
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly connections: IntegrationConnection[] };

/**
 * FOR-116: researched replacing this generic-icon mapping with each
 * provider's actual brand mark. Finding — none of the three can be
 * responsibly embedded today, per each provider's own published trademark
 * terms:
 *
 * <ul>
 *   <li><b>Google (Google Fit)</b>: Google's Brand Resource Center states the
 *       Google logo "can only be used if you have an existing partnership or
 *       sponsorship" with formal approval from Google's brand team
 *       (about.google/brand-resource-center/guidance/). FORMA has neither.
 *   <li><b>Apple (Apple Health)</b>: Apple's trademark guidelines require an
 *       express written license for the Apple logo/icon generally
 *       (apple.com/legal/intellectual-property/guidelinesfor3rdparties.html).
 *       The narrower "Works with Apple Health" badge and Health app icon are
 *       licensed only to apps with a genuine, shipped HealthKit integration
 *       (developer.apple.com/licensing-trademarks/works-with-apple-health/),
 *       and Apple explicitly says not to "create graphics, logotypes, or
 *       graphic renderings to represent the Apple Health app" — so a
 *       hand-approximated mark is out, not just the exact logo file.
 *   <li><b>Withings</b>: brand assets are distributed through their
 *       press/newsroom kit for media and partners
 *       (withings.com/us/en/health-solutions/press); no generic license for
 *       unaffiliated third-party apps was found.
 * </ul>
 *
 * All three programs above are conditioned on a real, verified integration
 * or partnership. FORMA has neither — there is no integrations backend yet
 * (see `frontend/src/api/integrations.ts` doc comment: every connection is a
 * static mock, and connect/sync/disconnect always reject until the External
 * Integrations epic, FOR-103, ships). So per this story's own explicit
 * fallback clause, the generic `Icon` mapping below is kept — deliberately,
 * not by oversight. Revisit once FORMA has a shipped integration with a
 * given provider and can request/verify that provider's actual brand assets.
 *
 * <p>Both render sites below (connected list, available list) must read this
 * same mapping — `IntegrationsSection.test.tsx`'s FOR-116 test locks that in.
 */
const PROVIDER_ICON_FALLBACK: Record<IntegrationProviderId, IconName> = {
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
        // Shared destructive-confirmation pattern (FOR-63), built on Modal.
        <ConfirmDialog
          title={`Desconectar ${disconnectTarget.providerName}`}
          message={`¿Seguro que quieres desconectar ${disconnectTarget.providerName}? Podrás volver a conectarlo cuando quieras.`}
          confirmLabel="Desconectar"
          pending={pendingProviderId === disconnectTarget.providerId}
          onConfirm={handleDisconnectConfirm}
          onCancel={() => setDisconnectTarget(undefined)}
        />
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
                <Icon name={PROVIDER_ICON_FALLBACK[provider.providerId]} />
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
                <Icon name={PROVIDER_ICON_FALLBACK[provider.providerId]} />
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
