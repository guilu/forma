package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.AchievementRepository;
import dev.diegobarrioh.forma.application.EarnedAchievement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcAchievementRepository} (FOR-135). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007, V18), like the FOR-130 {@code
 * JdbcWaterIntakeRepositoryTest}. Covers the round-trip, PK duplicate-prevention (idempotency) and
 * empty-database fixtures from tests.md.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcAchievementRepositoryTest {

  private static final String OWNER = "default-user";
  private static final String OTHER_OWNER = "someone-else";

  @Autowired private AchievementRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM earned_achievement");
  }

  @Test
  void findAllByOwnerIsEmptyOnACleanDatabase() {
    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }

  @Test
  void awardedAchievementRoundTripsThroughFindAllByOwner() {
    Instant earnedAt = Instant.parse("2026-07-10T08:00:00Z");

    boolean newlyAwarded = repository.awardIfNotEarned(OWNER, "FIRST_MEASUREMENT", earnedAt);
    List<EarnedAchievement> found = repository.findAllByOwner(OWNER);

    assertThat(newlyAwarded).isTrue();
    assertThat(found).hasSize(1);
    assertThat(found.get(0).achievementId()).isEqualTo("FIRST_MEASUREMENT");
    assertThat(found.get(0).earnedAt()).isEqualTo(earnedAt);
  }

  @Test
  void primaryKeyPreventsADuplicateAwardAndKeepsTheOriginalEarnedAt() {
    Instant firstEarnedAt = Instant.parse("2026-07-10T08:00:00Z");
    Instant secondAttemptAt = firstEarnedAt.plus(1, ChronoUnit.DAYS);

    boolean first = repository.awardIfNotEarned(OWNER, "FIRST_MEASUREMENT", firstEarnedAt);
    boolean second = repository.awardIfNotEarned(OWNER, "FIRST_MEASUREMENT", secondAttemptAt);

    assertThat(first).isTrue();
    assertThat(second).isFalse();
    List<EarnedAchievement> found = repository.findAllByOwner(OWNER);
    assertThat(found).hasSize(1);
    assertThat(found.get(0).earnedAt()).isEqualTo(firstEarnedAt);
  }

  @Test
  void aDirectDuplicateInsertAttemptNeverCorruptsTheTable() {
    // Simulates the concurrent-evaluation edge case (spec FOR-135) at the raw SQL level: the
    // (owner_id,
    // achievement_id) primary key itself — not just application-level "check then insert" — is what
    // prevents a duplicate row.
    Instant earnedAt = Instant.parse("2026-07-10T08:00:00Z");
    repository.awardIfNotEarned(OWNER, "FIRST_MEASUREMENT", earnedAt);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO earned_achievement (owner_id, achievement_id, earned_at) VALUES (?, ?, ?)",
                    OWNER,
                    "FIRST_MEASUREMENT",
                    java.sql.Timestamp.from(earnedAt)))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(repository.findAllByOwner(OWNER)).hasSize(1);
  }

  @Test
  void findAllByOwnerNeverReturnsAnotherOwnersEarnedAchievements() {
    repository.awardIfNotEarned(
        OTHER_OWNER, "FIRST_MEASUREMENT", Instant.parse("2026-07-10T08:00:00Z"));

    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }

  @Test
  void multipleDifferentAchievementsForTheSameOwnerAllPersist() {
    repository.awardIfNotEarned(OWNER, "FIRST_MEASUREMENT", Instant.parse("2026-07-10T08:00:00Z"));
    repository.awardIfNotEarned(OWNER, "FIRST_GOAL_CREATED", Instant.parse("2026-07-11T08:00:00Z"));

    assertThat(repository.findAllByOwner(OWNER))
        .extracting(EarnedAchievement::achievementId)
        .containsExactlyInAnyOrder("FIRST_MEASUREMENT", "FIRST_GOAL_CREATED");
  }
}
