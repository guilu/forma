package dev.diegobarrioh.forma.application;

import java.time.Instant;

/**
 * A provider OAuth token pair, exchanged or refreshed via a {@link ProviderOAuthGateway} (FOR-131).
 *
 * <p>This type crosses the application/adapter boundary so {@link IntegrationService} can hand the
 * access/refresh tokens to an {@link IntegrationTokenStore} for encrypted persistence — it never
 * flows further than that. It never reaches {@code IntegrationConnection} (the domain aggregate
 * stays token-free, ADR-004), never reaches a delivery-layer response DTO, and must never be logged
 * (ADR-008). {@code toString()} is deliberately overridden so an accidental {@code
 * log.info(tokens)} or exception message interpolation cannot leak the token values.
 *
 * @param accessToken the bearer access token; never logged, never returned to a client
 * @param refreshToken the refresh token used to obtain a new access token; never logged, never
 *     returned to a client
 * @param accessTokenExpiresAt when {@code accessToken} expires
 */
public record ExchangedTokens(
    String accessToken, String refreshToken, Instant accessTokenExpiresAt) {

  public ExchangedTokens {
    if (accessToken == null || accessToken.isBlank()) {
      throw new IllegalArgumentException("accessToken must not be blank");
    }
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("refreshToken must not be blank");
    }
    if (accessTokenExpiresAt == null) {
      throw new IllegalArgumentException("accessTokenExpiresAt must not be null");
    }
  }

  /** Never leak a token through logs/exception messages via an unguarded {@code toString()}. */
  @Override
  public String toString() {
    return "ExchangedTokens[REDACTED, accessTokenExpiresAt=" + accessTokenExpiresAt + "]";
  }
}
