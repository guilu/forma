import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiRequestError, createApiClient, getApiBaseUrl, apiClient } from './client';

/**
 * API client boundary tests (FOR-81): the placeholder client instantiates and
 * resolves a base URL. No product endpoints are exercised.
 */
describe('api client', () => {
  it('exposes a default client instance', () => {
    expect(typeof apiClient.baseUrl).toBe('string');
    expect(typeof apiClient.request).toBe('function');
  });

  it('uses the provided base URL', () => {
    const client = createApiClient('http://example.test');
    expect(client.baseUrl).toBe('http://example.test');
  });

  it('defaults to a same-origin (relative) base URL', () => {
    // No VITE_API_BASE_URL in tests → empty string, so requests are same-origin.
    expect(getApiBaseUrl()).toBe('');
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

  it('resolves 204 No Content responses without parsing a body (FOR-144 delete)', async () => {
    document.cookie = 'XSRF-TOKEN=delete-token; path=/';
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })));

    const client = createApiClient('http://test');
    const result = await client.request('/api/v1/progress/photos/abc', { method: 'DELETE' });

    expect(result).toBeUndefined();
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/';
  });
});

describe('api client requestBlob (FOR-144)', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('fetches the path and resolves the response body as a Blob', async () => {
    const bytes = new Uint8Array([1, 2, 3]);
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          new Response(bytes, { status: 200, headers: { 'Content-Type': 'image/jpeg' } }),
        ),
    );

    const client = createApiClient('http://test');
    const blob = await client.requestBlob('/api/v1/progress/photos/abc');

    // Assert the behavioural contract (content-type + size), not Blob identity:
    // `toBeInstanceOf(Blob)` is unreliable across realms (the mocked fetch's Blob
    // differs from the global one under jsdom/CI), and the arrayBuffer method is
    // absent on jsdom's Blob — type and size hold in every environment.
    expect(blob.type).toBe('image/jpeg');
    expect(blob.size).toBe(3);
  });

  it('throws ApiRequestError carrying the backend ApiError message on non-2xx', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ code: 'FORBIDDEN', message: 'No access to this photo' }), {
          status: 403,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    );

    const client = createApiClient('http://test');
    const error = await client
      .requestBlob('/api/v1/progress/photos/other')
      .catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiRequestError);
    expect((error as ApiRequestError).status).toBe(403);
    expect((error as ApiRequestError).message).toBe('No access to this photo');
  });
});

describe('api client session and CSRF handling (FOR-145d)', () => {
  afterEach(() => {
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; path=/';
    vi.unstubAllGlobals();
  });

  it('uses same-origin credentials for relative and same-origin base URLs', async () => {
    const fetchMock = vi.fn().mockImplementation(async () => new Response('{}'));
    vi.stubGlobal('fetch', fetchMock);

    await createApiClient('').request('/api/v1/auth/me');
    await createApiClient(window.location.origin).request('/api/v1/auth/me');

    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({ credentials: 'same-origin' });
    expect(fetchMock.mock.calls[1]?.[1]).toMatchObject({ credentials: 'same-origin' });
  });

  it('uses include credentials for a cross-origin base URL', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{}'));
    vi.stubGlobal('fetch', fetchMock);

    await createApiClient('https://api.example.test').request('/api/v1/auth/me');

    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({ credentials: 'include' });
  });

  it('primes and sends the CSRF cookie for an unsafe request', async () => {
    const fetchMock = vi
      .fn()
      .mockImplementationOnce(async () => {
        document.cookie = 'XSRF-TOKEN=token%20value; path=/';
        return new Response('{}');
      })
      .mockResolvedValueOnce(new Response('{}'));
    vi.stubGlobal('fetch', fetchMock);

    await createApiClient('').request('/api/v1/auth/login', { method: 'POST' });

    expect(fetchMock.mock.calls[0]?.[0]).toBe('/actuator/health');
    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({
      method: 'GET',
      credentials: 'same-origin',
    });
    expect(new Headers(fetchMock.mock.calls[1]?.[1]?.headers).get('X-XSRF-TOKEN')).toBe(
      'token value',
    );
  });

  it('fails safely when priming does not expose an XSRF token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{}'));
    vi.stubGlobal('fetch', fetchMock);

    await expect(
      createApiClient('https://api.example.test').request('/api/v1/auth/login', {
        method: 'POST',
      }),
    ).rejects.toThrow('No se pudo obtener el token CSRF');
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0]?.[0]).toBe('https://api.example.test/actuator/health');
  });

  it('does not send a CSRF header for safe requests', async () => {
    document.cookie = 'XSRF-TOKEN=present; path=/';
    const fetchMock = vi.fn().mockResolvedValue(new Response('{}'));
    vi.stubGlobal('fetch', fetchMock);

    await createApiClient('').request('/api/v1/auth/me');

    expect(new Headers(fetchMock.mock.calls[0]?.[1]?.headers).has('X-XSRF-TOKEN')).toBe(false);
  });

  it('sends credentials for blob requests', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(new Uint8Array([1])));
    vi.stubGlobal('fetch', fetchMock);

    await createApiClient('https://api.example.test').requestBlob('/photo');

    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({ credentials: 'include' });
  });
});
