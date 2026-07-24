package dev.diegobarrioh.forma.delivery.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Forces the deferred CSRF token to render on every request (FOR-145, ADR-012). {@code
 * CsrfTokenRequestAttributeHandler} exposes the token lazily as a {@code Supplier} — nothing calls
 * {@code .getToken()} on a plain API request that never renders a form/view, so without this filter
 * {@link org.springframework.security.web.csrf.CookieCsrfTokenRepository} would never actually
 * write the {@code XSRF-TOKEN} response cookie, and the SPA's "priming GET" (ADR-012 filter-chain
 * section) would have nothing to read. This is the documented Spring Security 6 pattern for
 * cookie-based CSRF with a JS single-page app.
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      // Resolving the token is what actually triggers the repository to write the cookie.
      csrfToken.getToken();
    }
    filterChain.doFilter(request, response);
  }
}
