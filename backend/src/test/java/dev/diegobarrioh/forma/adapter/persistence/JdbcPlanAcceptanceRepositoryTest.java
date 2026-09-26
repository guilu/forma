package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.PlanAcceptanceRepository;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcPlanAcceptanceRepository} (migration V58), against the in-memory
 * PostgreSQL-mode H2 with Flyway applied (ADR-007).
 *
 * <p>The interesting one is the second acceptance: {@code accept()} is a POST with no idempotency
 * key in front of it, so a double click must not throw on the primary key nor quietly move the date
 * on which this account started.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcPlanAcceptanceRepositoryTest {

  private static final UUID USER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;
  private static final Instant FIRST = Instant.parse("2026-08-06T09:00:00Z");
  private static final Instant LATER = Instant.parse("2026-09-01T18:30:00Z");

  /**
   * A second, real FK-valid account (FIX3): {@code plan_acceptance.user_id} references {@code
   * users(id)} (migration V58), so proving isolation needs an account that actually exists, not
   * just a literal UUID like {@link #acceptanceIsPerAccount()} reads with (that test never inserts
   * for it).
   */
  private static final UUID OTHER_ACCOUNT = UUID.randomUUID();

  private static final String OTHER_ACCOUNT_EMAIL = "plan-acceptance-other@test.local";

  @Autowired private PlanAcceptanceRepository acceptances;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void clearAcceptances() {
    jdbcTemplate.update("DELETE FROM plan_acceptance");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_ACCOUNT);
  }

  @Test
  void anAccountThatNeverAnsweredHasNotAccepted() {
    assertThat(acceptances.accepted(USER)).isFalse();
  }

  @Test
  void recordsTheAcceptance() {
    acceptances.markAccepted(USER, FIRST);

    assertThat(acceptances.accepted(USER)).isTrue();
  }

  /** Accepting twice is not an error, and does not move the instant it first happened. */
  @Test
  void acceptingAgainKeepsTheFirstInstantAndAddsNoRow() {
    acceptances.markAccepted(USER, FIRST);
    acceptances.markAccepted(USER, LATER);

    assertThat(acceptances.accepted(USER)).isTrue();
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM plan_acceptance", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT accepted_at FROM plan_acceptance WHERE user_id = ?", Instant.class, USER))
        .isEqualTo(FIRST);
  }

  /** One account accepting says nothing about another. */
  @Test
  void acceptanceIsPerAccount() {
    acceptances.markAccepted(USER, FIRST);

    assertThat(acceptances.accepted(UUID.fromString("99999999-9999-9999-9999-999999999999")))
        .isFalse();
  }

  @Test
  void anAccountThatNeverAcceptedHasNoStartInstant() {
    assertThat(acceptances.planStartedAt(USER)).isEmpty();
  }

  @Test
  void planStartedAtIsTheAcceptedInstant() {
    acceptances.markAccepted(USER, FIRST);

    assertThat(acceptances.planStartedAt(USER)).contains(FIRST);
  }

  /** Design D5: a restarted cycle answers {@code planStartedAt}, not the original acceptance. */
  @Test
  void planStartedAtPrefersARestartedCycleOverTheOriginalAcceptance() {
    acceptances.markAccepted(USER, FIRST);

    acceptances.restartCycle(USER, LATER);

    assertThat(acceptances.planStartedAt(USER)).contains(LATER);
  }

  /** Restarting is a separate write from accepting: {@code accepted_at} MUST NOT move. */
  @Test
  void restartingTheCycleDoesNotMoveTheOriginalAcceptedInstant() {
    acceptances.markAccepted(USER, FIRST);

    acceptances.restartCycle(USER, LATER);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT accepted_at FROM plan_acceptance WHERE user_id = ?", Instant.class, USER))
        .isEqualTo(FIRST);
  }

  /**
   * FIX3 (ADR-012): restarting one account's cycle must never reanchor another's. {@code
   * RESTART_CYCLE_SQL}'s {@code WHERE user_id = ?} is the only thing standing between this and a
   * restart that silently reaches every account in the table.
   */
  @Test
  void restartCycleNeverReanchorsAnotherAccountsCycle() {
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        OTHER_ACCOUNT,
        OTHER_ACCOUNT_EMAIL,
        "!");
    acceptances.markAccepted(USER, FIRST);
    acceptances.markAccepted(OTHER_ACCOUNT, FIRST);

    acceptances.restartCycle(USER, LATER);

    assertThat(acceptances.planStartedAt(USER)).contains(LATER);
    assertThat(acceptances.planStartedAt(OTHER_ACCOUNT)).contains(FIRST);
  }
}
