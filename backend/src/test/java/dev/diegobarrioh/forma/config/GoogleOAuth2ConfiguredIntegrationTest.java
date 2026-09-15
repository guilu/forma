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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * With {@code GOOGLE_CLIENT_ID}/{@code GOOGLE_CLIENT_SECRET} set, {@code SecurityConfig} registers
 * {@code oauth2Login()} (ADR-012 addendum) — so the same authorization path that {@link
 * GoogleOAuth2UnconfiguredIntegrationTest} redirects to the frontend for now redirects to Google
 * itself, permitAll'd and reachable by an anonymous visitor.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GoogleOAuth2ConfiguredIntegrationTest {

  @DynamicPropertySource
  static void googleOAuth2Credentials(DynamicPropertyRegistry registry) {
    registry.add("forma.oauth2.google.client-id", () -> "test-google-client-id");
    registry.add("forma.oauth2.google.client-secret", () -> "test-google-client-secret");
  }

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void authorizationPathRedirectsToGoogleWithThisEnvironmentsClientId() throws Exception {
    HttpClient httpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(restTemplate.getRootUri() + "/api/oauth2/authorization/google"))
            .GET()
            .build();

    HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

    assertThat(response.statusCode()).isBetween(300, 399);
    String location = response.headers().firstValue("Location").orElseThrow();
    assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
    assertThat(location).contains("client_id=test-google-client-id");
  }

  @Test
  void unauthenticatedRequestsToOrdinaryEndpointsStillReturn401() {
    org.springframework.http.ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/auth/me", String.class);

    assertThat(response.getStatusCode())
        .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
  }
}
