package dev.diegobarrioh.forma.domain;

/**
 * The result of a manual sync attempt (FOR-126): a small embedded value on {@link
 * IntegrationConnection}, never a separate aggregate — there is at most one "last" outcome per
 * connection (spec FOR-126 Data Model Notes).
 *
 * <p>{@code importedCount} is never fabricated (spec FOR-126 Functional Requirements): this slice
 * performs a stub/no-op import, so every {@link SyncResult#OK} outcome it produces carries {@code
 * 0}. Real counts arrive with real Withings sync (FOR-103 slice 3). Carries no token/secret field
 * (ADR-004) — a sync outcome is always safe to render to the user as-is.
 *
 * @param result what happened
 * @param importedCount number of records imported; never negative, never fabricated
 * @param message optional human-readable detail (e.g. why {@link SyncResult#NOT_CONNECTED})
 */
public record SyncOutcome(SyncResult result, int importedCount, String message) {

  public SyncOutcome {
    if (result == null) {
      throw new IllegalArgumentException("result must not be null");
    }
    if (importedCount < 0) {
      throw new IllegalArgumentException(
          "importedCount must not be negative, was: " + importedCount);
    }
  }
}
