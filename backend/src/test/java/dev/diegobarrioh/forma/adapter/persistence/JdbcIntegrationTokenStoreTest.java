package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.ExchangedTokens;
import dev.diegobarrioh.forma.application.IntegrationTokenStore;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcIntegrationTokenStore} (FOR-131, migration V15). Runs against the
 * in-memory PostgreSQL-mode H2 with Flyway migrations applied (ADR-007), like {@code
 * JdbcIntegrationRepositoryTest} (FOR-126).
 *
 * <p>The headline assertion ({@code storedBytesAreNotThePlaintextToken}) is {@code tests.md}'s
 * "Token store round-trip is encrypted at rest — the stored bytes are NOT the plaintext token" — it
 * reads the raw columns directly with {@link JdbcTemplate}, bypassing the port entirely, so a
 * regression that accidentally stored plaintext cannot hide behind the port's own decrypt-on-read.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcIntegrationTokenStoreTest {

  private static final String OWNER = "default-user";
  private static final String OTHER_OWNER = "someone-else";
  private static final Instant EXPIRES_AT = Instant.parse("2026-07-16T12:00:00Z");

  @Autowired private IntegrationTokenStore tokenStore;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM integration_token");
  }

  @Test
  void findIsEmptyWhenNeverStored() {
    assertThat(tokenStore.find(OWNER, IntegrationProvider.WITHINGS)).isEmpty();
  }

  @Test
  void storeThenFindRoundTripsTheTokens() {
    ExchangedTokens tokens =
        new ExchangedTokens("plain-access-token", "plain-refresh-token", EXPIRES_AT);

    tokenStore.store(OWNER, IntegrationProvider.WITHINGS, tokens);

    Optional<ExchangedTokens> found = tokenStore.find(OWNER, IntegrationProvider.WITHINGS);
    assertThat(found).isPresent();
    assertThat(found.get().accessToken()).isEqualTo("plain-access-token");
    assertThat(found.get().refreshToken()).isEqualTo("plain-refresh-token");
    assertThat(found.get().accessTokenExpiresAt()).isEqualTo(EXPIRES_AT);
  }

  @Test
  void storedBytesAreNotThePlaintextTokenEncryptedAtRest() {
    ExchangedTokens tokens =
        new ExchangedTokens(
            "super-secret-access-token-value", "super-secret-refresh-token-value", EXPIRES_AT);

    tokenStore.store(OWNER, IntegrationProvider.WITHINGS, tokens);

    var row =
        jdbcTemplate.queryForMap(
            "SELECT access_token_ciphertext, refresh_token_ciphertext FROM integration_token "
                + "WHERE owner_id = ? AND provider = ?",
            OWNER,
            IntegrationProvider.WITHINGS.name());
    byte[] accessCiphertext = (byte[]) row.get("access_token_ciphertext");
    byte[] refreshCiphertext = (byte[]) row.get("refresh_token_ciphertext");

    assertThat(new String(accessCiphertext)).doesNotContain("super-secret-access-token-value");
    assertThat(new String(refreshCiphertext)).doesNotContain("super-secret-refresh-token-value");
  }

  @Test
  void storeUpdatesAnExistingRowInsteadOfDuplicatingIt() {
    tokenStore.store(
        OWNER,
        IntegrationProvider.WITHINGS,
        new ExchangedTokens("access-1", "refresh-1", EXPIRES_AT));

    tokenStore.store(
        OWNER,
        IntegrationProvider.WITHINGS,
        new ExchangedTokens("access-2", "refresh-2", EXPIRES_AT.plusSeconds(60)));

    Integer rowCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM integration_token WHERE owner_id = ? AND provider = ?",
            Integer.class,
            OWNER,
            IntegrationProvider.WITHINGS.name());
    assertThat(rowCount).isEqualTo(1);
    assertThat(tokenStore.find(OWNER, IntegrationProvider.WITHINGS).orElseThrow().accessToken())
        .isEqualTo("access-2");
  }

  @Test
  void forgetDeletesTheStoredTokens() {
    tokenStore.store(
        OWNER, IntegrationProvider.WITHINGS, new ExchangedTokens("access", "refresh", EXPIRES_AT));

    tokenStore.forget(OWNER, IntegrationProvider.WITHINGS);

    assertThat(tokenStore.find(OWNER, IntegrationProvider.WITHINGS)).isEmpty();
  }

  @Test
  void forgetWhenNothingIsStoredIsANoOp() {
    tokenStore.forget(OWNER, IntegrationProvider.WITHINGS);

    assertThat(tokenStore.find(OWNER, IntegrationProvider.WITHINGS)).isEmpty();
  }

  @Test
  void findNeverReturnsAnotherOwnersTokens() {
    tokenStore.store(
        OTHER_OWNER,
        IntegrationProvider.WITHINGS,
        new ExchangedTokens("their-access", "their-refresh", EXPIRES_AT));

    assertThat(tokenStore.find(OWNER, IntegrationProvider.WITHINGS)).isEmpty();
  }
}
