package dev.diegobarrioh.forma.adapter.planagent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Real {@link PlanAgentTransport} using the JDK's built-in {@link HttpClient}, mirroring {@code
 * adapter/withings}'s {@code JdkHttpWithingsTransport} (no new Gradle dependency, per {@code
 * docs/coding-standards.md}: "Prefer clear, boring code over clever code.").
 *
 * <p><b>Timeout is configurable and defaults to 120 s (ADR-015 decision 10), not Withings' fixed 10
 * s.</b> A model asked to write a week of meals against a full food catalog will not answer in 10
 * s. 120 s is the exact figure the ADR proposes, and the ADR is explicit that it is an unmeasured
 * guess — this slice keeps it rather than substituting a different guess of its own, because no
 * measured data exists yet to justify a different number, and makes it {@code
 * forma.plan-agent.timeout-seconds} so the first real integration can correct it without a code
 * change (ADR-015 Risk 1).
 *
 * <p>Never logs the request body (which may carry personal figures) or the response body (which
 * carries the plan) — ADR-008, mirroring {@code adapter.withings.JdkHttpWithingsTransport} for the
 * same reason.
 */
@Component
public class JdkHttpPlanAgentTransport implements PlanAgentTransport {

  private final Duration requestTimeout;
  private final HttpClient httpClient;

  public JdkHttpPlanAgentTransport(
      @Value("${forma.plan-agent.timeout-seconds:120}") long timeoutSeconds) {
    this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
    this.httpClient = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
  }

  @Override
  public String post(String url, String apiKey, String jsonBody) {
    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(requestTimeout)
            .header("Content-Type", "application/json");
    if (apiKey != null && !apiKey.isBlank()) {
      requestBuilder.header("Authorization", "Bearer " + apiKey);
    }
    HttpRequest request =
        requestBuilder
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        throw new IllegalStateException(
            "Plan agent request failed with HTTP status " + response.statusCode());
      }
      return response.body();
    } catch (IOException ex) {
      throw new IllegalStateException("Plan agent request failed: I/O error", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Plan agent request was interrupted", ex);
    }
  }
}
