package dev.diegobarrioh.forma.adapter.withings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diegobarrioh.forma.application.ExchangedTokens;
import dev.diegobarrioh.forma.application.ProviderOAuthException;
import dev.diegobarrioh.forma.application.ProviderOAuthGateway;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The only {@link ProviderOAuthGateway} this story registers (FOR-131) — Withings-specific OAuth
 * logic (authorization URL, token exchange, refresh) lives here, behind the provider-neutral port
 * (ADR-004). No Withings request/response shape crosses {@link #exchangeAuthorizationCode} or
 * {@link #refreshTokens}'s return type — both map into {@link ExchangedTokens}.
 *
 * <p>Configuration (client id/secret, redirect URI, scope, endpoint URLs) is entirely env/config
 * driven via {@code forma.integrations.withings.*} (see {@code application.yml}), defaulting to
 * empty placeholders for the secrets — this class does not fail to construct when they are unset
 * (mirroring {@code AesGcmTokenCipher}'s lazy-fail design) so the application can still boot in an
 * environment that never exercises Withings OAuth; a real connect/callback attempt without real
 * credentials fails at the Withings call itself, surfaced as a {@link ProviderOAuthException}.
 *
 * <p>Revocation: Withings does not expose a documented, reliable programmatic token-revocation
 * endpoint (resolved as part of spec FOR-131's open question "whether revoke can be tested without
 * live credentials" — it can't, because there is no such endpoint to call). Disconnecting therefore
 * "revokes" by forgetting the encrypted tokens locally ({@code IntegrationTokenStore#forget},
 * called from {@code IntegrationService#disconnect}) rather than by this adapter calling Withings —
 * a documented, conservative resolution rather than inventing an unverified API contract.
 */
@Component
public class WithingsOAuthAdapter implements ProviderOAuthGateway {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String clientId;
  private final String clientSecret;
  private final String redirectUri;
  private final String scope;
  private final String authorizeUrl;
  private final String tokenUrl;
  private final WithingsHttpTransport transport;

  public WithingsOAuthAdapter(
      @Value("${forma.integrations.withings.client-id:}") String clientId,
      @Value("${forma.integrations.withings.client-secret:}") String clientSecret,
      @Value("${forma.integrations.withings.redirect-uri:https://forma.diegobarrioh.dev/auth}")
          String redirectUri,
      @Value("${forma.integrations.withings.scope:user.metrics}") String scope,
      @Value(
              "${forma.integrations.withings.authorize-url:https://account.withings.com/oauth2_user/authorize2}")
          String authorizeUrl,
      @Value("${forma.integrations.withings.token-url:https://wbsapi.withings.net/v2/oauth2}")
          String tokenUrl,
      WithingsHttpTransport transport) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.redirectUri = redirectUri;
    this.scope = scope;
    this.authorizeUrl = authorizeUrl;
    this.tokenUrl = tokenUrl;
    this.transport = transport;
  }

  @Override
  public IntegrationProvider provider() {
    return IntegrationProvider.WITHINGS;
  }

  @Override
  public String buildAuthorizationUrl(String state, String codeChallenge) {
    return authorizeUrl
        + "?response_type=code"
        + "&client_id="
        + encode(clientId)
        + "&redirect_uri="
        + encode(redirectUri)
        + "&scope="
        + encode(scope)
        + "&state="
        + encode(state)
        + "&code_challenge="
        + encode(codeChallenge)
        + "&code_challenge_method=S256";
  }

  @Override
  public ExchangedTokens exchangeAuthorizationCode(String code, String codeVerifier) {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("action", "requesttoken");
    form.put("grant_type", "authorization_code");
    form.put("client_id", clientId);
    form.put("client_secret", clientSecret);
    form.put("code", code);
    form.put("code_verifier", codeVerifier);
    form.put("redirect_uri", redirectUri);
    return callTokenEndpoint(form);
  }

  @Override
  public ExchangedTokens refreshTokens(String refreshToken) {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("action", "requesttoken");
    form.put("grant_type", "refresh_token");
    form.put("client_id", clientId);
    form.put("client_secret", clientSecret);
    form.put("refresh_token", refreshToken);
    return callTokenEndpoint(form);
  }

  private ExchangedTokens callTokenEndpoint(Map<String, String> form) {
    String responseBody;
    try {
      responseBody = transport.post(tokenUrl, form);
    } catch (RuntimeException ex) {
      throw new ProviderOAuthException(
          "No se pudo contactar a Withings para obtener el token.", ex);
    }
    return parseTokenResponse(responseBody, Instant.now());
  }

  /**
   * Package-private and static so the parsing logic (spec FOR-131 tests.md: "Token exchange: given
   * a recorded Withings token response, parse access/refresh/expiry") is directly unit-testable
   * with a fixed {@code now}, independent of the transport.
   */
  static ExchangedTokens parseTokenResponse(String responseBody, Instant now) {
    JsonNode root;
    try {
      root = MAPPER.readTree(responseBody);
    } catch (JsonProcessingException ex) {
      throw new ProviderOAuthException("No se pudo interpretar la respuesta de Withings.");
    }

    int status = root.path("status").asInt(-1);
    if (status != 0) {
      throw new ProviderOAuthException(
          "Withings devolvió un error al procesar la solicitud (status=" + status + ").");
    }

    JsonNode body = root.path("body");
    String accessToken = body.path("access_token").asText(null);
    String refreshToken = body.path("refresh_token").asText(null);
    long expiresInSeconds = body.path("expires_in").asLong(-1);
    if (accessToken == null || refreshToken == null || expiresInSeconds < 0) {
      throw new ProviderOAuthException("Withings devolvió una respuesta de token incompleta.");
    }

    return new ExchangedTokens(accessToken, refreshToken, now.plusSeconds(expiresInSeconds));
  }

  private static String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
