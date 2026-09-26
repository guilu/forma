package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.TrainingSetLogRepository;
import dev.diegobarrioh.forma.domain.LoggedSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for {@link JdbcTrainingSetLogRepository} (training-progression-and-logging slice
 * B, design D6/D7, migration V67). Runs against the in-memory PostgreSQL-mode H2 with Flyway
 * migrations applied (ADR-007), like {@code JdbcTrainingSessionStatusRepositoryTest}.
 *
 * <p>Its own H2 database (AGENTS.md: no test wipes a shared table) rather than the shared {@code
 * jdbc:h2:mem:forma} one, since this class's own assertions are precisely about what a fresh table
 * looks like from two different accounts/weeks.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties =
        "spring.datasource.url=jdbc:h2:mem:training_set_log_repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class JdbcTrainingSetLogRepositoryTest {

  private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final LocalDate THIS_WEEK = LocalDate.of(2026, 8, 17);
  private static final LocalDate LAST_WEEK = LocalDate.of(2026, 8, 10);

  @Autowired private TrainingSetLogRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    // This class owns its H2 database (see the class-level @TestPropertySource): clearing its own
    // table between tests is not the shared-table wipe AGENTS.md forbids — nothing else reads it.
    jdbcTemplate.update("DELETE FROM training_set_log");
    seedAccountOnce(USER_A, "training-set-log-a@test.local");
    seedAccountOnce(USER_B, "training-set-log-b@test.local");
  }

  private void seedAccountOnce(UUID userId, String email) {
    Integer existing =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId);
    if (existing == null || existing == 0) {
      jdbcTemplate.update(
          "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)", userId, email, "!");
    }
  }

  @Test
  void insertsThenReadsBackALoggedSet() {
    Instant loggedAt = Instant.parse("2026-08-18T09:00:00Z").truncatedTo(ChronoUnit.MILLIS);
    repository.upsertSet(
        USER_A, THIS_WEEK, "STRENGTH:PUSH", new LoggedSet("push-up", 2, 60.0, 8, true), loggedAt);

    List<LoggedSet> stored =
        repository.findByUserWeekAndSession(USER_A, THIS_WEEK, "STRENGTH:PUSH");

    assertThat(stored).hasSize(1);
    LoggedSet set = stored.get(0);
    assertThat(set.exerciseId()).isEqualTo("push-up");
    assertThat(set.setNumber()).isEqualTo(2);
    assertThat(set.weightKg()).isEqualTo(60.0);
    assertThat(set.reps()).isEqualTo(8);
    assertThat(set.done()).isTrue();
  }

  @Test
  void upsertUpdatesAnExistingRowInPlaceRatherThanDuplicatingIt() {
    repository.upsertSet(
        USER_A,
        THIS_WEEK,
        "STRENGTH:PUSH",
        new LoggedSet("push-up", 2, 60.0, 8, true),
        Instant.now());
    repository.upsertSet(
        USER_A,
        THIS_WEEK,
        "STRENGTH:PUSH",
        new LoggedSet("push-up", 2, 62.5, 6, true),
        Instant.now());

    List<LoggedSet> stored =
        repository.findByUserWeekAndSession(USER_A, THIS_WEEK, "STRENGTH:PUSH");

    assertThat(stored).hasSize(1);
    assertThat(stored.get(0).weightKg()).isEqualTo(62.5);
    assertThat(stored.get(0).reps()).isEqualTo(6);
  }

  @Test
  void acceptsAPartialWriteWithOnlyWeight() {
    repository.upsertSet(
        USER_A,
        THIS_WEEK,
        "STRENGTH:PUSH",
        new LoggedSet("push-up", 1, 60.0, null, false),
        Instant.now());

    List<LoggedSet> stored =
        repository.findByUserWeekAndSession(USER_A, THIS_WEEK, "STRENGTH:PUSH");

    assertThat(stored.get(0).weightKg()).isEqualTo(60.0);
    assertThat(stored.get(0).reps()).isNull();
  }

  @Test
  void isolatesRowsByWeek() {
    repository.upsertSet(
        USER_A,
        LAST_WEEK,
        "STRENGTH:PUSH",
        new LoggedSet("push-up", 1, 60.0, 8, true),
        Instant.now());

    assertThat(repository.findByUserWeekAndSession(USER_A, LAST_WEEK, "STRENGTH:PUSH")).hasSize(1);
    // The whole point of scoping by week_start: last week's log is invisible this week.
    assertThat(repository.findByUserWeekAndSession(USER_A, THIS_WEEK, "STRENGTH:PUSH")).isEmpty();
  }

  @Test
  void isolatesRowsByAccount() {
    repository.upsertSet(
        USER_A,
        THIS_WEEK,
        "STRENGTH:PUSH",
        new LoggedSet("push-up", 1, 60.0, 8, true),
        Instant.now());
    repository.upsertSet(
        USER_B,
        THIS_WEEK,
        "STRENGTH:PUSH",
        new LoggedSet("push-up", 1, 80.0, 5, true),
        Instant.now());

    assertThat(
            repository
                .findByUserWeekAndSession(USER_A, THIS_WEEK, "STRENGTH:PUSH")
                .get(0)
                .weightKg())
        .isEqualTo(60.0);
    assertThat(
            repository
                .findByUserWeekAndSession(USER_B, THIS_WEEK, "STRENGTH:PUSH")
                .get(0)
                .weightKg())
        .isEqualTo(80.0);
  }
}
