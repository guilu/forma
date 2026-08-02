package dev.diegobarrioh.forma.adapter.mercadona;

import dev.diegobarrioh.forma.application.StoreCatalogUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Real {@link MercadonaHttpTransport} on the JDK's {@link HttpClient} — no new dependency, matching
 * {@code JdkHttpWithingsTransport} (docs/coding-standards.md: "prefer boring code").
 *
 * <p>Identifies itself in the User-Agent. This is somebody else's shop, reached through an API they
 * never published: the least we can do is be nameable in their logs and give up quickly rather than
 * hold connections open.
 */
@Component
public class JdkHttpMercadonaTransport implements MercadonaHttpTransport {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
  private static final String USER_AGENT = "forma/1.0 (personal nutrition planner)";

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

  @Override
  public String get(String url) {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .GET()
            .build();
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        throw new StoreCatalogUnavailableException(
            "Mercadona respondió con HTTP " + response.statusCode());
      }
      return response.body();
    } catch (IOException ex) {
      throw new StoreCatalogUnavailableException("No se pudo contactar con Mercadona", ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new StoreCatalogUnavailableException("Consulta a Mercadona interrumpida", ex);
    }
  }
}
