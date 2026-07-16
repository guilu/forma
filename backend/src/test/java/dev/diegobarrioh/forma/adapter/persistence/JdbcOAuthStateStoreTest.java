package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.OAuthChallenge;
import dev.diegobarrioh.forma.application.OAuthStateStore;
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
 * Integration test for {@link JdbcOAuthStateStore} (FOR-131, migration V15). Runs against the
 * in-memory PostgreSQL-mode H2 with Flyway migrations applied (ADR-007), like {@code
 * JdbcIntegrationRepositoryTest} (FOR-126).
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcOAuthStateStoreTest {

  private static final String OWNER = "default-user";
  private static final String OTHER_OWNER = "someone-else";
  private static final Instant NOW = Instant.parse("2026-07-16T08:00:00Z");

  @Autowired private OAuthStateStore stateStore;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM integration_oauth_state");
  }

  @Test
  void createIssuesAChallengeWithADistinctStateVerifierAndChallenge() {
    OAuthChallenge challenge = stateStore.create(OWNER, IntegrationProvider.WITHINGS, NOW);

    assertThat(challenge.state()).isNotBlank();
    assertThat(challenge.codeVerifier()).isNotBlank();
    assertThat(challenge.codeChallenge()).isNotBlank();
    assertThat(challenge.codeChallenge()).isNotEqualTo(challenge.codeVerifier());
    assertThat(challenge.expiresAt()).isAfter(NOW);
  }

  @Test
  void createIssuesADifferentStateAndVerifierEachTime() {
    OAuthChallenge first = stateStore.create(OWNER, IntegrationProvider.WITHINGS, NOW);
    OAuthChallenge second = stateStore.create(OWNER, IntegrationProvider.GOOGLE_FIT, NOW);

    assertThat(first.state()).isNotEqualTo(second.state());
    assertThat(first.codeVerifier()).isNotEqualTo(second.codeVerifier());
  }

  @Test
  void consumeWithTheCorrectStateReturnsTheChallenge() {
    OAuthChallenge challenge = stateStore.create(OWNER, IntegrationProvider.WITHINGS, NOW);

    Optional<OAuthChallenge> consumed =
        stateStore.consume(OWNER, IntegrationProvider.WITHINGS, challenge.state(), NOW);

    assertThat(consumed).contains(challenge);
  }

  @Test
  void consumeIsSingleUseARepeatedConsumeWithTheSameStateFails() {
    OAuthChallenge challenge = stateStore.create(OWNER, IntegrationProvider.WITHINGS, NOW);
    stateStore.consume(OWNER, IntegrationProvider.WITHINGS, challenge.state(), NOW);

    Optional<OAuthChallenge> replay =
        stateStore.consume(OWNER, IntegrationProvider.WITHINGS, challenge.state(), NOW);

    assertThat(replay).isEmpty();
  }

  @Test
  void consumeWithAMismatchedStateFails() {
    stateStore.create(OWNER, IntegrationProvider.WITHINGS, NOW);

    Optional<OAuthChallenge> consumed =
        stateStore.consume(OWNER, IntegrationProvider.WITHINGS, "not-the-issued-state", NOW);

    assertThat(consumed).isEmpty();
  }

  @Test
  void consumeAfterExpiryFails() {
    OAuthChallenge challenge = stateStore.create(OWNER, IntegrationProvider.WITHINGS, NOW);

    Optional<OAuthChallenge> consumed =
        stateStore.consume(
            OWNER,
            IntegrationProvider.WITHINGS,
            challenge.state(),
            challenge.expiresAt().plusSeconds(1));

    assertThat(consumed).isEmpty();
  }

  @Test
  void consumeWithNoChallengeEverCreatedFails() {
    Optional<OAuthChallenge> consumed =
        stateStore.consume(OWNER, IntegrationProvider.WITHINGS, "any-state", NOW);

    assertThat(consumed).isEmpty();
  }

  @Test
  void aFreshCreateOverwritesAnUnconsumedPreviousChallengeForTheSameOwnerAndProvider() {
    OAuthChallenge first = stateStore.create(OWNER, IntegrationProvider.WITHINGS, NOW);

    OAuthChallenge second = stateStore.create(OWNER, IntegrationProvider.WITHINGS, NOW);

    assertThat(stateStore.consume(OWNER, IntegrationProvider.WITHINGS, first.state(), NOW))
        .isEmpty();
    assertThat(stateStore.consume(OWNER, IntegrationProvider.WITHINGS, second.state(), NOW))
        .contains(second);
  }

  @Test
  void consumeNeverMatchesAnotherOwnersChallenge() {
    OAuthChallenge challenge = stateStore.create(OTHER_OWNER, IntegrationProvider.WITHINGS, NOW);

    Optional<OAuthChallenge> consumed =
        stateStore.consume(OWNER, IntegrationProvider.WITHINGS, challenge.state(), NOW);

    assertThat(consumed).isEmpty();
  }
}
