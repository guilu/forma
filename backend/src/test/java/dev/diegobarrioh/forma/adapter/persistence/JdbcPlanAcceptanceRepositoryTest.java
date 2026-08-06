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

  @Autowired private PlanAcceptanceRepository acceptances;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void clearAcceptances() {
    jdbcTemplate.update("DELETE FROM plan_acceptance");
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
}
