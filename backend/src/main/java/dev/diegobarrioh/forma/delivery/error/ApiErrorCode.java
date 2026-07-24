package dev.diegobarrioh.forma.delivery.error;

/**
 * Stable, machine-readable API error codes (FOR-88, ADR-005). Clients can branch on these without
 * parsing human-readable messages.
 *
 * <p>{@link #UNAUTHORIZED} and {@link #FORBIDDEN} were reserved placeholders until real
 * authentication (ADR-012, FOR-145) activated them: {@link #UNAUTHORIZED} on a missing/invalid
 * session or bad login credentials, {@link #FORBIDDEN} on a missing/invalid CSRF token or a
 * cross-owner access attempt.
 */
public enum ApiErrorCode {
  /** Request failed input validation. */
  VALIDATION_ERROR,
  /** Requested resource does not exist. */
  NOT_FOUND,
  /** Authentication is required or failed (FOR-145, ADR-012). */
  UNAUTHORIZED,
  /** Authenticated caller lacks permission, or a CSRF check failed (FOR-145, ADR-012). */
  FORBIDDEN,
  /** The request conflicts with existing state (FOR-145, e.g. a duplicate registration email). */
  CONFLICT,
  /**
   * An upstream external provider call failed (FOR-131, e.g. a Withings OAuth token exchange or
   * refresh) — the caller's request was valid, but a dependency this server does not control
   * failed. Maps to HTTP 502.
   */
  PROVIDER_ERROR,
  /** Unexpected server-side error; details are logged, never returned. */
  INTERNAL_ERROR
}
