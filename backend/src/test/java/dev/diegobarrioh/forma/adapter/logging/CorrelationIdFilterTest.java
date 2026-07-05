package dev.diegobarrioh.forma.adapter.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Verifies the correlation-id baseline (FOR-91): an id is generated when absent, a provided id is
 * propagated, unsafe input is sanitized, the id is available in the MDC during the request and
 * removed afterwards.
 */
class CorrelationIdFilterTest {

  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void generatesCorrelationIdWhenHeaderMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ping");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> mdcDuringRequest = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (req, res) -> mdcDuringRequest.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

    String returned = response.getHeader(CorrelationIdFilter.HEADER);
    assertThat(returned).isNotBlank();
    assertThat(mdcDuringRequest.get()).isEqualTo(returned);
    // MDC is cleared once the request completes.
    assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void propagatesProvidedCorrelationId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ping");
    request.addHeader(CorrelationIdFilter.HEADER, "abc-123");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> mdcDuringRequest = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (req, res) -> mdcDuringRequest.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

    assertThat(mdcDuringRequest.get()).isEqualTo("abc-123");
    assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("abc-123");
  }

  @Test
  void sanitizesUnsafeCorrelationId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/ping");
    request.addHeader(CorrelationIdFilter.HEADER, "bad\r\nvalue with spaces");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    String returned = response.getHeader(CorrelationIdFilter.HEADER);
    assertThat(returned).doesNotContain("\n", "\r", " ").isEqualTo("badvaluewithspaces");
  }
}
