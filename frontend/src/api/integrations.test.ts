import { describe, expect, it, vi } from 'vitest';
import { type ApiClient } from './client';
import {
  connectIntegration,
  disconnectIntegration,
  listIntegrations,
  syncIntegration,
  type IntegrationConnection,
} from './integrations';

/**
 * FOR-126 shipped the real backend for this slice (`IntegrationController`,
 * verified directly against `backend/src/main/java/.../delivery/integrations/*`).
 * These tests exercise the real request wiring via an injected {@link ApiClient}
 * mock (same pattern as `goals.test.ts`), replacing the old
 * "always-rejecting mock" suite this module shipped under FOR-57.
 */
describe('integrations API (FOR-126 delivery/integrations contract, FOR-123)', () => {
  it('GETs /api/v1/integrations and maps the envelope into IntegrationConnection read models', async () => {
    const request = vi.fn().mockResolvedValue({
      providers: [
        {
          provider: 'WITHINGS',
          status: 'CONNECTED',
          connectedAt: '2026-07-15T08:00:00Z',
          lastSyncAt: '2026-07-15T09:00:00Z',
          lastSyncOutcome: { result: 'OK', importedCount: 0, message: null },
        },
        {
          provider: 'GOOGLE_FIT',
          status: 'DISCONNECTED',
          connectedAt: null,
          lastSyncAt: null,
          lastSyncOutcome: null,
        },
        {
          provider: 'APPLE_HEALTH',
          status: 'DISCONNECTED',
          connectedAt: null,
          lastSyncAt: null,
          lastSyncOutcome: null,
        },
      ],
    });
    const client: ApiClient = { baseUrl: 'http://test', request };

    const connections = await listIntegrations(client);

    expect(request).toHaveBeenCalledWith('/api/v1/integrations');
    expect(connections).toEqual<IntegrationConnection[]>([
      {
        providerId: 'WITHINGS',
        providerName: 'Withings',
        description: 'Sincroniza automáticamente tus datos de salud y composición corporal.',
        status: 'CONNECTED',
        lastSyncAt: '2026-07-15T09:00:00Z',
      },
      {
        providerId: 'GOOGLE_FIT',
        providerName: 'Google Fit',
        description: 'Sincroniza tu actividad y entrenamientos.',
        status: 'NOT_CONNECTED',
        lastSyncAt: undefined,
      },
      {
        providerId: 'APPLE_HEALTH',
        providerName: 'Apple Health',
        description: 'Sincroniza tus datos de salud de Apple.',
        status: 'NOT_CONNECTED',
        lastSyncAt: undefined,
      },
    ]);
  });

  it('translates the backend DISCONNECTED status to the frontend NOT_CONNECTED status', async () => {
    const request = vi.fn().mockResolvedValue({
      providers: [
        {
          provider: 'WITHINGS',
          status: 'DISCONNECTED',
          connectedAt: null,
          lastSyncAt: null,
          lastSyncOutcome: null,
        },
      ],
    });
    const client: ApiClient = { baseUrl: 'http://test', request };

    const [connection] = await listIntegrations(client);

    expect(connection.status).toBe('NOT_CONNECTED');
  });

  it('never includes a token/credential field on any connection', async () => {
    const request = vi.fn().mockResolvedValue({
      providers: [
        {
          provider: 'WITHINGS',
          status: 'CONNECTED',
          connectedAt: '2026-07-15T08:00:00Z',
          lastSyncAt: null,
          lastSyncOutcome: null,
        },
      ],
    });
    const client: ApiClient = { baseUrl: 'http://test', request };

    const [connection] = await listIntegrations(client);

    expect(connection).not.toHaveProperty('token');
    expect(connection).not.toHaveProperty('accessToken');
    expect(connection).not.toHaveProperty('refreshToken');
  });

  it('POSTs connect to the lower-cased provider path segment (matches IntegrationController)', async () => {
    const result = {
      provider: 'WITHINGS',
      status: 'CONNECTED',
      connectedAt: '2026-07-15T08:00:00Z',
    };
    const request = vi.fn().mockResolvedValue(result);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const response = await connectIntegration('WITHINGS', client);

    expect(request).toHaveBeenCalledWith('/api/v1/integrations/withings/connect', {
      method: 'POST',
    });
    expect(response).toBe(result);
  });

  it('DELETEs disconnect to the lower-cased provider path segment', async () => {
    const result = { provider: 'WITHINGS', status: 'DISCONNECTED', connectedAt: null };
    const request = vi.fn().mockResolvedValue(result);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const response = await disconnectIntegration('WITHINGS', client);

    expect(request).toHaveBeenCalledWith('/api/v1/integrations/withings', { method: 'DELETE' });
    expect(response).toBe(result);
  });

  it('POSTs a manual sync and resolves with the real outcome (importedCount never fabricated)', async () => {
    const outcome = {
      result: 'OK',
      importedCount: 0,
      lastSyncAt: '2026-07-15T09:00:00Z',
      message: null,
    };
    const request = vi.fn().mockResolvedValue(outcome);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const response = await syncIntegration('WITHINGS', client);

    expect(request).toHaveBeenCalledWith('/api/v1/integrations/withings/sync', { method: 'POST' });
    expect(response).toEqual(outcome);
    expect(response.importedCount).toBe(0);
  });

  it('resolves (does not reject) with a readable NOT_CONNECTED outcome when syncing a disconnected provider', async () => {
    const outcome = {
      result: 'NOT_CONNECTED',
      importedCount: 0,
      lastSyncAt: null,
      message: 'El proveedor no está conectado.',
    };
    const request = vi.fn().mockResolvedValue(outcome);
    const client: ApiClient = { baseUrl: 'http://test', request };

    await expect(syncIntegration('WITHINGS', client)).resolves.toEqual(outcome);
  });

  it('propagates a rejected request (e.g. ApiRequestError) instead of swallowing it', async () => {
    const request = vi.fn().mockRejectedValue(new Error('network'));
    const client: ApiClient = { baseUrl: 'http://test', request };

    await expect(connectIntegration('GOOGLE_FIT', client)).rejects.toThrow('network');
  });
});
