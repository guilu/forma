package dev.diegobarrioh.forma.delivery.integrations;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import java.time.Instant;

/**
 * Delivery read model for one provider's status row (FOR-126 api.md, {@code GET /integrations}).
 * Never carries a token/secret field — the domain type it is built from carries none either
 * (ADR-004, spec FOR-126 boundary rule).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IntegrationConnectionResponse(
    String provider,
    String status,
    Instant connectedAt,
    Instant lastSyncAt,
    SyncOutcomeResponse lastSyncOutcome) {

  public static IntegrationConnectionResponse from(IntegrationConnection connection) {
    return new IntegrationConnectionResponse(
        connection.provider().name(),
        connection.status().name(),
        connection.connectedAt(),
        connection.lastSyncAt(),
        SyncOutcomeResponse.from(connection.lastSyncOutcome()));
  }
}
