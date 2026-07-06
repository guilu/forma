package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.BodyMeasurementRepository;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcBodyMeasurementRepository} (FOR-16). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (see application-test.yml), following the
 * pattern of {@code MigrationBaselineTest} (ADR-007, "tests run against migrated schema").
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcBodyMeasurementRepositoryTest {

  @Autowired private BodyMeasurementRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  // Isolate each test from rows left by others (shared in-memory DB across the context).
  @org.junit.jupiter.api.BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM body_measurements");
  }

  @Test
  void savesAndReadsBackEquivalentValues() {
    BodyMeasurement measurement =
        new BodyMeasurement(
            Instant.parse("2026-07-05T08:00:00Z"),
            MeasurementSource.MANUAL,
            73.456,
            18.34,
            22.71,
            "after run");

    repository.save(measurement);

    List<BodyMeasurement> stored = repository.list();
    assertThat(stored).hasSize(1);
    BodyMeasurement read = stored.get(0);
    assertThat(read.measuredAt()).isEqualTo(measurement.measuredAt());
    assertThat(read.source()).isEqualTo(MeasurementSource.MANUAL);
    // NUMERIC precision preserved across save/read (no floating-point drift).
    assertThat(read.weightKg()).isEqualTo(73.456);
    assertThat(read.bodyFatPercentage()).isEqualTo(18.34);
    assertThat(read.bmi()).isEqualTo(22.71);
    assertThat(read.notes()).isEqualTo("after run");
    // Derived masses recomputed on read from the persisted inputs (FOR-15).
    assertThat(read.fatMassKg()).isPresent();
    assertThat(read.leanMassKg()).isPresent();
  }

  @Test
  void listReturnsMeasurementsMostRecentFirst() {
    BodyMeasurement older =
        new BodyMeasurement(
            Instant.parse("2026-07-01T08:00:00Z"),
            MeasurementSource.MANUAL,
            80.0,
            25.0,
            null,
            null);
    BodyMeasurement newer =
        new BodyMeasurement(
            Instant.parse("2026-07-05T08:00:00Z"),
            MeasurementSource.MANUAL,
            79.5,
            24.0,
            null,
            null);

    // Insert oldest first to prove ordering is by measured_at, not insertion order.
    repository.save(older);
    repository.save(newer);

    List<BodyMeasurement> stored = repository.list();
    assertThat(stored)
        .extracting(BodyMeasurement::measuredAt)
        .containsExactly(newer.measuredAt(), older.measuredAt());
  }

  @Test
  void roundTripsNullableFields() {
    BodyMeasurement minimal =
        new BodyMeasurement(
            Instant.parse("2026-07-05T08:00:00Z"),
            MeasurementSource.MANUAL,
            80.0,
            null,
            null,
            null);

    repository.save(minimal);

    BodyMeasurement read = repository.list().get(0);
    assertThat(read.bodyFatPercentage()).isNull();
    assertThat(read.bmi()).isNull();
    assertThat(read.notes()).isNull();
    // Without body fat, derived masses are absent (FOR-15 contract) rather than zero.
    assertThat(read.fatMassKg()).isEmpty();
    assertThat(read.leanMassKg()).isEmpty();
  }
}
