package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.AchievementRepository;
import dev.diegobarrioh.forma.application.EarnedAchievement;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
 *
 * <p>FOR-145b-2 (migration V28): {@code earned_achievement.user_id} FK-references {@code
 * users(id)}, so {@code OTHER_OWNER} must be a real seeded row (matching {@code
 * JdbcGoalRepositoryTest}'s pattern for Class-A tables). {@code OWNER} reuses the always-present
 * legacy placeholder account.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcAchievementRepositoryTest {

  private static final UUID OWNER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;
  private static final UUID OTHER_OWNER = UUID.randomUUID();

  @Autowired private AchievementRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedTables() {
    jdbcTemplate.update("DELETE FROM earned_achievement");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        OTHER_OWNER,
        "achievement-other-owner@test.local",
        "!");
  }

  /**
   * Leaves no live {@code earned_achievement} rows referencing {@code OTHER_OWNER} after the last
   * test in this class runs (ADR-007 shared named in-memory H2) — otherwise a later test class that
   * blanket-deletes non-placeholder {@code users} rows would hit an FK violation.
   */
  @AfterEach
  void cleanUpOtherOwner() {
    jdbcTemplate.update("DELETE FROM earned_achievement");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
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
    // (user_id,
    // achievement_id) primary key itself — not just application-level "check then insert" — is what
    // prevents a duplicate row.
    Instant earnedAt = Instant.parse("2026-07-10T08:00:00Z");
    repository.awardIfNotEarned(OWNER, "FIRST_MEASUREMENT", earnedAt);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO earned_achievement (user_id, achievement_id, earned_at) VALUES (?, ?, ?)",
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
