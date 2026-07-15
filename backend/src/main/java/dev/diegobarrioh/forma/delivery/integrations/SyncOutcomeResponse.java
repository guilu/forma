package dev.diegobarrioh.forma.delivery.integrations;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.domain.SyncOutcome;

/**
 * Nested delivery read model for a connection's last sync outcome (FOR-126 api.md, {@code GET
 * /integrations}). Carries no token/secret (ADR-004) — only the safe, user-readable fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SyncOutcomeResponse(String result, int importedCount, String message) {

  public static SyncOutcomeResponse from(SyncOutcome outcome) {
    if (outcome == null) {
      return null;
    }
    return new SyncOutcomeResponse(
        outcome.result().name(), outcome.importedCount(), outcome.message());
  }
}
