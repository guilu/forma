package dev.diegobarrioh.forma.delivery.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Answers {@code GET /api/oauth2/authorization/google} when Google login is not configured in this
 * environment (ADR-012 addendum: {@code GOOGLE_CLIENT_ID}/{@code GOOGLE_CLIENT_SECRET} unset).
 *
 * <p>When Google IS configured, {@code SecurityConfig} registers {@code oauth2Login()}, whose
 * {@code OAuth2AuthorizationRequestRedirectFilter} intercepts this exact path in the security
 * filter chain — upstream of Spring MVC dispatch — and redirects to Google itself, so this
 * controller method is simply never reached. It exists for the other case: unconfigured, the filter
 * is never added, the path is still {@code permitAll}, and without a handler here the request would
 * fall through to a bare container 404 instead of a page the user can act on. Always registered
 * rather than conditionally, since the two cases can never both try to answer the same request.
 */
@RestController
public class GoogleOAuth2FallbackController {

  private final FrontendRedirectResolver frontendRedirectResolver;

  public GoogleOAuth2FallbackController(FrontendRedirectResolver frontendRedirectResolver) {
    this.frontendRedirectResolver = frontendRedirectResolver;
  }

  @GetMapping("/api/oauth2/authorization/google")
  public void googleLoginUnavailable(HttpServletResponse response) throws IOException {
    response.sendRedirect(frontendRedirectResolver.resolve("/login?error=google"));
  }
}
