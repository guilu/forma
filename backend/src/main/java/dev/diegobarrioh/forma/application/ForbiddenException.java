package dev.diegobarrioh.forma.application;

/**
 * Thrown by application use cases when the caller attempts to access or mutate a resource owned by
 * a different account (ADR-002: "Reject cross-user reads and writes" — enforced even in the
 * single-user MVP, AGENTS.md Forbidden shortcuts: "Bypassing authorization because the MVP is
 * currently single-user"). First thrown by {@link ProgressPhotoService} (FOR-140, progress-photos
 * slice of FOR-104): a photo id that exists but belongs to another owner is 403, distinct from an
 * unknown id (404, {@link NotFoundException}).
 *
 * <p>The delivery layer maps it to a {@code FORBIDDEN} (403) {@code ApiError}, activating the
 * {@link dev.diegobarrioh.forma.delivery.error.ApiErrorCode#FORBIDDEN} code reserved by FOR-88 for
 * this first real use. Defined in the application layer so use cases can throw it without depending
 * on delivery (ADR-001), mirroring {@link NotFoundException} and {@link ValidationException}.
 */
public class ForbiddenException extends RuntimeException {
  public ForbiddenException(String message) {
    super(message);
  }
}
