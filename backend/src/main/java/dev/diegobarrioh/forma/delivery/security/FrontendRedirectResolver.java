package dev.diegobarrioh.forma.delivery.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves a path the browser is redirected to after a Google login attempt (ADR-014) into the URL
 * actually sent in the {@code Location} header — shared by {@code SecurityConfig}'s {@code
 * oauth2Login} failure handler, {@link GoogleOAuth2SuccessHandler} (success and its own rejection
 * path), and {@link GoogleOAuth2FallbackController}.
 *
 * <p>Defaults to empty, which resolves every path unchanged (a same-origin relative redirect) —
 * this is correct in every supported flow, not just prod: {@code frontend/nginx.conf} proxies
 * {@code /api/} to the backend behind one origin in both production and Docker Compose, and Vite's
 * dev server ({@code vite.config.ts}'s {@code server.proxy['/api']}) does the same for plain {@code
 * npm run dev} — in all three, the browser only ever talks to the frontend's own origin, and the
 * backend's redirect {@code Location} header is resolved against whatever origin the browser is
 * actually on. An absolute {@code forma.frontend-url} is only needed for the one flow that
 * genuinely differs: hitting the backend directly on its own port, bypassing every proxy (e.g.
 * manually exercising {@code ./gradlew bootRun} without the frontend running at all) — not a
 * supported way to exercise Google login, so it is not the default.
 */
@Component
public class FrontendRedirectResolver {

  private final String frontendUrl;

  public FrontendRedirectResolver(@Value("${forma.frontend-url:}") String frontendUrl) {
    this.frontendUrl = frontendUrl == null ? "" : frontendUrl.strip();
  }

  /**
   * @param path an absolute path starting with {@code /}, e.g. {@code "/app"}.
   */
  public String resolve(String path) {
    return frontendUrl.isEmpty() ? path : frontendUrl + path;
  }
}
