package dev.diegobarrioh.forma.adapter.withings;

import java.util.Map;

/**
 * Minimal HTTP transport seam for {@link WithingsOAuthAdapter} (FOR-131). Exists purely so tests
 * can substitute a fixture-backed fake instead of performing a real network call (spec FOR-131
 * tests.md: "Never call the live Withings API in tests — use recorded fixtures / mocked HTTP");
 * {@link JdkHttpWithingsTransport} is the only production implementation.
 */
public interface WithingsHttpTransport {

  /**
   * POSTs {@code formParams} as {@code application/x-www-form-urlencoded} to {@code url} and
   * returns the raw response body. Withings embeds errors in the body of an HTTP 200 response (a
   * {@code status} field), so this does not throw for a non-{@code 0} Withings status — only for a
   * genuine transport failure (connection refused, timeout, non-2xx HTTP status, ...).
   *
   * @throws RuntimeException if the request cannot be completed
   */
  String post(String url, Map<String, String> formParams);
}
