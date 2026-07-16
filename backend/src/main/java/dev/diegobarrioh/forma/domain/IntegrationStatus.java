package dev.diegobarrioh.forma.domain;

/**
 * An {@link IntegrationConnection}'s connection state (FOR-126, extended by FOR-131).
 *
 * <p>FOR-126 only had {@link #DISCONNECTED}/{@link #CONNECTED} because its connect was a mock with
 * an always-immediate transition. FOR-131 adds real Withings OAuth, which has a real gap between
 * "authorization URL issued" and "callback completed the token exchange" — {@link #PENDING} makes
 * that gap visible instead of lying that the provider is still just DISCONNECTED (spec FOR-131 Data
 * Model Notes: "add a PENDING/AWAITING_CALLBACK state if needed; document"). {@link #NEEDS_REAUTH}
 * makes a failed token refresh visible too, instead of silently dropping back to DISCONNECTED and
 * losing the fact that the user was connected (spec FOR-131 Edge Cases: "Refresh failure → mark
 * connection needing re-auth; do not silently drop").
 */
public enum IntegrationStatus {
  /** No active connection; the default before any connect, or after a disconnect. */
  DISCONNECTED,
  /**
   * An authorization URL was issued and the provider is awaiting the OAuth callback to complete the
   * token exchange (FOR-131). Never set for providers without a real OAuth gateway.
   */
  PENDING,
  /**
   * The provider is connected. Mock (no OAuth) before FOR-131; real Withings OAuth from FOR-131.
   */
  CONNECTED,
  /**
   * The provider was connected but its access token could not be refreshed and the stored refresh
   * token is no longer usable (FOR-131); the user must reconnect. Sync history is preserved.
   */
  NEEDS_REAUTH
}
