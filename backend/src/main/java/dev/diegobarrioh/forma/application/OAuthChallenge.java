package dev.diegobarrioh.forma.application;

import java.time.Instant;

/**
 * A single-use, expiring OAuth authorization-code + PKCE challenge (FOR-131), issued by {@link
 * OAuthStateStore#create} and consumed exactly once by {@link OAuthStateStore#consume}.
 *
 * <p>{@code state} is the CSRF nonce round-tripped through the provider redirect and validated on
 * callback. {@code codeVerifier}/{@code codeChallenge} are the PKCE (RFC 7636) pair: {@code
 * codeChallenge} (derived, not secret) goes into the authorization URL; {@code codeVerifier}
 * (secret, single-use) is presented at token-exchange time so a stolen authorization code alone is
 * not enough to redeem tokens. Neither value is a provider access/refresh token, so this type is
 * not held to the same never-log bar as {@link ExchangedTokens} — but it is still never echoed back
 * to a client (spec FOR-131 api.md: "Never log or echo code, state, or any token").
 *
 * @param state the CSRF nonce
 * @param codeVerifier the PKCE code verifier (secret, single-use)
 * @param codeChallenge the PKCE code challenge (S256 of {@code codeVerifier}, sent to the provider)
 * @param expiresAt when this challenge stops being valid
 */
public record OAuthChallenge(
    String state, String codeVerifier, String codeChallenge, Instant expiresAt) {

  public OAuthChallenge {
    if (state == null || state.isBlank()) {
      throw new IllegalArgumentException("state must not be blank");
    }
    if (codeVerifier == null || codeVerifier.isBlank()) {
      throw new IllegalArgumentException("codeVerifier must not be blank");
    }
    if (codeChallenge == null || codeChallenge.isBlank()) {
      throw new IllegalArgumentException("codeChallenge must not be blank");
    }
    if (expiresAt == null) {
      throw new IllegalArgumentException("expiresAt must not be null");
    }
  }
}
