package dev.diegobarrioh.forma.delivery.security;

import dev.diegobarrioh.forma.application.CurrentUserProvider;
import dev.diegobarrioh.forma.application.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * {@link CurrentUserProvider} adapter reading {@link SecurityContextHolder} (FOR-145, ADR-012
 * design §4). The only place in the application that reads the Spring Security context directly —
 * every use case depends on the {@link CurrentUserProvider} port instead (ADR-001).
 *
 * <p>Throwing here is defense-in-depth: the {@code SecurityFilterChain} should already reject an
 * unauthenticated request to a protected endpoint before any use case runs, so an unauthenticated
 * {@link Authentication} reaching this point means a port was called somewhere the filter chain
 * does not cover.
 */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

  @Override
  public UUID currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof FormaUserPrincipal principal)) {
      throw new UnauthorizedException("Authentication required");
    }
    return principal.id();
  }
}
