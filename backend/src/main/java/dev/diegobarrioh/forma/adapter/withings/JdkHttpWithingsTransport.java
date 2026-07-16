package dev.diegobarrioh.forma.adapter.withings;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Real {@link WithingsHttpTransport} using the JDK's built-in {@link HttpClient} (FOR-131) — no new
 * Gradle dependency needed (no RestTemplate/WebClient/OkHttp elsewhere in this codebase, so the JDK
 * client keeps this addition boring, per {@code docs/coding-standards.md}: "Prefer clear, boring
 * code over clever code.").
 *
 * <p>Never logs the request body (which carries {@code client_secret}/{@code code}/{@code
 * refresh_token}) or the response body (which carries tokens) — ADR-008.
 */
@Component
public class JdkHttpWithingsTransport implements WithingsHttpTransport {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

  @Override
  public String post(String url, Map<String, String> formParams) {
    String body = encodeForm(formParams);
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        throw new IllegalStateException(
            "Withings request failed with HTTP status " + response.statusCode());
      }
      return response.body();
    } catch (IOException ex) {
      throw new IllegalStateException("Withings request failed: I/O error", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Withings request was interrupted", ex);
    }
  }

  private static String encodeForm(Map<String, String> formParams) {
    return formParams.entrySet().stream()
        .map(
            entry ->
                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));
  }
}
