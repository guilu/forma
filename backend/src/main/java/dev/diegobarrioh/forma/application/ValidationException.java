package dev.diegobarrioh.forma.application;

/**
 * Thrown by application use cases or delivery-layer parsing when caller input fails validation
 * outside of bean-validation's {@code @Valid} request-body checks — e.g. an unrecognized
 * path-variable enum value such as an unknown integration {@code provider} (spec FOR-126 api.md:
 * "Unknown provider path value → 400 VALIDATION_ERROR"). The delivery layer maps it to a {@code
 * VALIDATION_ERROR} (400) {@code ApiError}, mirroring {@link NotFoundException}'s 404 mapping.
 * Defined in the application layer so use cases and controllers can throw it without delivery
 * depending back on them (ADR-001).
 */
public class ValidationException extends RuntimeException {
  public ValidationException(String message) {
    super(message);
  }
}
