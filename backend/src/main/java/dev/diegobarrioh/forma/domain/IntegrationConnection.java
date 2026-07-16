package dev.diegobarrioh.forma.domain;

import java.time.Instant;

/**
 * A provider-neutral external integration connection (FOR-126, first implementable slice of
 * FOR-103): the connect/disconnect/manual-sync shell that will later carry real OAuth state.
 *
 * <p>Framework-free (ADR-001) — no Spring, JDBC or HTTP types. Carries no identity of its own: a
 * connection's identity IS the (owner, {@link #provider}) pair, exactly one row per provider per
 * owner (unlike {@code Goal}'s generated-id list shape) — owner-scoping (ADR-002) lives outside
 * this type, at the application/persistence boundary, like every other aggregate in this codebase.
 *
 * <p><b>Deliberately carries no token/secret field or accessor</b> (ADR-004, spec FOR-126 boundary
 * rule): this is the seam later FOR-103 slices build encrypted token storage against, entirely
 * inside the persistence adapter, without ever touching this type or the {@code
 * IntegrationRepository} port it flows through.
 *
 * @param provider the external provider this connection is for; required
 * @param status connection lifecycle state; defaults to {@link IntegrationStatus#DISCONNECTED} when
 *     {@code null}
 * @param connectedAt when the connection was established; {@code null} while disconnected
 * @param lastSyncAt when the last sync attempt that actually ran completed; {@code null} if never
 *     synced
 * @param lastSyncOutcome the last sync's result; {@code null} if never synced
 */
public record IntegrationConnection(
    IntegrationProvider provider,
    IntegrationStatus status,
    Instant connectedAt,
    Instant lastSyncAt,
    SyncOutcome lastSyncOutcome) {

  public IntegrationConnection {
    if (provider == null) {
      throw new IllegalArgumentException("provider must not be null");
    }
    if (status == null) {
      status = IntegrationStatus.DISCONNECTED;
    }
  }

  /** The default state for a provider that has never been connected (spec FOR-126 Edge Cases). */
  public static IntegrationConnection disconnectedDefault(IntegrationProvider provider) {
    return new IntegrationConnection(provider, IntegrationStatus.DISCONNECTED, null, null, null);
  }

  /**
   * Marks the provider connected as of {@code now}. Idempotent when already connected (spec FOR-126
   * Open Questions: "connect-when-connected → idempotent, stays CONNECTED") — the original {@link
   * #connectedAt} is preserved rather than overwritten by a reconnect.
   */
  public IntegrationConnection connect(Instant now) {
    if (status == IntegrationStatus.CONNECTED) {
      return this;
    }
    return new IntegrationConnection(
        provider, IntegrationStatus.CONNECTED, now, lastSyncAt, lastSyncOutcome);
  }

  /**
   * Marks the provider disconnected and clears {@link #connectedAt}. Idempotent no-op when already
   * disconnected (spec FOR-126 Edge Cases). Sync history ({@link #lastSyncAt}/{@link
   * #lastSyncOutcome}) is preserved, not cleared — it is an honest record of what happened while
   * the connection was active, and the API never claims a sync happened "now" because of a
   * disconnect.
   */
  public IntegrationConnection disconnect() {
    if (status == IntegrationStatus.DISCONNECTED) {
      return this;
    }
    return new IntegrationConnection(
        provider, IntegrationStatus.DISCONNECTED, null, lastSyncAt, lastSyncOutcome);
  }

  /** Records a sync attempt that actually ran, at {@code syncedAt}, with the given outcome. */
  public IntegrationConnection withSyncOutcome(Instant syncedAt, SyncOutcome outcome) {
    return new IntegrationConnection(provider, status, connectedAt, syncedAt, outcome);
  }

  /**
   * Marks the provider {@link IntegrationStatus#PENDING} while a real OAuth authorization
   * round-trip is in flight (FOR-131): an authorization URL was issued and the callback has not
   * completed yet. Never downgrades an already-{@link IntegrationStatus#CONNECTED} provider —
   * re-authorizing an already-working connection must not make it look disconnected/pending in the
   * meantime (spec FOR-131 Open Questions: "Connect when already CONNECTED → document").
   */
  public IntegrationConnection awaitingCallback() {
    if (status == IntegrationStatus.CONNECTED) {
      return this;
    }
    return new IntegrationConnection(
        provider, IntegrationStatus.PENDING, connectedAt, lastSyncAt, lastSyncOutcome);
  }

  /**
   * Marks the provider {@link IntegrationStatus#NEEDS_REAUTH} after a token refresh failure
   * (FOR-131 Edge Cases: "Refresh failure → mark connection needing re-auth; do not silently
   * drop"). {@link #connectedAt}, {@link #lastSyncAt} and {@link #lastSyncOutcome} are preserved —
   * this is a readable "was connected, now needs the user to reconnect" state, not a reset. A
   * connection that was never connected has nothing to re-authorize, so this is a no-op for {@link
   * IntegrationStatus#DISCONNECTED}.
   */
  public IntegrationConnection needsReauth() {
    if (status == IntegrationStatus.DISCONNECTED) {
      return this;
    }
    return new IntegrationConnection(
        provider, IntegrationStatus.NEEDS_REAUTH, connectedAt, lastSyncAt, lastSyncOutcome);
  }
}
