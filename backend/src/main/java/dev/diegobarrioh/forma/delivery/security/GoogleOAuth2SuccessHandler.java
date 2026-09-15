package dev.diegobarrioh.forma.delivery.security;

import dev.diegobarrioh.forma.application.GoogleIdentity;
import dev.diegobarrioh.forma.application.UserService;
import dev.diegobarrioh.forma.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Completes a successful Google login (ADR-012 addendum). Session-based, unlike akademia's
 * JWT-in-a-redirect-URL flow: nothing token-like ever crosses the browser here. It maps the
 * authenticated {@link OidcUser}'s claims into a framework-free {@link GoogleIdentity}, resolves it
 * to a FORMA account via {@link UserService#loginWithGoogle}, establishes the session exactly like
 * {@code AuthController#login} does (via the shared {@link SessionAuthenticator}), records the
 * login, and redirects the browser back to the SPA's default authenticated destination.
 */
@Component
public class GoogleOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final UserService userService;
  private final SessionAuthenticator sessionAuthenticator;
  private final String frontendUrl;

  public GoogleOAuth2SuccessHandler(
      UserService userService,
      SessionAuthenticator sessionAuthenticator,
      @Value("${forma.frontend-url:http://localhost:5173}") String frontendUrl) {
    this.userService = userService;
    this.sessionAuthenticator = sessionAuthenticator;
    this.frontendUrl = frontendUrl;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws java.io.IOException {
    OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
    OidcIdToken idToken = oidcUser.getIdToken();
    GoogleIdentity identity =
        new GoogleIdentity(
            idToken.getSubject(),
            idToken.getEmail(),
            Boolean.TRUE.equals(idToken.getEmailVerified()));

    User user = userService.loginWithGoogle(identity);
    sessionAuthenticator.establishSession(
        sessionAuthenticator.authenticationFor(user), request, response);
    userService.recordSuccessfulLogin(user.id());

    // "/app" is the SPA's own default authenticated destination (authDestination.ts). The
    // pre-login "from" location is not preserved across the Google redirect round trip — a known,
    // documented limitation (the PR notes), not an oversight.
    getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/app");
  }
}
