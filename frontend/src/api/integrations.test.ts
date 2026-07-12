import { describe, expect, it } from 'vitest';
import { ApiRequestError } from './client';
import {
  connectIntegration,
  disconnectIntegration,
  listIntegrations,
  syncIntegration,
} from './integrations';

describe('integrations API client (mock — no backend yet, see module doc comment)', () => {
  it('lists Withings as connected with a last-sync timestamp, and the others as not connected', async () => {
    const connections = await listIntegrations();

    expect(connections).toEqual([
      expect.objectContaining({
        providerId: 'WITHINGS',
        providerName: 'Withings',
        status: 'CONNECTED',
        lastSyncAt: '2026-07-10T08:15:00Z',
      }),
      expect.objectContaining({ providerId: 'GOOGLE_FIT', status: 'NOT_CONNECTED' }),
      expect.objectContaining({ providerId: 'APPLE_HEALTH', status: 'NOT_CONNECTED' }),
    ]);
  });

  it('never includes a token/credential field on any connection', async () => {
    const connections = await listIntegrations();

    for (const connection of connections) {
      expect(connection).not.toHaveProperty('token');
      expect(connection).not.toHaveProperty('accessToken');
      expect(connection).not.toHaveProperty('refreshToken');
    }
  });

  it('rejects connectIntegration with a safe, non-technical message', async () => {
    await expect(connectIntegration('GOOGLE_FIT')).rejects.toMatchObject({
      message: expect.stringContaining('Google Fit'),
    });
    await expect(connectIntegration('GOOGLE_FIT')).rejects.toBeInstanceOf(ApiRequestError);
  });

  it('rejects disconnectIntegration with a safe, non-technical message', async () => {
    await expect(disconnectIntegration('WITHINGS')).rejects.toMatchObject({
      message: expect.stringContaining('Withings'),
    });
  });

  it('rejects syncIntegration with a safe, non-technical message and no sensitive data', async () => {
    await expect(syncIntegration('WITHINGS')).rejects.toMatchObject({
      message: expect.stringContaining('Withings'),
    });
    try {
      await syncIntegration('WITHINGS');
      throw new Error('expected syncIntegration to reject');
    } catch (error) {
      const message = (error as Error).message;
      expect(message).not.toMatch(/token|secret|password/i);
    }
  });

  it('rejects with a 404-style error for an unrecognized provider id', async () => {
    // @ts-expect-error — exercising the runtime guard for an invalid id.
    await expect(connectIntegration('UNKNOWN')).rejects.toBeInstanceOf(ApiRequestError);
  });
});
