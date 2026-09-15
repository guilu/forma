package dev.diegobarrioh.forma.delivery.security;

import dev.diegobarrioh.forma.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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

  /**
   * Tears down any authentication tied to this request: clears the thread-local {@link
   * SecurityContextHolder} and invalidates the HTTP session if one exists. Used when a login
   * attempt is rejected <em>after</em> Spring Security's own filter has already saved a
   * not-yet-domain-validated {@code Authentication} into the session — {@code
   * GoogleOAuth2SuccessHandler}'s case: {@code AbstractAuthenticationProcessingFilter} persists the
   * raw {@code OAuth2AuthenticationToken}/{@code OidcUser} principal via the {@link
   * SecurityContextRepository} <em>before</em> calling the success handler, so a business-rule
   * rejection inside the handler (unverified email, inactive account, re-link conflict) must
   * explicitly undo that half-built session rather than merely refusing to build a fresh one.
   */
  public void clearSession(HttpServletRequest request) {
    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }
}
