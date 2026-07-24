package dev.diegobarrioh.forma.delivery.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diegobarrioh.forma.delivery.error.ApiError;
import dev.diegobarrioh.forma.delivery.error.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Rejects an authenticated-but-forbidden request (including a missing/invalid CSRF token — Spring
 * Security's {@code CsrfFilter} throws {@link AccessDeniedException}, FOR-145 spec: "Missing CSRF
 * token -> 403") with the standard {@link ApiError} JSON shape (FOR-88, ADR-005), mirroring {@link
 * ApiAuthenticationEntryPoint}.
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ApiError error =
        ApiError.of(
            ApiErrorCode.FORBIDDEN, "Access denied", request.getHeader("X-Correlation-Id"), null);
    objectMapper.writeValue(response.getWriter(), error);
  }
}
