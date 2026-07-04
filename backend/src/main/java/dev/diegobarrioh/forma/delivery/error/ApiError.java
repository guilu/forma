package dev.diegobarrioh.forma.delivery.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Standard API error response shape (FOR-88, ADR-005 / specs/FOR-88/api.md).
 *
 * <p>Consistent across every error so clients can rely on it. Null fields are omitted from the
 * JSON. It never carries stack traces, secrets, tokens or provider payloads.
 *
 * @param code stable machine-readable code
 * @param message safe, human-readable summary
 * @param correlationId request correlation id when available (wired fully by FOR-91); null
 *     otherwise
 * @param details optional per-field validation details
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String code, String message, String correlationId, List<FieldValidationError> details) {

  /** A single field-level validation problem. */
  public record FieldValidationError(String field, String message) {}

  /** Build an error, normalizing empty detail lists to {@code null} so they are omitted. */
  public static ApiError of(
      ApiErrorCode code, String message, String correlationId, List<FieldValidationError> details) {
    List<FieldValidationError> normalized =
        (details == null || details.isEmpty()) ? null : List.copyOf(details);
    return new ApiError(code.name(), message, correlationId, normalized);
  }
}
