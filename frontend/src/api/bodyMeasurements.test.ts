import { describe, expect, it, vi } from 'vitest';
import { createBodyMeasurement, listBodyMeasurements } from './bodyMeasurements';
import { ApiRequestError, type ApiClient } from './client';

/**
 * Body measurements API-module tests (FOR-18). Verifies the request is built on
 * the shared client with the FOR-17 contract shape, and that client errors
 * propagate. No real network is used.
 */
describe('createBodyMeasurement', () => {
  it('POSTs the measurement payload to the versioned endpoint', async () => {
    const request = vi.fn().mockResolvedValue({ source: 'MANUAL' });
    const client: ApiClient = { baseUrl: 'http://test', request };

    await createBodyMeasurement(
      { measuredAt: '2026-07-05T08:00:00Z', weightKg: 78.4, bodyFatPercentage: 18.2, bmi: 23.9 },
      client,
    );

    expect(request).toHaveBeenCalledWith('/api/v1/body/measurements', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        measuredAt: '2026-07-05T08:00:00Z',
        weightKg: 78.4,
        bodyFatPercentage: 18.2,
        bmi: 23.9,
      }),
    });
  });

  it('propagates ApiRequestError from the client', async () => {
    const request = vi
      .fn()
      .mockRejectedValue(new ApiRequestError(400, 'Request validation failed'));
    const client: ApiClient = { baseUrl: 'http://test', request };

    await expect(
      createBodyMeasurement(
        { measuredAt: '2026-07-05T08:00:00Z', weightKg: 78.4, bodyFatPercentage: 18.2, bmi: 23.9 },
        client,
      ),
    ).rejects.toBeInstanceOf(ApiRequestError);
  });
});

describe('listBodyMeasurements', () => {
  it('GETs the versioned endpoint and returns the measurements', async () => {
    const measurements = [{ source: 'MANUAL', weightKg: 73.6 }];
    const request = vi.fn().mockResolvedValue(measurements);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await listBodyMeasurements(client);

    expect(request).toHaveBeenCalledWith('/api/v1/body/measurements');
    expect(result).toBe(measurements);
  });
});
