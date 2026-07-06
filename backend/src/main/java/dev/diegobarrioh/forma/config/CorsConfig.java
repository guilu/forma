package dev.diegobarrioh.forma.config;

import dev.diegobarrioh.forma.delivery.ApiPaths;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for the browser frontend (ADR-005/ADR-006).
 *
 * <p>The single-page app is served from a different origin than the API in every environment (Vite
 * dev server on {@code :5173}, the compose frontend container on {@code :3000}, a real deployment
 * on its own host), so the browser requires the API to opt in to cross-origin requests. Without
 * this, {@code fetch} calls from the frontend are blocked even though the same request succeeds
 * from curl.
 *
 * <p>Allowed origins are configuration-driven via {@code forma.cors.allowed-origins} and default to
 * the local development origins only. Real environments set the property to their exact frontend
 * origin. A production wildcard ({@code *}) is deliberately never used (AGENTS.md forbidden
 * shortcuts). No credentials/cookies are used yet (single-user MVP, auth is a later story), so
 * credentials are not enabled.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final List<String> allowedOrigins;

  public CorsConfig(
      @Value("${forma.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
          List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping(ApiPaths.V1 + "/**")
        .allowedOrigins(allowedOrigins.toArray(String[]::new))
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false);
  }
}
