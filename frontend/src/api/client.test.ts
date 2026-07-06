import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiRequestError, createApiClient, getApiBaseUrl, apiClient } from './client';

/**
 * API client boundary tests (FOR-81): the placeholder client instantiates and
 * resolves a base URL. No product endpoints are exercised.
 */
describe('api client', () => {
  it('exposes a default client instance', () => {
    expect(apiClient.baseUrl).toBeTruthy();
    expect(typeof apiClient.request).toBe('function');
  });

  it('uses the provided base URL', () => {
    const client = createApiClient('http://example.test');
    expect(client.baseUrl).toBe('http://example.test');
  });

  it('resolves a non-empty base URL from the environment', () => {
    expect(getApiBaseUrl()).toMatch(/^https?:\/\//);
  });
});

describe('api client error handling', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('throws ApiRequestError carrying the backend ApiError message on non-2xx', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            code: 'VALIDATION_ERROR',
            message: 'Request validation failed',
            details: [{ field: 'weightKg', message: 'must not be null' }],
          }),
          { status: 400, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    const client = createApiClient('http://test');
    const error = await client.request('/api/v1/body/measurements').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiRequestError);
    const apiError = error as ApiRequestError;
    expect(apiError.message).toBe('Request validation failed');
    expect(apiError.code).toBe('VALIDATION_ERROR');
    expect(apiError.details?.[0]?.field).toBe('weightKg');
  });

  it('falls back to a generic message when the error body is not an ApiError', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response('boom', { status: 500, statusText: 'Server Error' })),
    );

    const client = createApiClient('http://test');
    const error = (await client
      .request('/api/v1/body/measurements')
      .catch((e: unknown) => e)) as ApiRequestError;

    expect(error).toBeInstanceOf(ApiRequestError);
    expect(error.status).toBe(500);
    expect(error.message).toContain('500');
  });
});
