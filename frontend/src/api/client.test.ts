import { describe, expect, it } from 'vitest';
import { createApiClient, getApiBaseUrl, apiClient } from './client';

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
