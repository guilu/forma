/**
 * External integrations API surface (FOR-57), shaped like every other client in
 * this folder (ADR-006 — one client boundary, typed read models + commands).
 *
 * <p>FOR-126 shipped the real backend for this slice — verified directly
 * against `backend/src/main/java/.../delivery/integrations/*`
 * (`IntegrationController`, `IntegrationsListResponse`,
 * `IntegrationConnectionResponse`, `ConnectionStatusResponse`,
 * `SyncResponse`, `SyncOutcomeResponse`), not just `specs/FOR-126/api.md`.
 * This replaces the previous FOR-57 mock fixture and the `Promise<never>`
 * command stubs with real {@link apiClient} calls, unblocking FOR-123's
 * success-feedback wiring.
 *
 * <p><b>Enum casing (verified, not assumed):</b> the backend's
 * `IntegrationProvider`/`IntegrationStatus` enums are UPPERCASE
 * (`WITHINGS`/`GOOGLE_FIT`/`APPLE_HEALTH`, `CONNECTED`/`DISCONNECTED`).
 * {@link IntegrationProviderId} mirrors the provider names verbatim; the
 * connect/disconnect/sync path segment is lower-cased to match
 * `IntegrationController`'s `@PathVariable` (parsed case-insensitively via
 * `IntegrationProvider.valueOf(raw.toUpperCase())`, but every other client in
 * this codebase sends the documented lowercase form, e.g. `api.md`'s
 * `/integrations/withings/connect`).
 *
 * <p><b>Provider display copy is frontend-owned</b> — the backend's read
 * model deliberately carries no `providerName`/`description` field (FOR-126
 * spec: "Success messages come from the frontend... the backend response for
 * these commands isn't expected to carry user-facing copy"), so {@link
 * PROVIDER_METADATA} keeps the same copy this module has shipped since FOR-57.
 *
 * <p><b>Sync is a documented stub this slice</b> (FOR-126): {@link
 * syncIntegration} always resolves with `importedCount: 0` — real Withings
 * sync is FOR-103 slice 3, not this story. Syncing a disconnected provider
 * also resolves (200) with a readable `result: 'NOT_CONNECTED'` outcome
 * rather than throwing (spec FOR-126 Edge Cases) — callers must check
 * `result`, not assume every resolved sync succeeded (FOR-123: never
 * fabricate a "sync succeeded" message for that case).
 */
import { apiClient, type ApiClient } from './client';

export type IntegrationProviderId = 'WITHINGS' | 'GOOGLE_FIT' | 'APPLE_HEALTH';

export type IntegrationStatus = 'CONNECTED' | 'NOT_CONNECTED';

/**
 * A provider's connection state as the frontend renders it. Deliberately
 * carries no token/credential field — ADR-004 "Do not expose provider tokens
 * to domain services or the frontend" applies to the read model shape
 * itself, not just to what the UI happens to render (the backend's own
 * `IntegrationConnectionResponse` carries none either).
 */
export interface IntegrationConnection {
  readonly providerId: IntegrationProviderId;
  readonly providerName: string;
  readonly description: string;
  readonly status: IntegrationStatus;
  /** ISO-8601 timestamp of the last sync attempt; absent if never synced. */
  readonly lastSyncAt?: string;
}

/** Base path for the FOR-126 integrations endpoints. */
export const INTEGRATIONS_PATH = '/api/v1/integrations';

/** Frontend-owned display copy per provider (FOR-126: the backend carries none). */
const PROVIDER_METADATA: Record<IntegrationProviderId, { name: string; description: string }> = {
  WITHINGS: {
    name: 'Withings',
    description: 'Sincroniza automáticamente tus datos de salud y composición corporal.',
  },
  GOOGLE_FIT: {
    name: 'Google Fit',
    description: 'Sincroniza tu actividad y entrenamientos.',
  },
  APPLE_HEALTH: {
    name: 'Apple Health',
    description: 'Sincroniza tus datos de salud de Apple.',
  },
};

/** Raw last-sync outcome nested in a status row (`SyncOutcomeResponse`, FOR-126). */
interface SyncOutcomeRow {
  readonly result: 'OK' | 'NOT_CONNECTED';
  readonly importedCount: number;
  readonly message: string | null;
}

/** Raw `GET /api/v1/integrations` provider row (`IntegrationConnectionResponse`, FOR-126). */
interface ConnectionStatusRow {
  readonly provider: IntegrationProviderId;
  readonly status: 'CONNECTED' | 'DISCONNECTED';
  readonly connectedAt: string | null;
  readonly lastSyncAt: string | null;
  readonly lastSyncOutcome: SyncOutcomeRow | null;
}

/** Raw `GET /api/v1/integrations` envelope (`IntegrationsListResponse`, FOR-126). */
interface IntegrationsListBody {
  readonly providers: readonly ConnectionStatusRow[];
}

/**
 * Raw connect/disconnect response (`ConnectionStatusResponse`, FOR-126):
 * shared by `POST /{provider}/connect` and `DELETE /{provider}` — no sync
 * fields (those only appear on the status list and the sync response).
 */
export interface ConnectionActionResult {
  readonly provider: IntegrationProviderId;
  readonly status: 'CONNECTED' | 'DISCONNECTED';
  readonly connectedAt: string | null;
}

/**
 * Raw manual-sync response (`SyncResponse`, FOR-126). `importedCount` is
 * never fabricated by the backend — always `0` this slice (stub/no-op
 * import; real counts arrive with FOR-103 slice 3).
 */
export interface SyncOutcome {
  readonly result: 'OK' | 'NOT_CONNECTED';
  readonly importedCount: number;
  readonly lastSyncAt: string | null;
  readonly message: string | null;
}

/** Maps one backend status row + frontend-owned copy into the UI read model. */
function toIntegrationConnection(row: ConnectionStatusRow): IntegrationConnection {
  const metadata = PROVIDER_METADATA[row.provider];
  return {
    providerId: row.provider,
    providerName: metadata.name,
    description: metadata.description,
    status: row.status === 'CONNECTED' ? 'CONNECTED' : 'NOT_CONNECTED',
    lastSyncAt: row.lastSyncAt ?? undefined,
  };
}

/** Lower-cases the provider id to match `IntegrationController`'s documented path segment. */
function providerPath(providerId: IntegrationProviderId): string {
  return providerId.toLowerCase();
}

/**
 * Lists every known provider's real connection state (FOR-126: `GET`, empty
 * is never the result — providers default to `NOT_CONNECTED`, 404 is never
 * returned).
 */
export function listIntegrations(client: ApiClient = apiClient): Promise<IntegrationConnection[]> {
  return client
    .request<IntegrationsListBody>(INTEGRATIONS_PATH)
    .then((body) => body.providers.map(toIntegrationConnection));
}

/**
 * Connects a provider (mock, no OAuth this slice — FOR-126; real OAuth is
 * FOR-103 slice 2). Idempotent when already connected.
 */
export function connectIntegration(
  providerId: IntegrationProviderId,
  client: ApiClient = apiClient,
): Promise<ConnectionActionResult> {
  return client.request<ConnectionActionResult>(
    `${INTEGRATIONS_PATH}/${providerPath(providerId)}/connect`,
    { method: 'POST' },
  );
}

/** Disconnects a provider; idempotent no-op when already disconnected. */
export function disconnectIntegration(
  providerId: IntegrationProviderId,
  client: ApiClient = apiClient,
): Promise<ConnectionActionResult> {
  return client.request<ConnectionActionResult>(
    `${INTEGRATIONS_PATH}/${providerPath(providerId)}`,
    {
      method: 'DELETE',
    },
  );
}

/**
 * Triggers a manual sync now. Resolves even when the provider isn't
 * connected (`result: 'NOT_CONNECTED'`) — callers must check `result`
 * rather than treat every resolution as a successful sync.
 */
export function syncIntegration(
  providerId: IntegrationProviderId,
  client: ApiClient = apiClient,
): Promise<SyncOutcome> {
  return client.request<SyncOutcome>(`${INTEGRATIONS_PATH}/${providerPath(providerId)}/sync`, {
    method: 'POST',
  });
}
