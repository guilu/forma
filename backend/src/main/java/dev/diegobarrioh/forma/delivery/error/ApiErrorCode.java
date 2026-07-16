package dev.diegobarrioh.forma.delivery.error;

/**
 * Stable, machine-readable API error codes (FOR-88, ADR-005). Clients can branch on these without
 * parsing human-readable messages.
 *
 * <p>{@link #UNAUTHORIZED} and {@link #FORBIDDEN} are reserved placeholders: the
 * authentication/authorization flow (ADR-002) is a later story, but reserving the codes now keeps
 * the contract stable when it lands.
 */
public enum ApiErrorCode {
  /** Request failed input validation. */
  VALIDATION_ERROR,
  /** Requested resource does not exist. */
  NOT_FOUND,
  /** Authentication is required or failed. Reserved placeholder. */
  UNAUTHORIZED,
  /** Authenticated caller lacks permission. Reserved placeholder. */
  FORBIDDEN,
  /**
   * An upstream external provider call failed (FOR-131, e.g. a Withings OAuth token exchange or
   * refresh) — the caller's request was valid, but a dependency this server does not control
   * failed. Maps to HTTP 502.
   */
  PROVIDER_ERROR,
  /** Unexpected server-side error; details are logged, never returned. */
  INTERNAL_ERROR
}
