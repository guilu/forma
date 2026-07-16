package dev.diegobarrioh.forma.application;

/**
 * Thrown by a {@link ProviderMeasuresGateway} when fetching a provider's measures fails (FOR-132) —
 * the provider is unreachable, rate-limited, returns a 5xx, or returns a response the adapter
 * cannot parse (spec FOR-132 Edge Cases: "Withings rate limit / 5xx / unreachable → readable
 * failure outcome, connection not corrupted, no secret leak, no crash").
 *
 * <p>Unlike {@link ProviderOAuthException} (which the delivery layer maps to an HTTP 502), this
 * exception is always caught inside {@link IntegrationService#sync} and converted to a readable
 * {@code SyncResult#ERROR} outcome in the 200 response body (spec FOR-132 api.md: "surfaced as a
 * readable result in the 200 outcome body... not an HTTP 5xx to the client").
 *
 * <p>The message is always a safe, generic summary — constructors deliberately do not accept the
 * raw provider response body or the access token, so a future call site cannot accidentally leak
 * one into this exception's message (ADR-008).
 */
public class ProviderSyncException extends RuntimeException {
  public ProviderSyncException(String safeMessage) {
    super(safeMessage);
  }

  public ProviderSyncException(String safeMessage, Throwable cause) {
    super(safeMessage, cause);
  }
}
