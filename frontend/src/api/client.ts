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

// Same-origin by default: the app calls relative `/api/...` paths so the browser
// hits whatever host served the SPA. In production the frontend's nginx reverse-
// proxies `/api/` to the backend container; in dev the Vite server proxies it to
// the local backend (see vite.config.ts). This avoids baking a fixed backend
// host (e.g. localhost:8080) into the production bundle. Override only when the
// API is genuinely on another origin, via VITE_API_BASE_URL.
const DEFAULT_BASE_URL = '';

/** Resolve the backend base URL from the Vite environment; empty = same origin. */
export function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL ?? DEFAULT_BASE_URL;
}

/** A single field-level validation problem (mirrors the backend `ApiError`). */
export interface ApiFieldError {
  readonly field: string;
  readonly message: string;
}

/** Backend standard error response shape (docs/api-conventions.md, FOR-88). */
export interface ApiErrorBody {
  readonly code: string;
  readonly message: string;
  readonly correlationId?: string;
  readonly details?: ReadonlyArray<ApiFieldError>;
}

/**
 * Error thrown when the backend responds with a non-2xx status. It carries the
 * backend's safe `ApiError.message` (never a raw response body/stack trace) so
 * feature pages can show it directly, plus the machine-readable `code` and any
 * per-field `details` for inline validation feedback.
 */
export class ApiRequestError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly details?: ReadonlyArray<ApiFieldError>;

  constructor(
    status: number,
    message: string,
    code?: string,
    details?: ReadonlyArray<ApiFieldError>,
  ) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

export interface ApiClient {
  readonly baseUrl: string;
  /**
   * Thin typed wrapper over `fetch` that prefixes the base URL and requests
   * JSON. Feature stories build their calls on top of this rather than calling
   * `fetch` directly. On a non-2xx response it throws {@link ApiRequestError}
   * carrying the backend's `ApiError.message`.
   */
  request<T>(path: string, init?: RequestInit): Promise<T>;
}

/** Best-effort parse of a backend `ApiError` body; returns undefined if absent/malformed. */
async function parseApiError(response: Response): Promise<ApiErrorBody | undefined> {
  try {
    const body = (await response.json()) as Partial<ApiErrorBody>;
    if (body && typeof body.message === 'string' && typeof body.code === 'string') {
      return body as ApiErrorBody;
    }
  } catch {
    // Non-JSON or empty body — fall back to a generic message below.
  }
  return undefined;
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
        const body = await parseApiError(response);
        throw new ApiRequestError(
          response.status,
          body?.message ?? `API request failed: ${response.status} ${response.statusText}`,
          body?.code,
          body?.details,
        );
      }

      return (await response.json()) as T;
    },
  };
}

/** Shared default client instance for the application. */
export const apiClient = createApiClient();
