package dev.diegobarrioh.forma.adapter.withings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.application.ExchangedTokens;
import dev.diegobarrioh.forma.application.ProviderOAuthException;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Adapter tests for {@link WithingsOAuthAdapter} (FOR-131 tests.md Adapter Tests). Never calls the
 * live Withings API — {@link FakeWithingsHttpTransport} stands in for the real HTTP transport, and
 * every token response is a recorded/representative fixture under {@code
 * src/test/resources/fixtures/withings/} (see each fixture's {@code _comment}: these are shaped
 * like Withings' publicly documented API, not captured from a live call, since this story has no
 * real credentials).
 */
class WithingsOAuthAdapterTest {

  private static final String CLIENT_ID = "test-client-id";
  private static final String CLIENT_SECRET = "test-client-secret";
  private static final String REDIRECT_URI = "https://forma.diegobarrioh.dev/auth";
  private static final String SCOPE = "user.metrics";
  private static final String AUTHORIZE_URL = "https://account.withings.com/oauth2_user/authorize2";
  private static final String TOKEN_URL = "https://wbsapi.withings.net/v2/oauth2";

  private final FakeWithingsHttpTransport transport = new FakeWithingsHttpTransport();
  private final WithingsOAuthAdapter adapter =
      new WithingsOAuthAdapter(
          CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, SCOPE, AUTHORIZE_URL, TOKEN_URL, transport);

  @Test
  void providerIsWithings() {
    assertThat(adapter.provider()).isEqualTo(IntegrationProvider.WITHINGS);
  }

  @Test
  void buildAuthorizationUrlCarriesClientIdRedirectUriScopeStateAndPkceChallenge() {
    String url = adapter.buildAuthorizationUrl("the-state-value", "the-code-challenge-value");

    Map<String, String> query = parseQuery(url);
    assertThat(url).startsWith(AUTHORIZE_URL + "?");
    assertThat(query.get("response_type")).isEqualTo("code");
    assertThat(query.get("client_id")).isEqualTo(CLIENT_ID);
    assertThat(query.get("redirect_uri")).isEqualTo(REDIRECT_URI);
    assertThat(query.get("scope")).isEqualTo(SCOPE);
    assertThat(query.get("state")).isEqualTo("the-state-value");
    assertThat(query.get("code_challenge")).isEqualTo("the-code-challenge-value");
    assertThat(query.get("code_challenge_method")).isEqualTo("S256");
  }

  @Test
  void buildAuthorizationUrlNeverContainsTheClientSecret() {
    String url = adapter.buildAuthorizationUrl("state", "challenge");

    assertThat(url).doesNotContain(CLIENT_SECRET);
  }

  @Test
  void exchangeAuthorizationCodeParsesAccessRefreshAndExpiryFromARecordedTokenResponse() {
    transport.nextResponse = fixture("token-response-success.json");

    ExchangedTokens tokens =
        adapter.exchangeAuthorizationCode("the-auth-code", "the-code-verifier");

    assertThat(tokens.accessToken()).isEqualTo("FIXTURE_ACCESS_TOKEN_abcdef0123456789");
    assertThat(tokens.refreshToken()).isEqualTo("FIXTURE_REFRESH_TOKEN_abcdef0123456789");
    assertThat(tokens.accessTokenExpiresAt()).isAfter(Instant.now());
  }

  @Test
  void exchangeAuthorizationCodePostsTheExpectedFormParametersAndNeverLogsSecretsInTheUrl() {
    transport.nextResponse = fixture("token-response-success.json");

    adapter.exchangeAuthorizationCode("the-auth-code", "the-code-verifier");

    assertThat(transport.lastUrl).isEqualTo(TOKEN_URL);
    assertThat(transport.lastForm.get("grant_type")).isEqualTo("authorization_code");
    assertThat(transport.lastForm.get("action")).isEqualTo("requesttoken");
    assertThat(transport.lastForm.get("client_id")).isEqualTo(CLIENT_ID);
    assertThat(transport.lastForm.get("client_secret")).isEqualTo(CLIENT_SECRET);
    assertThat(transport.lastForm.get("code")).isEqualTo("the-auth-code");
    assertThat(transport.lastForm.get("code_verifier")).isEqualTo("the-code-verifier");
    assertThat(transport.lastForm.get("redirect_uri")).isEqualTo(REDIRECT_URI);
    // The client secret/code travel in the POST form body, never appended to a URL that could
    // land in access logs (spec FOR-131 api.md: "Never log or echo code, state, or any token").
    assertThat(transport.lastUrl).doesNotContain(CLIENT_SECRET).doesNotContain("the-auth-code");
  }

  @Test
  void refreshTokensParsesANewAccessAndRefreshTokenFromARecordedRefreshResponse() {
    transport.nextResponse = fixture("refresh-response-success.json");

    ExchangedTokens refreshed = adapter.refreshTokens("the-old-refresh-token");

    assertThat(refreshed.accessToken()).isEqualTo("FIXTURE_REFRESHED_ACCESS_TOKEN_0123456789");
    assertThat(refreshed.refreshToken()).isEqualTo("FIXTURE_REFRESHED_REFRESH_TOKEN_0123456789");
    assertThat(refreshed.accessTokenExpiresAt()).isAfter(Instant.now());
  }

  @Test
  void refreshTokensPostsTheRefreshTokenGrantType() {
    transport.nextResponse = fixture("refresh-response-success.json");

    adapter.refreshTokens("the-old-refresh-token");

    assertThat(transport.lastForm.get("grant_type")).isEqualTo("refresh_token");
    assertThat(transport.lastForm.get("refresh_token")).isEqualTo("the-old-refresh-token");
  }

  @Test
  void exchangeAuthorizationCodeOnAWithingsErrorStatusThrowsWithNoSecretInTheMessage() {
    transport.nextResponse = fixture("token-response-error.json");

    assertThatThrownBy(
            () -> adapter.exchangeAuthorizationCode("the-auth-code", "the-code-verifier"))
        .isInstanceOf(ProviderOAuthException.class)
        .satisfies(
            ex -> {
              assertThat(ex.getMessage()).doesNotContain(CLIENT_SECRET);
              assertThat(ex.getMessage()).doesNotContain("the-auth-code");
              assertThat(ex.getMessage()).doesNotContain("the-code-verifier");
            });
  }

  @Test
  void exchangeAuthorizationCodeOnAMalformedResponseThrowsWithoutLeakingTheRawBody() {
    transport.nextResponse = fixture("token-response-malformed.json");

    assertThatThrownBy(
            () -> adapter.exchangeAuthorizationCode("the-auth-code", "the-code-verifier"))
        .isInstanceOf(ProviderOAuthException.class)
        .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("not valid json"));
  }

  @Test
  void exchangeAuthorizationCodeWhenTheTransportFailsWrapsItWithoutLeakingSecrets() {
    transport.failWith = new RuntimeException("Connection refused: wbsapi.withings.net");

    assertThatThrownBy(
            () -> adapter.exchangeAuthorizationCode("the-auth-code", "the-code-verifier"))
        .isInstanceOf(ProviderOAuthException.class)
        .satisfies(
            ex -> {
              assertThat(ex.getMessage()).doesNotContain(CLIENT_SECRET);
              assertThat(ex.getMessage()).doesNotContain("the-auth-code");
            });
  }

  @Test
  void refreshTokensOnAWithingsErrorStatusThrowsWithNoSecretInTheMessage() {
    transport.nextResponse = fixture("token-response-error.json");

    assertThatThrownBy(() -> adapter.refreshTokens("the-old-refresh-token"))
        .isInstanceOf(ProviderOAuthException.class)
        .satisfies(
            ex -> {
              assertThat(ex.getMessage()).doesNotContain(CLIENT_SECRET);
              assertThat(ex.getMessage()).doesNotContain("the-old-refresh-token");
            });
  }

  private static String fixture(String name) {
    try {
      return Files.readString(
          Path.of("src/test/resources/fixtures/withings/" + name), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static Map<String, String> parseQuery(String url) {
    String query = url.substring(url.indexOf('?') + 1);
    Map<String, String> result = new LinkedHashMap<>();
    for (String pair : query.split("&")) {
      String[] parts = pair.split("=", 2);
      result.put(
          URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
          URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
    }
    return result;
  }

  /** Fake HTTP transport — records the last call, never performs a real network request. */
  private static class FakeWithingsHttpTransport implements WithingsHttpTransport {
    String nextResponse;
    RuntimeException failWith;
    String lastUrl;
    Map<String, String> lastForm;

    @Override
    public String post(String url, Map<String, String> formParams) {
      lastUrl = url;
      lastForm = formParams;
      if (failWith != null) {
        throw failWith;
      }
      return nextResponse;
    }
  }
}
