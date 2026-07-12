/**
 * External integrations API surface (FOR-57), shaped like every other client in
 * this folder (ADR-006 — one client boundary, typed read models + commands).
 *
 * <p><b>There is no integrations backend yet.</b> Verified against the
 * repository (no `integration`/`withings` controller or persisted connection
 * state under `backend/src/main/java`, see AGENTS.md bootstrap status). The
 * External Integrations epic (OAuth connect/disconnect, encrypted token
 * storage, provider sync into `BodyMeasurement`, connection/sync status +
 * manual-sync endpoints) is tracked separately (enabler/epic FOR-103, not yet
 * built). Repository state has priority over the spec's target design — so
 * this module does NOT call `apiClient.request()` against endpoints that do
 * not exist (that would either silently 404 in production or require faking a
 * server in tests). Instead:
 *
 * <ul>
 *   <li>{@link listIntegrations} resolves a documented, static MOCK fixture
 *       matching `docs/8-configuracion.png` ("CONEXIONES E INTEGRACIONES"):
 *       Withings connected, Google Fit / Apple Health not connected.
 *   <li>{@link connectIntegration}, {@link disconnectIntegration} and
 *       {@link syncIntegration} are real command entry points (the UI wires
 *       real buttons to them) but they reject with a safe, user-readable
 *       {@link ApiRequestError} explaining the backend does not exist yet —
 *       never a fake success. This is intentional: FOR-57's scope is "entry
 *       points exist but must be clearly non-functional where no backend
 *       exists" (spec Edge Cases), not simulated OAuth.
 * </ul>
 *
 * <p>When FOR-103 lands, swap each function body for an `apiClient.request()`
 * call against the path already documented on it below — the exported
 * signatures are designed to stay stable across that swap.
 */
import { ApiRequestError } from './client';

export type IntegrationProviderId = 'WITHINGS' | 'GOOGLE_FIT' | 'APPLE_HEALTH';

export type IntegrationStatus = 'CONNECTED' | 'NOT_CONNECTED';

/**
 * A provider's connection state as the (future) backend would expose it.
 * Deliberately carries no token/credential field — ADR-004 "Do not expose
 * provider tokens to domain services or the frontend" applies to the read
 * model shape itself, not just to what the UI happens to render.
 */
export interface IntegrationConnection {
  readonly providerId: IntegrationProviderId;
  readonly providerName: string;
  readonly description: string;
  readonly status: IntegrationStatus;
  /** ISO-8601 timestamp of the last successful sync; absent if never synced. */
  readonly lastSyncAt?: string;
}

/**
 * Planned base path for the External Integrations epic (FOR-103, not
 * implemented). Exported so the eventual FOR-103 swap and any consumer that
 * wants to log/reference the future endpoint have one source of truth.
 */
export const INTEGRATIONS_PATH = '/api/v1/integrations';

/**
 * Static mock fixture (tests.md: "Withings connected (with last sync), Google
 * Fit / Apple Health available"). `lastSyncAt` is a fixed sample timestamp, not
 * a live value — there is nothing live to read yet.
 */
const MOCK_CONNECTIONS: readonly IntegrationConnection[] = [
  {
    providerId: 'WITHINGS',
    providerName: 'Withings',
    description: 'Sincroniza automáticamente tus datos de salud y composición corporal.',
    status: 'CONNECTED',
    lastSyncAt: '2026-07-10T08:15:00Z',
  },
  {
    providerId: 'GOOGLE_FIT',
    providerName: 'Google Fit',
    description: 'Sincroniza tu actividad y entrenamientos.',
    status: 'NOT_CONNECTED',
  },
  {
    providerId: 'APPLE_HEALTH',
    providerName: 'Apple Health',
    description: 'Sincroniza tus datos de salud de Apple.',
    status: 'NOT_CONNECTED',
  },
];

/** A backend-shaped "not implemented yet" error, never leaking internals. */
function notImplementedError(action: string, provider: IntegrationConnection): ApiRequestError {
  return new ApiRequestError(
    501,
    `No se pudo ${action} ${provider.providerName}: la integración con proveedores externos ` +
      'todavía no está disponible.',
    'NOT_IMPLEMENTED',
  );
}

/**
 * Resolves a provider from the fixture, or a rejected promise carrying a
 * 404-style {@link ApiRequestError} for an unrecognized id. Returns a promise
 * (rather than throwing synchronously) so every exported command here is
 * safe to chain with `.catch()`/`await` uniformly, regardless of which branch
 * fails.
 */
function findProvider(providerId: IntegrationProviderId): Promise<IntegrationConnection> {
  const provider = MOCK_CONNECTIONS.find((connection) => connection.providerId === providerId);
  if (!provider) {
    return Promise.reject(
      new ApiRequestError(404, 'Proveedor de integración no reconocido.', 'NOT_FOUND'),
    );
  }
  return Promise.resolve(provider);
}

/**
 * Lists every provider's connection state (connected + available).
 * Planned: `GET {@link INTEGRATIONS_PATH}`.
 */
export function listIntegrations(): Promise<IntegrationConnection[]> {
  return Promise.resolve([...MOCK_CONNECTIONS]);
}

/**
 * Connect entry point — hands off to the provider's OAuth flow (redirect
 * handled outside this story, see specs/FOR-57/ui.md). No real OAuth exists
 * yet, so this always rejects with a safe, explicit message.
 * Planned: `POST {@link INTEGRATIONS_PATH}/{providerId}/connect`.
 */
export function connectIntegration(providerId: IntegrationProviderId): Promise<never> {
  return findProvider(providerId).then((provider) =>
    Promise.reject(notImplementedError('conectar con', provider)),
  );
}

/**
 * Disconnect entry point (explicit confirmation happens in the UI, FOR-63
 * destructive pattern). Always rejects — no revocation backend exists yet.
 * Planned: `POST {@link INTEGRATIONS_PATH}/{providerId}/disconnect`.
 */
export function disconnectIntegration(providerId: IntegrationProviderId): Promise<never> {
  return findProvider(providerId).then((provider) =>
    Promise.reject(notImplementedError('desconectar', provider)),
  );
}

/**
 * Manual "sync now" entry point. Always rejects — no sync backend exists yet.
 * Planned: `POST {@link INTEGRATIONS_PATH}/{providerId}/sync`.
 */
export function syncIntegration(providerId: IntegrationProviderId): Promise<never> {
  return findProvider(providerId).then((provider) =>
    Promise.reject(notImplementedError('sincronizar', provider)),
  );
}
