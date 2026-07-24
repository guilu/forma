package dev.diegobarrioh.forma.delivery.auth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end auth flow over a real embedded server (FOR-145, ADR-012 spec: Registration & Login
 * requirements). Deliberately uses {@link WebEnvironment#RANDOM_PORT} + {@link TestRestTemplate}
 * rather than {@code MockMvc}: asserting real {@code Set-Cookie} attribute flags
 * (httpOnly/Secure/SameSite) requires an actual servlet container writing the session cookie —
 * MockMvc's mock dispatcher never goes through Tomcat's {@code SessionCookieConfig} and would not
 * exercise this correctly.
 *
 * <p>Cookie-based CSRF (ADR-012) applies to every state-changing request including {@code
 * register}/{@code login}, so every test primes the {@code XSRF-TOKEN} cookie first via an
 * unauthenticated {@code GET} (mirroring the SPA's own priming flow) and echoes it back as the
 * {@code X-XSRF-TOKEN} header, exactly like {@code CsrfCookieFilter}'s javadoc describes.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthenticationFlowIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTestUsers() {
    jdbcTemplate.update("DELETE FROM users WHERE id <> ?", LegacyUserBootstrap.PLACEHOLDER_USER_ID);
  }

  /** A CSRF cookie/token pair primed from an unauthenticated GET, reused across a test's calls. */
  private record Csrf(String cookiePair, String token) {}

  private Csrf primeCsrf() {
    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).as("priming GET must issue the XSRF-TOKEN cookie").isNotNull();
    String xsrfHeader =
        setCookies.stream()
            .filter(c -> c.startsWith("XSRF-TOKEN="))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No XSRF-TOKEN cookie in: " + setCookies));
    String cookiePair = xsrfHeader.split(";", 2)[0];
    String token = cookiePair.substring("XSRF-TOKEN=".length());
    return new Csrf(cookiePair, token);
  }

  private HttpHeaders headersFor(Csrf csrf, String... extraCookiePairs) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    StringBuilder cookieHeader = new StringBuilder(csrf.cookiePair());
    for (String extra : extraCookiePairs) {
      cookieHeader.append("; ").append(extra);
    }
    headers.set(HttpHeaders.COOKIE, cookieHeader.toString());
    headers.set("X-XSRF-TOKEN", csrf.token());
    return headers;
  }

  private static String sessionCookiePair(ResponseEntity<?> response) {
    List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).as("login must set a session cookie").isNotNull();
    String jsessionHeader =
        setCookies.stream()
            .filter(c -> c.startsWith("JSESSIONID="))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No JSESSIONID cookie in: " + setCookies));
    return jsessionHeader.split(";", 2)[0];
  }

  @Test
  void primingGetIssuesAReadableCsrfCookie() {
    Csrf csrf = primeCsrf();

    assertThat(csrf.token()).isNotBlank();
  }

  @Test
  void registerCreatesAnAccountAndReturns201WithoutAPasswordHash() {
    Csrf csrf = primeCsrf();
    HttpEntity<String> request =
        new HttpEntity<>(
            "{\"email\":\"new.user@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headersFor(csrf));

    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/v1/auth/register", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).contains("new.user@x.com");
    assertThat(response.getBody()).doesNotContain("password").doesNotContain("Str0ngP@ssw0rd!");

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, "new.user@x.com");
    assertThat(count).isEqualTo(1);
    String storedHash =
        jdbcTemplate.queryForObject(
            "SELECT password_hash FROM users WHERE email = ?", String.class, "new.user@x.com");
    assertThat(storedHash).startsWith("{argon2}");
  }

  @Test
  void registerRejectsADuplicateEmailWith409AndDoesNotCreateASecondRow() {
    Csrf csrf = primeCsrf();
    HttpEntity<String> firstRequest =
        new HttpEntity<>(
            "{\"email\":\"dup@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headersFor(csrf));
    restTemplate.postForEntity("/api/v1/auth/register", firstRequest, String.class);

    HttpEntity<String> secondRequest =
        new HttpEntity<>(
            "{\"email\":\"dup@x.com\",\"password\":\"AnotherStr0ngP@ss!\"}", headersFor(csrf));
    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/v1/auth/register", secondRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).contains("CONFLICT");
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, "dup@x.com");
    assertThat(count).isEqualTo(1);
  }

  @Test
  void registerRejectsAWeakPasswordWith400() {
    Csrf csrf = primeCsrf();
    HttpEntity<String> request =
        new HttpEntity<>("{\"email\":\"weak@x.com\",\"password\":\"short\"}", headersFor(csrf));

    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/v1/auth/register", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("VALIDATION_ERROR");
  }

  @Test
  void registerWithoutACsrfTokenIsRejectedWith403() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    HttpEntity<String> request =
        new HttpEntity<>("{\"email\":\"nocsrf@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headers);

    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/v1/auth/register", request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void loginWithCorrectCredentialsSetsAnHttpOnlySecureSameSiteLaxCookieAndUpdatesLastLoginAt() {
    Csrf csrf = primeCsrf();
    HttpEntity<String> registerRequest =
        new HttpEntity<>(
            "{\"email\":\"login.ok@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headersFor(csrf));
    restTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    HttpEntity<String> loginRequest =
        new HttpEntity<>(
            "{\"email\":\"login.ok@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headersFor(csrf));
    ResponseEntity<String> response =
        restTemplate.postForEntity("/api/v1/auth/login", loginRequest, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("login.ok@x.com");

    List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).isNotNull();
    String jsessionHeader =
        setCookies.stream().filter(c -> c.startsWith("JSESSIONID=")).findFirst().orElseThrow();
    assertThat(jsessionHeader).containsIgnoringCase("HttpOnly");
    assertThat(jsessionHeader).containsIgnoringCase("Secure");
    assertThat(jsessionHeader).containsIgnoringCase("SameSite=Lax");

    Timestamp lastLoginAt =
        jdbcTemplate.queryForObject(
            "SELECT last_login_at FROM users WHERE email = ?", Timestamp.class, "login.ok@x.com");
    assertThat(lastLoginAt).isNotNull();
  }

  @Test
  void loginWithAWrongPasswordReturns401AndSetsNoSessionCookie() throws Exception {
    Csrf csrf = primeCsrf();
    HttpEntity<String> registerRequest =
        new HttpEntity<>(
            "{\"email\":\"login.bad@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headersFor(csrf));
    restTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

    // A 401 response to a POST with a body triggers a documented JDK HttpURLConnection retry bug
    // ("cannot retry due to server authentication, in streaming mode") that Spring 6.1's
    // SimpleClientHttpRequestFactory can no longer work around (its streaming toggle became a
    // no-op) — java.net.http.HttpClient has no such issue, so this one assertion uses it directly
    // against the same real embedded server instead of TestRestTemplate.
    java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
    java.net.http.HttpRequest loginRequest =
        java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(restTemplate.getRootUri() + "/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .header(HttpHeaders.COOKIE, csrf.cookiePair())
            .header("X-XSRF-TOKEN", csrf.token())
            .POST(
                java.net.http.HttpRequest.BodyPublishers.ofString(
                    "{\"email\":\"login.bad@x.com\",\"password\":\"WrongPassword123!\"}"))
            .build();
    java.net.http.HttpResponse<String> response =
        httpClient.send(loginRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    List<String> setCookies = response.headers().allValues(HttpHeaders.SET_COOKIE);
    boolean hasSessionCookie =
        setCookies != null && setCookies.stream().anyMatch(c -> c.startsWith("JSESSIONID="));
    assertThat(hasSessionCookie).isFalse();
  }

  @Test
  void unauthenticatedRequestToAProtectedEndpointReturns401() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/auth/me", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).contains("UNAUTHORIZED");
  }

  @Test
  void authenticatedMeReturnsTheCallerWithoutAPasswordHash() {
    Csrf csrf = primeCsrf();
    HttpEntity<String> registerRequest =
        new HttpEntity<>(
            "{\"email\":\"me.ok@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headersFor(csrf));
    restTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);
    HttpEntity<String> loginRequest =
        new HttpEntity<>(
            "{\"email\":\"me.ok@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headersFor(csrf));
    ResponseEntity<String> loginResponse =
        restTemplate.postForEntity("/api/v1/auth/login", loginRequest, String.class);
    String sessionCookie = sessionCookiePair(loginResponse);

    HttpHeaders meHeaders = new HttpHeaders();
    meHeaders.set(HttpHeaders.COOKIE, sessionCookie);
    ResponseEntity<String> meResponse =
        restTemplate.exchange(
            "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(meHeaders), String.class);

    assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(meResponse.getBody()).contains("me.ok@x.com");
    assertThat(meResponse.getBody()).doesNotContain("password");
  }

  @Test
  void logoutInvalidatesTheServerSessionSoTheOldCookieIsLaterUnauthenticated() {
    Csrf csrf = primeCsrf();
    HttpEntity<String> registerRequest =
        new HttpEntity<>(
            "{\"email\":\"logout.ok@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headersFor(csrf));
    restTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);
    HttpEntity<String> loginRequest =
        new HttpEntity<>(
            "{\"email\":\"logout.ok@x.com\",\"password\":\"Str0ngP@ssw0rd!\"}", headersFor(csrf));
    ResponseEntity<String> loginResponse =
        restTemplate.postForEntity("/api/v1/auth/login", loginRequest, String.class);
    String sessionCookie = sessionCookiePair(loginResponse);

    HttpEntity<Void> logoutRequest = new HttpEntity<>(headersFor(csrf, sessionCookie));
    ResponseEntity<Void> logoutResponse =
        restTemplate.exchange("/api/v1/auth/logout", HttpMethod.POST, logoutRequest, Void.class);
    assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    HttpHeaders meHeaders = new HttpHeaders();
    meHeaders.set(HttpHeaders.COOKIE, sessionCookie);
    ResponseEntity<String> meResponse =
        restTemplate.exchange(
            "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(meHeaders), String.class);

    assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
