package dev.diegobarrioh.forma.delivery.integrations;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import java.time.Instant;

/**
 * Response body for {@code POST /api/v1/integrations/{provider}/sync} (FOR-126 api.md, extended by
 * FOR-132 with {@code duplicatesSkipped}): the outcome fields flattened with the connection's
 * {@code lastSyncAt}, unlike the nested {@link SyncOutcomeResponse} used inside the status list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SyncResponse(
    String result, int importedCount, int duplicatesSkipped, Instant lastSyncAt, String message) {

  public static SyncResponse from(IntegrationConnection connection) {
    var outcome = connection.lastSyncOutcome();
    return new SyncResponse(
        outcome.result().name(),
        outcome.importedCount(),
        outcome.duplicatesSkipped(),
        connection.lastSyncAt(),
        outcome.message());
  }
}
