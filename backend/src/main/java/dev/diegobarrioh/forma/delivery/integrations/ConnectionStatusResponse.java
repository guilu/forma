package dev.diegobarrioh.forma.delivery.integrations;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import java.time.Instant;

/**
 * Response body shared by {@code POST /{provider}/connect} and {@code DELETE /{provider}} (FOR-126
 * api.md): the minimal connect/disconnect confirmation shape (no sync fields — those only appear on
 * the status list and the sync response).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConnectionStatusResponse(String provider, String status, Instant connectedAt) {

  public static ConnectionStatusResponse from(IntegrationConnection connection) {
    return new ConnectionStatusResponse(
        connection.provider().name(), connection.status().name(), connection.connectedAt());
  }
}
