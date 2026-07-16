package dev.diegobarrioh.forma.domain;

/**
 * The result of a manual sync attempt (FOR-126, extended by FOR-132): a small embedded value on
 * {@link IntegrationConnection}, never a separate aggregate — there is at most one "last" outcome
 * per connection (spec FOR-126 Data Model Notes).
 *
 * <p>{@code importedCount} and {@code duplicatesSkipped} are never fabricated (spec FOR-126/FOR-132
 * Functional Requirements). FOR-126 could only ever produce {@code importedCount = 0} (stub/no-op
 * import); FOR-132 adds a real Withings Getmeas import, so {@link SyncResult#OK} now carries real
 * counts, and {@code duplicatesSkipped} counts measure groups already imported on a previous sync
 * (idempotent dedup, ADR-004/spec FOR-132). Carries no token/secret field (ADR-004) — a sync
 * outcome is always safe to render to the user as-is.
 *
 * @param result what happened
 * @param importedCount number of records imported; never negative, never fabricated
 * @param duplicatesSkipped number of provider measure groups skipped because they were already
 *     imported on a previous sync (FOR-132); never negative, always {@code 0} for outcomes that
 *     performed no real import (FOR-126 stub, {@link SyncResult#NOT_CONNECTED}, {@link
 *     SyncResult#NEEDS_REAUTH}, {@link SyncResult#ERROR})
 * @param message optional human-readable detail (e.g. why {@link SyncResult#NOT_CONNECTED})
 */
public record SyncOutcome(
    SyncResult result, int importedCount, int duplicatesSkipped, String message) {

  public SyncOutcome {
    if (result == null) {
      throw new IllegalArgumentException("result must not be null");
    }
    if (importedCount < 0) {
      throw new IllegalArgumentException(
          "importedCount must not be negative, was: " + importedCount);
    }
    if (duplicatesSkipped < 0) {
      throw new IllegalArgumentException(
          "duplicatesSkipped must not be negative, was: " + duplicatesSkipped);
    }
  }
}
