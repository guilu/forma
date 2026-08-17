package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.TrainingSessionStatusRepository;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcTrainingSessionStatusRepository} (FOR-27). Runs against the
 * in-memory PostgreSQL-mode H2 with Flyway migrations applied (ADR-007), like the FOR-16 test.
 *
 * <p>Migration V60: rows are keyed by {@code (user_id, week_start, session_key)} and the session
 * key no longer contains a day, so the same session recurs week after week as separate rows and can
 * carry a day override for one week at a time.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcTrainingSessionStatusRepositoryTest {

  private static final UUID OWNER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;
  private static final LocalDate THIS_WEEK = LocalDate.of(2026, 8, 17);
  private static final LocalDate LAST_WEEK = LocalDate.of(2026, 8, 10);

  @Autowired private TrainingSessionStatusRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM training_session_status");
  }

  @Test
  void insertsThenReadsBackStatus() {
    // Truncated to milliseconds: the column is a TIMESTAMP, so nanosecond precision does not
    // survive the round trip and asserting on it would make this test flaky by platform.
    Instant completedAt = Instant.parse("2026-08-22T10:15:30Z").truncatedTo(ChronoUnit.MILLIS);
    repository.upsertStatus(
        OWNER, THIS_WEEK, "RUNNING:LONG_RUN", SessionStatus.COMPLETED, completedAt, "Hecho");

    var stored = repository.findByUserAndWeek(OWNER, THIS_WEEK);
    assertThat(stored).containsKey("RUNNING:LONG_RUN");
    assertThat(stored.get("RUNNING:LONG_RUN").status()).isEqualTo(SessionStatus.COMPLETED);
    assertThat(stored.get("RUNNING:LONG_RUN").completedAt()).isEqualTo(completedAt);
    assertThat(stored.get("RUNNING:LONG_RUN").notes()).isEqualTo("Hecho");
  }

  @Test
  void upsertUpdatesAnExistingRowInPlace() {
    repository.upsertStatus(
        OWNER, THIS_WEEK, "STRENGTH:PUSH", SessionStatus.COMPLETED, Instant.now(), "v1");
    repository.upsertStatus(OWNER, THIS_WEEK, "STRENGTH:PUSH", SessionStatus.SKIPPED, null, null);

    var stored = repository.findByUserAndWeek(OWNER, THIS_WEEK);
    // Still a single row, updated in place.
    assertThat(stored).hasSize(1);
    assertThat(stored.get("STRENGTH:PUSH").status()).isEqualTo(SessionStatus.SKIPPED);
    assertThat(stored.get("STRENGTH:PUSH").completedAt()).isNull();
    assertThat(stored.get("STRENGTH:PUSH").notes()).isNull();
  }

  @Test
  void keepsEachWeeksRowsApart() {
    repository.upsertStatus(
        OWNER, LAST_WEEK, "RUNNING:EASY", SessionStatus.COMPLETED, Instant.now(), null);

    assertThat(repository.findByUserAndWeek(OWNER, LAST_WEEK)).containsKey("RUNNING:EASY");
    // The whole point of V60: last week's completion is invisible this week.
    assertThat(repository.findByUserAndWeek(OWNER, THIS_WEEK)).isEmpty();
  }

  @Test
  void storesADayOverrideWithoutAStatusHavingBeenRecorded() {
    repository.upsertScheduledDay(OWNER, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);

    var stored = repository.findByUserAndWeek(OWNER, THIS_WEEK);
    assertThat(stored.get("STRENGTH:PUSH").scheduledDay()).isEqualTo(DayOfWeek.MONDAY);
    // Moving a session says nothing about having done it.
    assertThat(stored.get("STRENGTH:PUSH").status()).isEqualTo(SessionStatus.PLANNED);
  }

  @Test
  void completingAMovedSessionKeepsItOnTheDayItWasMovedTo() {
    repository.upsertScheduledDay(OWNER, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);
    repository.upsertStatus(
        OWNER, THIS_WEEK, "STRENGTH:PUSH", SessionStatus.COMPLETED, Instant.now(), "hecho hoy");

    var stored = repository.findByUserAndWeek(OWNER, THIS_WEEK);
    assertThat(stored.get("STRENGTH:PUSH").status()).isEqualTo(SessionStatus.COMPLETED);
    assertThat(stored.get("STRENGTH:PUSH").scheduledDay()).isEqualTo(DayOfWeek.MONDAY);
  }

  @Test
  void clearingTheDayOverrideRestoresThePlannedDay() {
    repository.upsertScheduledDay(OWNER, THIS_WEEK, "STRENGTH:PUSH", DayOfWeek.MONDAY);
    repository.upsertScheduledDay(OWNER, THIS_WEEK, "STRENGTH:PUSH", null);

    assertThat(repository.findByUserAndWeek(OWNER, THIS_WEEK).get("STRENGTH:PUSH").scheduledDay())
        .isNull();
  }
}
