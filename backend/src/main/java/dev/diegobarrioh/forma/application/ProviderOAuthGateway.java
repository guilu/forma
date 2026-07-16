package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.IntegrationProvider;

/**
 * Provider-neutral port for OAuth authorization-URL building and token exchange/refresh (FOR-131,
 * ADR-004: "External integrations will be implemented as adapters behind provider-neutral
 * application ports"). {@code adapter/withings}'s {@code WithingsOAuthAdapter} is the only
 * implementation this story registers; Google Fit / Apple Health (spec FOR-103 slice 5, explicitly
 * out of scope here) would add further implementations behind this same port, never change it.
 *
 * <p>No Withings-specific type (request/response JSON shape, Withings error codes, ...) appears in
 * this interface's signature — those stay inside the adapter (ADR-004 Rules: "Do not store provider
 * payloads as the primary domain model"). {@link ExchangedTokens} is the provider-neutral result
 * shape every implementation must map its provider's response into.
 */
public interface ProviderOAuthGateway {

  /** Which {@link IntegrationProvider} this gateway implements OAuth for. */
  IntegrationProvider provider();

  /**
   * Builds the provider's authorization URL for the browser to visit, embedding {@code state} (CSRF
   * nonce) and {@code codeChallenge} (PKCE, RFC 7636 S256) alongside this adapter's configured
   * client id, redirect URI and scope. Pure/no network call — cannot throw {@link
   * ProviderOAuthException}.
   */
  String buildAuthorizationUrl(String state, String codeChallenge);

  /**
   * Exchanges an authorization {@code code} (from the OAuth callback) for tokens, presenting {@code
   * codeVerifier} (PKCE) to prove this exchange comes from the same party that started the
   * authorization request. Never logs {@code code}, {@code codeVerifier} or the response body.
   *
   * @throws ProviderOAuthException if the provider is unreachable, rejects the exchange, or returns
   *     a response this adapter cannot parse
   */
  ExchangedTokens exchangeAuthorizationCode(String code, String codeVerifier);

  /**
   * Obtains a fresh access token using a previously-issued {@code refreshToken}. Never logs {@code
   * refreshToken} or the response body.
   *
   * @throws ProviderOAuthException if the provider is unreachable, rejects the refresh (e.g. the
   *     refresh token was itself revoked), or returns a response this adapter cannot parse
   */
  ExchangedTokens refreshTokens(String refreshToken);
}
