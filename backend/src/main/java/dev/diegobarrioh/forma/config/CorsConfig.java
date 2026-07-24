package dev.diegobarrioh.forma.config;

import dev.diegobarrioh.forma.delivery.ApiPaths;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for the browser frontend (ADR-005/ADR-006, ADR-012).
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
 * shortcuts) — mandatory once credentials are enabled (ADR-012).
 *
 * <p>Exposes a single {@link CorsConfigurationSource} bean consumed by {@code
 * SecurityConfig#securityFilterChain} (ADR-012 design §"CORS"). Session-cookie auth (FOR-145)
 * requires cross-origin requests to carry credentials, so {@code allowCredentials} is now {@code
 * true} — this previously used a separate {@code WebMvcConfigurer} mapping with credentials
 * disabled (single-user MVP, pre-auth); that mapping is replaced by this one source of truth so
 * Spring MVC and Spring Security never disagree on the CORS policy.
 */
@Configuration
public class CorsConfig {

  private final List<String> allowedOrigins;

  public CorsConfig(
      @Value("${forma.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
          List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    // The SPA must be able to read the CSRF cookie's paired header contract (ADR-012 CSRF
    // section); the cookie itself is JS-readable (CookieCsrfTokenRepository.withHttpOnlyFalse()),
    // no extra exposed-header is required for the cookie, but naming it here documents intent.
    configuration.setExposedHeaders(List.of("X-XSRF-TOKEN"));
    // Session-cookie auth requires credentialed cross-origin requests — explicit origins only,
    // never a wildcard, is mandatory once this is true (ADR-012).
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration(ApiPaths.V1 + "/**", configuration);
    return source;
  }
}
