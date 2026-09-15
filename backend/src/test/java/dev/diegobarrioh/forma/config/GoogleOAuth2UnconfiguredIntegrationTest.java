package dev.diegobarrioh.forma.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * With no {@code GOOGLE_CLIENT_ID}/{@code GOOGLE_CLIENT_SECRET} configured (the {@code test}
 * profile's default — {@code application-test.yml} sets neither), {@code SecurityConfig} never
 * registers {@code oauth2Login()} (ADR-012 addendum). {@link
 * dev.diegobarrioh.forma.delivery.security.GoogleOAuth2FallbackController} must answer the
 * authorization path instead, so an anonymous visitor gets a page to act on rather than a bare
 * 404/500.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GoogleOAuth2UnconfiguredIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void authorizationPathRedirectsToTheFrontendLoginPageWithAnErrorFlag() throws Exception {
    // Deliberately NOT TestRestTemplate: it follows redirects by default, and the redirect target
    // (the frontend's own dev origin) is never running in this backend-only test.
    HttpClient httpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(restTemplate.getRootUri() + "/api/oauth2/authorization/google"))
            .GET()
            .build();

    HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

    assertThat(response.statusCode()).isBetween(300, 399);
    assertThat(response.headers().firstValue("Location"))
        .contains("http://localhost:5173/login?error=google");
  }
}
