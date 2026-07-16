package dev.diegobarrioh.forma.domain;

/**
 * Outcome classification for a {@link SyncOutcome} (FOR-126, extended by FOR-132).
 *
 * <p>FOR-126 only had {@link #OK} (stub/no-op import) and {@link #NOT_CONNECTED} (spec FOR-126 Edge
 * Cases: "Sync on a DISCONNECTED provider → 409 or a readable outcome; document" — this slice picks
 * the readable-outcome option to keep the FOR-57/FOR-123 frontend error handling simple). FOR-132
 * adds the two failure outcomes a real Withings Getmeas sync can actually produce: {@link
 * #NEEDS_REAUTH} (token refresh failed — spec FOR-132 Edge Cases: "Token expired → refreshed first;
 * refresh failure → NEEDS_REAUTH") and {@link #ERROR} (Withings unreachable/rate-limited/5xx — spec
 * FOR-132 Edge Cases: "readable failure outcome, connection not corrupted, no secret leak, no
 * crash"). Every value here is surfaced to the client as a 200 {@code result} field, never an HTTP
 * 5xx (spec FOR-132 api.md).
 */
public enum SyncResult {
  /** The sync ran without error; {@code importedCount}/{@code duplicatesSkipped} are real. */
  OK,
  /** The provider was not connected; no sync was performed. */
  NOT_CONNECTED,
  /** The access token could not be refreshed; the user must reconnect (FOR-132). */
  NEEDS_REAUTH,
  /** The provider call failed (unreachable, rate-limited, 5xx, unparseable) (FOR-132). */
  ERROR
}
