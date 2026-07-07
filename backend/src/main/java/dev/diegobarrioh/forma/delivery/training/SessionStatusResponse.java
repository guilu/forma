package dev.diegobarrioh.forma.delivery.training;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.StoredSessionStatus;

/**
 * Response body for {@code PATCH /api/v1/training/sessions/{id}/status} (FOR-27): the updated
 * session status. Null {@code notes} are omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionStatusResponse(String id, String status, String notes) {

  public static SessionStatusResponse from(StoredSessionStatus stored) {
    return new SessionStatusResponse(stored.sessionId(), stored.status().name(), stored.notes());
  }
}
