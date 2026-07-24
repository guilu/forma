package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.GoalRepository;
import dev.diegobarrioh.forma.application.StoredGoal;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.Goal;
import dev.diegobarrioh.forma.domain.GoalMetric;
import dev.diegobarrioh.forma.domain.GoalStatus;
import dev.diegobarrioh.forma.domain.Milestone;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcGoalRepository} (FOR-125). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (ADR-007, V11), like the FOR-107/FOR-39 tests.
 * Covers the round-trip and empty-database fixtures from tests.md.
 *
 * <p>FOR-145b-1 (migration V27): {@code goal.user_id} FK-references {@code users(id)}, so {@code
 * OTHER_OWNER} must be a real seeded row (unlike the pre-145b arbitrary {@code "someone-else"}
 * string). {@code OWNER} reuses the always-present legacy placeholder account.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcGoalRepositoryTest {

  private static final UUID OWNER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;
  private static final UUID OTHER_OWNER = UUID.randomUUID();

  @Autowired private GoalRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTables() {
    jdbcTemplate.update("DELETE FROM goal_milestone");
    jdbcTemplate.update("DELETE FROM goal");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        OTHER_OWNER,
        "goal-other-owner@test.local",
        "!");
  }

  /**
   * Leaves no live {@code goal} rows referencing {@code OTHER_OWNER} after the last test in this
   * class runs — otherwise a later test class sharing the same named in-memory H2 DB (ADR-007) that
   * blanket-deletes non-placeholder {@code users} rows (e.g. {@code
   * AuthenticationFlowIntegrationTest#clearTestUsers}) would hit an FK violation.
   */
  @AfterEach
  void cleanUpOtherOwner() {
    jdbcTemplate.update("DELETE FROM goal_milestone");
    jdbcTemplate.update("DELETE FROM goal");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
  }

  @Test
  void findAllByOwnerIsEmptyOnACleanDatabase() {
    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }

  @Test
  void createPersistsAGoalWithMilestonesInOrder() {
    Goal goal =
        new Goal(
            "Bajar a 12% grasa",
            GoalMetric.BODY_FAT_PCT,
            12.0,
            LocalDate.of(2026, 12, 31),
            GoalStatus.ACTIVE,
            List.of(
                new Milestone(null, "15%", 15.0, false),
                new Milestone(null, "13.5%", 13.5, false)));

    StoredGoal created = repository.create(OWNER, goal);

    assertThat(created.id()).isNotBlank();
    assertThat(created.goal().title()).isEqualTo("Bajar a 12% grasa");
    assertThat(created.goal().milestones()).hasSize(2);
    assertThat(created.goal().milestones().get(0).title()).isEqualTo("15%");
    assertThat(created.goal().milestones().get(0).id()).isNotBlank();
    assertThat(created.goal().milestones().get(1).title()).isEqualTo("13.5%");
  }

  @Test
  void createdGoalRoundTripsThroughFindAllByOwner() {
    Goal goal =
        new Goal(
            "Bajar a 12% grasa",
            GoalMetric.BODY_FAT_PCT,
            12.0,
            LocalDate.of(2026, 12, 31),
            GoalStatus.ACTIVE,
            List.of(new Milestone(null, "15%", 15.0, false)));
    StoredGoal created = repository.create(OWNER, goal);

    List<StoredGoal> all = repository.findAllByOwner(OWNER);

    assertThat(all).hasSize(1);
    StoredGoal read = all.get(0);
    assertThat(read.id()).isEqualTo(created.id());
    assertThat(read.goal().metric()).isEqualTo(GoalMetric.BODY_FAT_PCT);
    assertThat(read.goal().target()).isEqualTo(12.0);
    assertThat(read.goal().dueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    assertThat(read.goal().status()).isEqualTo(GoalStatus.ACTIVE);
    assertThat(read.goal().milestones()).hasSize(1);
  }

  @Test
  void aGoalWithNoMilestonesAndNoDueDateRoundTrips() {
    Goal goal = new Goal("Correr 10k", GoalMetric.WEIGHT_KG, 70.0, null, null, List.of());

    StoredGoal created = repository.create(OWNER, goal);
    StoredGoal read = repository.findById(OWNER, created.id()).orElseThrow();

    assertThat(read.goal().dueDate()).isNull();
    assertThat(read.goal().milestones()).isEmpty();
  }

  @Test
  void findAllByOwnerNeverReturnsAnotherOwnersGoals() {
    repository.create(
        OTHER_OWNER, new Goal("Ajeno", GoalMetric.WEIGHT_KG, 70.0, null, null, List.of()));

    assertThat(repository.findAllByOwner(OWNER)).isEmpty();
  }

  @Test
  void findByIdReturnsEmptyForAnUnknownId() {
    assertThat(repository.findById(OWNER, java.util.UUID.randomUUID().toString())).isEmpty();
  }

  @Test
  void findByIdReturnsEmptyWhenTheGoalBelongsToAnotherOwner() {
    StoredGoal created =
        repository.create(
            OTHER_OWNER, new Goal("Ajeno", GoalMetric.WEIGHT_KG, 70.0, null, null, List.of()));

    assertThat(repository.findById(OWNER, created.id())).isEmpty();
  }

  @Test
  void updateReplacesGoalFieldsAndMilestoneCompletionState() {
    Goal goal =
        new Goal(
            "Bajar a 12% grasa",
            GoalMetric.BODY_FAT_PCT,
            12.0,
            null,
            GoalStatus.ACTIVE,
            List.of(new Milestone(null, "15%", 15.0, false)));
    StoredGoal created = repository.create(OWNER, goal);
    Milestone existingMilestone = created.goal().milestones().get(0);

    Goal merged =
        new Goal(
            "Bajar a 11% grasa",
            GoalMetric.BODY_FAT_PCT,
            11.0,
            LocalDate.of(2027, 1, 1),
            GoalStatus.ACHIEVED,
            List.of(existingMilestone.withCompleted(true)));

    Optional<StoredGoal> updated = repository.update(OWNER, created.id(), merged);

    assertThat(updated).isPresent();
    assertThat(updated.orElseThrow().goal().title()).isEqualTo("Bajar a 11% grasa");
    assertThat(updated.orElseThrow().goal().target()).isEqualTo(11.0);
    assertThat(updated.orElseThrow().goal().dueDate()).isEqualTo(LocalDate.of(2027, 1, 1));
    assertThat(updated.orElseThrow().goal().status()).isEqualTo(GoalStatus.ACHIEVED);
    assertThat(updated.orElseThrow().goal().milestones().get(0).completed()).isTrue();
  }

  @Test
  void updateOfAnUnknownIdReturnsEmpty() {
    Goal goal = new Goal("X", GoalMetric.WEIGHT_KG, 70.0, null, null, List.of());

    assertThat(repository.update(OWNER, java.util.UUID.randomUUID().toString(), goal)).isEmpty();
  }
}
