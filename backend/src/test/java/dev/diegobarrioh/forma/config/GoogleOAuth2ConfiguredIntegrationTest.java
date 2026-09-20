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

  /**
   * Regression test (fresh-review follow-up on ADR-014): {@code server.forward-headers-strategy=
   * framework} (application.yml) must build the OAuth {@code redirect_uri} — the {@code {baseUrl}}
   * half of {@code SecurityConfig#googleClientRegistration}'s template — from {@code
   * X-Forwarded-Host}/{@code X-Forwarded-Proto} rather than from this test server's own real `Host`
   * header (a random port on localhost). This is exactly what {@code frontend/vite.config.ts}'s dev
   * proxy now sends (verified empirically with a throwaway echo server — see its commit) and what
   * {@code frontend/nginx.conf} sends in prod/compose: without it, Google would be asked to
   * redirect back to this backend's own port instead of whichever origin the browser is actually
   * on, landing the user on a page that never serves the SPA.
   */
  @Test
  void authorizationPathBuildsTheRedirectUriFromXForwardedHost() throws Exception {
    HttpClient httpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(restTemplate.getRootUri() + "/api/oauth2/authorization/google"))
            .header("X-Forwarded-Host", "localhost:5173")
            .header("X-Forwarded-Proto", "http")
            .GET()
            .build();

    HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

    assertThat(response.statusCode()).isBetween(300, 399);
    String location = response.headers().firstValue("Location").orElseThrow();
    String redirectUri =
        java.util.Arrays.stream(URI.create(location).getRawQuery().split("&"))
            .filter(param -> param.startsWith("redirect_uri="))
            .map(param -> param.substring("redirect_uri=".length()))
            .map(
                value -> java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No redirect_uri param in: " + location));
    assertThat(redirectUri).isEqualTo("http://localhost:5173/api/login/oauth2/code/google");
  }

  /**
   * Regression coverage for the double-reverse-proxy bug found in a real deployment (public TLS
   * proxy → {@code frontend/nginx.conf} → this backend): the redirect_uri contract this backend
   * half owns. These three pin exactly what {@link
   * #authorizationPathBuildsTheRedirectUriFromXForwardedHost} already pins, plus the two extra
   * cases the bug report needed: a proto/host pair with no port at all (prod, over https), and the
   * same pair with an explicit {@code X-Forwarded-Port} that must NOT leak into the URI as {@code
   * :443}. All three were passing before the nginx fix — the bug was entirely in {@code
   * frontend/nginx.conf} overwriting these headers with its own view instead of forwarding what the
   * outer proxy sent (see the file's header comment and {@code docs/adr/ADR-014-google-login.md}) —
   * so this class only pins the backend's half of the contract; it does not reproduce the nginx bug
   * itself (that needs a real double-nginx setup — see {@code frontend/nginx/test/}).
   */
  @Test
  void authorizationPathBuildsHttpsRedirectUriWithNoPortFromForwardedHeaders() throws Exception {
    String redirectUri = redirectUriFor("https", "forma.backendtothefuture.com", null);

    assertThat(redirectUri)
        .isEqualTo("https://forma.backendtothefuture.com/api/login/oauth2/code/google");
  }

  @Test
  void authorizationPathIgnoresXForwardedPortWhenItMatchesTheSchemesDefaultPort() throws Exception {
    String redirectUri = redirectUriFor("https", "forma.backendtothefuture.com", "443");

    assertThat(redirectUri)
        .isEqualTo("https://forma.backendtothefuture.com/api/login/oauth2/code/google");
  }

  @Test
  void authorizationPathKeepsAnExplicitPortCarriedInsideXForwardedHost() throws Exception {
    String redirectUri = redirectUriFor("http", "localhost:3002", null);

    assertThat(redirectUri).isEqualTo("http://localhost:3002/api/login/oauth2/code/google");
  }

  private String redirectUriFor(String forwardedProto, String forwardedHost, String forwardedPort)
      throws Exception {
    HttpClient httpClient =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(restTemplate.getRootUri() + "/api/oauth2/authorization/google"))
            .header("X-Forwarded-Proto", forwardedProto)
            .header("X-Forwarded-Host", forwardedHost);
    if (forwardedPort != null) {
      requestBuilder.header("X-Forwarded-Port", forwardedPort);
    }

    HttpResponse<Void> response =
        httpClient.send(requestBuilder.GET().build(), HttpResponse.BodyHandlers.discarding());

    assertThat(response.statusCode()).isBetween(300, 399);
    String location = response.headers().firstValue("Location").orElseThrow();
    return java.util.Arrays.stream(URI.create(location).getRawQuery().split("&"))
        .filter(param -> param.startsWith("redirect_uri="))
        .map(param -> param.substring("redirect_uri=".length()))
        .map(value -> java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No redirect_uri param in: " + location));
  }
}
