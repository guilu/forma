package dev.diegobarrioh.forma.delivery.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diegobarrioh.forma.delivery.error.ApiError;
import dev.diegobarrioh.forma.delivery.error.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Rejects an unauthenticated request to a protected endpoint with the same {@link ApiError} JSON
 * shape every other error uses (FOR-88, ADR-005) — never Spring Security's default login-page
 * redirect (FOR-145, ADR-012: "custom AuthenticationEntryPoint -> 401 ApiError JSON, NOT a
 * login-page redirect"). Runs at the filter-chain level, before any controller/{@code
 * GlobalExceptionHandler} code — {@link GlobalExceptionHandler} only sees exceptions thrown inside
 * a controller method.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ApiError error =
        ApiError.of(
            ApiErrorCode.UNAUTHORIZED,
            "Authentication required",
            request.getHeader("X-Correlation-Id"),
            null);
    objectMapper.writeValue(response.getWriter(), error);
  }
}
