package dev.diegobarrioh.forma.domain;

/**
 * Outcome classification for a {@link SyncOutcome} (FOR-126).
 *
 * <p>Only the two outcomes this slice can actually produce exist here — a stub/no-op sync always
 * succeeds when connected ({@link #OK}), and syncing a disconnected provider is a readable,
 * non-error outcome ({@link #NOT_CONNECTED}) rather than a thrown exception (spec FOR-126 Edge
 * Cases: "Sync on a DISCONNECTED provider → 409 or a readable outcome; document" — this slice picks
 * the readable-outcome option to keep the FOR-57/FOR-123 frontend error handling simple). Provider
 * failure/rate-limit outcomes are out of scope until real sync exists (FOR-103 slice 3).
 */
public enum SyncResult {
  /** The sync ran (stub/no-op import in this slice) without error. */
  OK,
  /** The provider was not connected; no sync was performed. */
  NOT_CONNECTED
}
