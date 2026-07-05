package dev.diegobarrioh.forma.adapter.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation id to every request and exposes it to logging (FOR-91, ADR-008).
 *
 * <p>Reads {@value #HEADER} from the incoming request or generates a fresh id, puts it in the SLF4J
 * {@link MDC} under {@value #MDC_KEY} so every log line for the request carries it, and echoes it
 * back on the response header. A minimal, safe request log (method, path, status, duration only —
 * never headers, query strings, bodies, tokens or personal data) is emitted on completion.
 *
 * <p>Runs first so downstream logging always has the correlation id available.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  /** Header used to receive and return the correlation id. */
  public static final String HEADER = "X-Correlation-Id";

  /** MDC key under which the correlation id is stored for logging. */
  public static final String MDC_KEY = "correlationId";

  private static final int MAX_LENGTH = 64;

  private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = sanitize(request.getHeader(HEADER));
    if (correlationId.isEmpty()) {
      correlationId = UUID.randomUUID().toString();
    }

    MDC.put(MDC_KEY, correlationId);
    response.setHeader(HEADER, correlationId);

    long startNanos = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
      log.info(
          "{} {} -> {} ({} ms)",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
      MDC.remove(MDC_KEY);
    }
  }

  /**
   * Keep only safe characters from a client-supplied id and cap its length, to avoid log forging
   * and oversized values. Returns an empty string when nothing usable remains.
   */
  private static String sanitize(String value) {
    if (value == null) {
      return "";
    }
    String cleaned = value.replaceAll("[^A-Za-z0-9_-]", "");
    return cleaned.length() > MAX_LENGTH ? cleaned.substring(0, MAX_LENGTH) : cleaned;
  }
}
