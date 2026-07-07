package dev.diegobarrioh.forma.application;

/**
 * Thrown by application use cases when a requested resource does not exist (FOR-27). The delivery
 * layer maps it to a {@code NOT_FOUND} (404) {@code ApiError}. Defined in the application layer so
 * use cases can throw it without depending on delivery (ADR-001).
 */
public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }
}
