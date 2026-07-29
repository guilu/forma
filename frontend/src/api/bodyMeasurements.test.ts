import { describe, expect, it, vi } from 'vitest';
import {
  createBodyMeasurement,
  deleteBodyMeasurement,
  listBodyMeasurements,
} from './bodyMeasurements';
import { ApiRequestError, type ApiClient } from './client';

/**
 * Body measurements API-module tests (FOR-18). Verifies the request is built on
 * the shared client with the FOR-17 contract shape, and that client errors
 * propagate. No real network is used.
 */
describe('createBodyMeasurement', () => {
  it('POSTs the measurement payload to the versioned endpoint', async () => {
    const request = vi.fn().mockResolvedValue({ source: 'MANUAL' });
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

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
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    await expect(
      createBodyMeasurement(
        { measuredAt: '2026-07-05T08:00:00Z', weightKg: 78.4, bodyFatPercentage: 18.2, bmi: 23.9 },
        client,
      ),
    ).rejects.toBeInstanceOf(ApiRequestError);
  });
});

describe('deleteBodyMeasurement', () => {
  it('DELETEs the addressed measurement under the versioned endpoint', async () => {
    const request = vi.fn().mockResolvedValue(undefined);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    await deleteBodyMeasurement('0f7d2f3e-1a4b-4c8d-9e2f-5a6b7c8d9e0f', client);

    expect(request).toHaveBeenCalledWith(
      '/api/v1/body/measurements/0f7d2f3e-1a4b-4c8d-9e2f-5a6b7c8d9e0f',
      { method: 'DELETE' },
    );
  });

  it('percent-encodes the id rather than pasting it into the path', async () => {
    const request = vi.fn().mockResolvedValue(undefined);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    await deleteBodyMeasurement('../profile', client);

    expect(request).toHaveBeenCalledWith('/api/v1/body/measurements/..%2Fprofile', {
      method: 'DELETE',
    });
  });

  it('propagates ApiRequestError from the client', async () => {
    const request = vi.fn().mockRejectedValue(new ApiRequestError(404, 'No existe'));
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    await expect(deleteBodyMeasurement('missing', client)).rejects.toBeInstanceOf(ApiRequestError);
  });
});

describe('listBodyMeasurements', () => {
  it('GETs the versioned endpoint and returns the measurements', async () => {
    const measurements = [{ source: 'MANUAL', weightKg: 73.6 }];
    const request = vi.fn().mockResolvedValue(measurements);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await listBodyMeasurements(client);

    expect(request).toHaveBeenCalledWith('/api/v1/body/measurements');
    expect(result).toBe(measurements);
  });
});
