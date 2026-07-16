package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExchangedTokens} (FOR-131): a value type that carries live provider tokens
 * across the application/adapter boundary and therefore must never leak them through an unguarded
 * {@code toString()} (ADR-008: "Do not log passwords, access tokens, refresh tokens or provider
 * secrets" — a bare {@code log.info("tokens={}", tokens)} or an exception message that interpolates
 * the object is a realistic way this could happen by accident).
 */
class ExchangedTokensTest {

  @Test
  void toStringNeverContainsTheRawAccessOrRefreshTokenValues() {
    ExchangedTokens tokens =
        new ExchangedTokens(
            "super-secret-access-token",
            "super-secret-refresh-token",
            Instant.parse("2026-07-16T10:00:00Z"));

    String rendered = tokens.toString();

    assertThat(rendered).doesNotContain("super-secret-access-token");
    assertThat(rendered).doesNotContain("super-secret-refresh-token");
  }

  @Test
  void rejectsBlankAccessToken() {
    assertThatThrownBy(() -> new ExchangedTokens(" ", "refresh", Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankRefreshToken() {
    assertThatThrownBy(() -> new ExchangedTokens("access", " ", Instant.now()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullExpiry() {
    assertThatThrownBy(() -> new ExchangedTokens("access", "refresh", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
