package dev.diegobarrioh.forma.domain;

/**
 * An {@link IntegrationConnection}'s connection state (FOR-126). No intermediate
 * "connecting"/"awaiting auth" state exists in this slice — that belongs to the real OAuth flow
 * (FOR-103 slice 2); this slice's connect is a mock, so the transition is always immediate.
 */
public enum IntegrationStatus {
  /** No active connection; the default before any connect, or after a disconnect. */
  DISCONNECTED,
  /** The provider is connected (mock, no OAuth in this slice). */
  CONNECTED
}
