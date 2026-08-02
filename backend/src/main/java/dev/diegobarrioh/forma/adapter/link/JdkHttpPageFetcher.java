package dev.diegobarrioh.forma.adapter.link;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fetches a page with the JDK's {@link HttpClient} (FOR-200) — no new dependency, matching the
 * Withings and Mercadona transports.
 *
 * <p>Reads at most {@link #MAX_BYTES}. A product page is not a download: Amazon's is 2.7 MB and the
 * photo is in the first 500 KB, so a cap keeps a hostile or merely enormous page from being pulled
 * into memory whole. The body is truncated rather than rejected — the tag is near the top of the
 * document either way.
 *
 * <p>Redirects are followed by the client, but every hop is re-checked against the same
 * public-address rule as the original URL: a public host is free to redirect to a private one, and
 * that is the interesting half of the attack.
 */
@Component
public class JdkHttpPageFetcher implements PageFetcher {

  private static final Logger log = LoggerFactory.getLogger(JdkHttpPageFetcher.class);

  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final int MAX_BYTES = 1_000_000;
  private static final int MAX_HOPS = 3;
  private static final String USER_AGENT =
      "Mozilla/5.0 (compatible; forma/1.0; personal nutrition planner)";

  private final HttpClient httpClient =
      HttpClient.newBuilder()
          // Never automatic: each hop has to pass the same address check as the
          // URL that was typed, so they are followed by hand below.
          .followRedirects(HttpClient.Redirect.NEVER)
          .connectTimeout(TIMEOUT)
          .build();

  @Override
  public Optional<String> fetch(String url) {
    String current = url;
    for (int hop = 0; hop <= MAX_HOPS; hop++) {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(current))
              .timeout(TIMEOUT)
              .header("User-Agent", USER_AGENT)
              .header("Accept", "text/html,application/xhtml+xml")
              .GET()
              .build();
      HttpResponse<InputStream> response;
      try {
        response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
      } catch (IOException ex) {
        log.info("No se pudo leer el enlace {} ({})", current, ex.getMessage());
        return Optional.empty();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return Optional.empty();
      }
      int status = response.statusCode();
      if (status / 100 == 3) {
        Optional<String> location = response.headers().firstValue("location");
        if (location.isEmpty()) {
          return Optional.empty();
        }
        current = URI.create(current).resolve(location.get()).toString();
        // The whole point of following by hand.
        HttpLinkPreview.requirePublicHttpUrl(current, java.net.InetAddress::getAllByName);
        continue;
      }
      if (status / 100 != 2) {
        log.info("El enlace {} respondió {}", current, status);
        return Optional.empty();
      }
      return read(response);
    }
    return Optional.empty();
  }

  private static Optional<String> read(HttpResponse<InputStream> response) {
    try (InputStream body = response.body()) {
      return Optional.of(new String(body.readNBytes(MAX_BYTES), StandardCharsets.UTF_8));
    } catch (IOException ex) {
      return Optional.empty();
    }
  }
}
