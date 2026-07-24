package dev.diegobarrioh.forma.application;

/**
 * Thrown by application use cases when a request conflicts with existing state — first use:
 * self-registration with an email that already has an account (FOR-145, ADR-012 spec: "Duplicate
 * email rejected"). The delivery layer maps it to a {@code CONFLICT} (409) {@code ApiError},
 * mirroring {@link NotFoundException}/{@link ForbiddenException}. Defined in the application layer
 * so use cases can throw it without depending on delivery (ADR-001).
 */
public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }
}
