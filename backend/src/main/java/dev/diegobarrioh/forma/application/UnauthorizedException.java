package dev.diegobarrioh.forma.application;

/**
 * Thrown when a use case requires an authenticated caller and none is present (FOR-145, ADR-012).
 * In practice this is defense-in-depth: the Spring Security filter chain should already reject an
 * unauthenticated request to a protected endpoint before any use case runs, so this is only reached
 * if a port is invoked outside that chain (e.g. {@link CurrentUserProvider} called with no security
 * context). The delivery layer maps it to an {@code UNAUTHORIZED} (401) {@code ApiError}, mirroring
 * {@link NotFoundException}/{@link ForbiddenException}. Defined in the application layer so use
 * cases and ports can throw it without depending on delivery (ADR-001).
 */
public class UnauthorizedException extends RuntimeException {
  public UnauthorizedException(String message) {
    super(message);
  }
}
