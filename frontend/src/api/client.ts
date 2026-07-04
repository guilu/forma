/**
 * Centralized API client boundary (FOR-81).
 *
 * ADR-006 and the coding standards require all backend access to go through a
 * single client instead of scattered `fetch` calls. This is a placeholder: it
 * establishes the boundary and base-URL resolution, but defines no product
 * endpoints — those arrive with their owning stories (the backend API baseline
 * is FOR-88). The frontend never implements domain rules; it only calls read
 * models and commands exposed here.
 */

const DEFAULT_BASE_URL = 'http://localhost:8080';

/** Resolve the backend base URL from the Vite environment, with a dev fallback. */
export function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL ?? DEFAULT_BASE_URL;
}

export interface ApiClient {
  readonly baseUrl: string;
  /**
   * Thin typed wrapper over `fetch` that prefixes the base URL and requests
   * JSON. Feature stories build their calls on top of this rather than calling
   * `fetch` directly.
   */
  request<T>(path: string, init?: RequestInit): Promise<T>;
}

export function createApiClient(baseUrl: string = getApiBaseUrl()): ApiClient {
  return {
    baseUrl,
    async request<T>(path: string, init?: RequestInit): Promise<T> {
      const response = await fetch(`${baseUrl}${path}`, {
        headers: { Accept: 'application/json', ...init?.headers },
        ...init,
      });

      if (!response.ok) {
        throw new Error(`API request failed: ${response.status} ${response.statusText}`);
      }

      return (await response.json()) as T;
    },
  };
}

/** Shared default client instance for the application. */
export const apiClient = createApiClient();
