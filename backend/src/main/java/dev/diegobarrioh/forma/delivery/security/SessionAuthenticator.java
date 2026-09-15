package dev.diegobarrioh.forma.delivery.security;

import dev.diegobarrioh.forma.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Establishes an authenticated HTTP session (ADR-012), shared by every login entry point: password
 * login ({@code AuthController#login}) and Google login ({@code GoogleOAuth2SuccessHandler}).
 * Extracted so both authenticate a user into the session the exact same way rather than duplicating
 * the rotation/save sequence (session-fixation protection matters regardless of how the caller
 * proved who they are).
 */
@Component
public class SessionAuthenticator {

  private final SecurityContextRepository securityContextRepository;

  public SessionAuthenticator(SecurityContextRepository securityContextRepository) {
    this.securityContextRepository = securityContextRepository;
  }

  /**
   * Builds the {@link Authentication} for an already-resolved account: a {@link FormaUserPrincipal}
   * wrapped in a {@link UsernamePasswordAuthenticationToken} with no credentials (there is nothing
   * left to verify — the caller already proved identity, whether via password or Google) and the
   * account's authorities.
   */
  public Authentication authenticationFor(User user) {
    FormaUserPrincipal principal = FormaUserPrincipal.from(user);
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }

  /**
   * Rotates the session id (session-fixation protection — only meaningful when a session already
   * exists; the CSRF-priming request never creates one) and persists {@code authentication} via the
   * {@link SecurityContextRepository}, mirroring what the filter-driven path does for every other
   * authenticated request.
   */
  public void establishSession(
      Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    if (request.getSession(false) != null) {
      request.changeSessionId();
    }
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);
  }
}
