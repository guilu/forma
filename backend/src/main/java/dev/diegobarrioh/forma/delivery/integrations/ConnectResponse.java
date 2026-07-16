package dev.diegobarrioh.forma.delivery.integrations;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.ConnectResult;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import java.time.Instant;

/**
 * Response body for {@code POST /{provider}/connect} (FOR-131 api.md, changed from FOR-126).
 * Exactly one shape is populated per {@link ConnectResult}: {@link #authorizationUrl} alone for a
 * provider with a real OAuth gateway (Withings), or {@link #provider}/{@link #status}/{@link
 * #connectedAt} alone for a provider still on the FOR-126 mock-connect fallback — null fields are
 * omitted from the JSON so each response looks like a single coherent shape to the client, not a
 * sparse union.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConnectResponse(
    String authorizationUrl, String provider, String status, Instant connectedAt) {

  public static ConnectResponse from(ConnectResult result) {
    if (result.authorizationUrl() != null) {
      return new ConnectResponse(result.authorizationUrl(), null, null, null);
    }
    IntegrationConnection connection = result.connection();
    return new ConnectResponse(
        null, connection.provider().name(), connection.status().name(), connection.connectedAt());
  }
}
