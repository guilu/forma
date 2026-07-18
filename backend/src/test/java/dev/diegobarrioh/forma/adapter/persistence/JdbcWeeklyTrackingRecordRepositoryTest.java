package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.WeeklyTrackingRecordRepository;
import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcWeeklyTrackingRecordRepository} (FOR-155). Runs against the
 * in-memory PostgreSQL-mode H2 with Flyway migrations applied (see application-test.yml), following
 * {@code JdbcBodyMeasurementRepositoryTest} / {@code JdbcGoalRepository}'s pattern (ADR-007).
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcWeeklyTrackingRecordRepositoryTest {

  private static final String OWNER_ID = "default-user";
  private static final String OTHER_OWNER_ID = "someone-else";

  @Autowired private WeeklyTrackingRecordRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM weekly_tracking_record");
  }

  @Test
  void startsEmptyWithNoSeedRows() {
    // Agreed model: SEGUIMIENTO starts empty; only the user's own weekly entries populate it.
    assertThat(repository.findAllByOwner(OWNER_ID)).isEmpty();
  }

  @Test
  void upsertInsertsAndReadsBackAllSeguimientoFields() {
    WeeklyTrackingRecord record =
        new WeeklyTrackingRecord(
            1, LocalDate.parse("2026-07-06"), 73.6, 14.7, 22.7, 13.0, "6:00", 2300.0, "nota");

    repository.upsert(OWNER_ID, record);

    Optional<WeeklyTrackingRecord> read = repository.findByOwnerAndWeek(OWNER_ID, 1);
    assertThat(read).isPresent();
    assertThat(read.get().week()).isEqualTo(1);
    assertThat(read.get().date()).isEqualTo(LocalDate.parse("2026-07-06"));
    assertThat(read.get().weightKg()).isEqualTo(73.6);
    assertThat(read.get().bodyFatPercentage()).isEqualTo(14.7);
    assertThat(read.get().bmi()).isEqualTo(22.7);
    assertThat(read.get().runningKm()).isEqualTo(13.0);
    assertThat(read.get().pace4kmMinPerKm()).isEqualTo("6:00");
    assertThat(read.get().recommendedKcal()).isEqualTo(2300.0);
    assertThat(read.get().comment()).isEqualTo("nota");
  }

  @Test
  void upsertUpdatesExistingWeekInsteadOfDuplicating() {
    WeeklyTrackingRecord original =
        new WeeklyTrackingRecord(
            1, LocalDate.parse("2026-07-06"), 73.6, 14.7, 22.7, 13.0, "6:00", 2300.0, "v1");
    WeeklyTrackingRecord updated =
        new WeeklyTrackingRecord(
            1, LocalDate.parse("2026-07-06"), 73.2, 14.5, 22.6, 15.0, "5:55", 2300.0, "v2");

    repository.upsert(OWNER_ID, original);
    repository.upsert(OWNER_ID, updated);

    List<WeeklyTrackingRecord> all = repository.findAllByOwner(OWNER_ID);
    assertThat(all).hasSize(1);
    assertThat(all.get(0).weightKg()).isEqualTo(73.2);
    assertThat(all.get(0).comment()).isEqualTo("v2");
  }

  @Test
  void roundTripsPartialRecordWithNullableFields() {
    WeeklyTrackingRecord partial =
        new WeeklyTrackingRecord(
            2, LocalDate.parse("2026-07-13"), null, null, null, null, null, null, null);

    repository.upsert(OWNER_ID, partial);

    WeeklyTrackingRecord read = repository.findByOwnerAndWeek(OWNER_ID, 2).orElseThrow();
    assertThat(read.weightKg()).isNull();
    assertThat(read.runningKm()).isNull();
    assertThat(read.pace4kmMinPerKm()).isNull();
    assertThat(read.recommendedKcal()).isNull();
    assertThat(read.comment()).isNull();
  }

  @Test
  void findAllByOwnerOrdersByWeekDescending() {
    repository.upsert(
        OWNER_ID,
        new WeeklyTrackingRecord(
            1, LocalDate.parse("2026-07-06"), null, null, null, null, null, null, null));
    repository.upsert(
        OWNER_ID,
        new WeeklyTrackingRecord(
            2, LocalDate.parse("2026-07-13"), null, null, null, null, null, null, null));

    List<WeeklyTrackingRecord> all = repository.findAllByOwner(OWNER_ID);
    assertThat(all).extracting(WeeklyTrackingRecord::week).containsExactly(2, 1);
  }

  @Test
  void findByOwnerAndWeekIsOwnerScoped() {
    repository.upsert(
        OTHER_OWNER_ID,
        new WeeklyTrackingRecord(
            1, LocalDate.parse("2026-07-06"), null, null, null, null, null, null, null));

    assertThat(repository.findByOwnerAndWeek(OWNER_ID, 1)).isEmpty();
  }

  @Test
  void findByOwnerAndWeekReturnsEmptyWhenMissing() {
    assertThat(repository.findByOwnerAndWeek(OWNER_ID, 99)).isEmpty();
  }
}
