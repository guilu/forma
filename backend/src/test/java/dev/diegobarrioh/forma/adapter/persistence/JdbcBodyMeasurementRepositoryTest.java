package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.BodyMeasurementRepository;
import dev.diegobarrioh.forma.application.StoredBodyMeasurement;
import dev.diegobarrioh.forma.bootstrap.LegacyUserBootstrap;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link JdbcBodyMeasurementRepository} (FOR-16). Runs against the in-memory
 * PostgreSQL-mode H2 with Flyway migrations applied (see application-test.yml), following the
 * pattern of {@code MigrationBaselineTest} (ADR-007, "tests run against migrated schema").
 *
 * <p>FOR-145c (migration V30): {@code body_measurements.user_id} FK-references {@code users(id)},
 * so every call is scoped through the always-present legacy placeholder account.
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcBodyMeasurementRepositoryTest {

  private static final UUID OWNER = LegacyUserBootstrap.PLACEHOLDER_USER_ID;
  private static final UUID OTHER_OWNER = UUID.randomUUID();

  @Autowired private BodyMeasurementRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  // Isolate each test from rows left by others (shared in-memory DB across the context).
  @org.junit.jupiter.api.BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM body_measurements");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
    jdbcTemplate.update(
        "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
        OTHER_OWNER,
        "body-other-owner@test.local",
        "!");
  }

  /**
   * Leaves no rows referencing {@code OTHER_OWNER} behind: a later test class sharing this named
   * in-memory H2 DB (ADR-007) that blanket-deletes non-placeholder {@code users} would otherwise
   * hit an FK violation — same guard as {@code JdbcGoalRepositoryTest}.
   */
  @org.junit.jupiter.api.AfterEach
  void cleanUpOtherOwner() {
    jdbcTemplate.update("DELETE FROM body_measurements");
    jdbcTemplate.update("DELETE FROM users WHERE id = ?", OTHER_OWNER);
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
            null,
            null,
            "after run");

    repository.save(OWNER, measurement);

    List<BodyMeasurement> stored = repository.list(OWNER);
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
  void savesAndReadsBackMuscleMassAndWaterPercentage() {
    BodyMeasurement measurement =
        new BodyMeasurement(
            Instant.parse("2026-07-11T08:00:00Z"),
            MeasurementSource.MANUAL,
            73.6,
            14.7,
            22.7,
            62.8,
            58.0,
            "Báscula Withings");

    repository.save(OWNER, measurement);

    BodyMeasurement read = repository.list(OWNER).get(0);
    assertThat(read.muscleMassKg()).isEqualTo(62.8);
    assertThat(read.waterPercentage()).isEqualTo(58.0);
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
            null,
            null,
            null);
    BodyMeasurement newer =
        new BodyMeasurement(
            Instant.parse("2026-07-05T08:00:00Z"),
            MeasurementSource.MANUAL,
            79.5,
            24.0,
            null,
            null,
            null,
            null);

    // Insert oldest first to prove ordering is by measured_at, not insertion order.
    repository.save(OWNER, older);
    repository.save(OWNER, newer);

    List<BodyMeasurement> stored = repository.list(OWNER);
    assertThat(stored)
        .extracting(BodyMeasurement::measuredAt)
        .containsExactly(newer.measuredAt(), older.measuredAt());
  }

  /**
   * The row's primary key never left this adapter before FOR-187: the domain type carries no
   * identity, so a caller had no way to name one measurement among several. {@code listWithIds}
   * lends the persistence id to the delivery layer without turning the domain value object into an
   * entity.
   */
  @Test
  void listWithIdsCarriesThePersistenceIdAlongsideEachMeasurement() {
    BodyMeasurement newer = measurement("2026-07-06T08:00:00Z", 74.0);
    BodyMeasurement older = measurement("2026-07-05T08:00:00Z", 75.0);
    repository.save(OWNER, older);
    repository.save(OWNER, newer);

    List<StoredBodyMeasurement> stored = repository.listWithIds(OWNER);

    // Same order as list(): newest first.
    assertThat(stored)
        .extracting(s -> s.measurement().measuredAt())
        .containsExactly(newer.measuredAt(), older.measuredAt());
    assertThat(stored).extracting(StoredBodyMeasurement::id).doesNotContainNull();
    // Two rows, two distinct ids — the id is what makes them addressable.
    assertThat(stored).extracting(StoredBodyMeasurement::id).doesNotHaveDuplicates();
  }

  @Test
  void deleteRemovesOnlyTheAddressedMeasurement() {
    repository.save(OWNER, measurement("2026-07-05T08:00:00Z", 75.0));
    repository.save(OWNER, measurement("2026-07-06T08:00:00Z", 74.0));
    StoredBodyMeasurement target = repository.listWithIds(OWNER).get(0);

    assertThat(repository.delete(OWNER, target.id())).isTrue();

    assertThat(repository.list(OWNER))
        .extracting(BodyMeasurement::measuredAt)
        .containsExactly(Instant.parse("2026-07-05T08:00:00Z"));
  }

  @Test
  void deleteReportsNothingRemovedForAnUnknownId() {
    repository.save(OWNER, measurement("2026-07-05T08:00:00Z", 75.0));

    assertThat(repository.delete(OWNER, UUID.randomUUID())).isFalse();
    assertThat(repository.list(OWNER)).hasSize(1);
  }

  /** Owner scoping is enforced in SQL, not by the caller checking first (ADR-002). */
  @Test
  void deleteLeavesAnotherOwnersMeasurementAlone() {
    repository.save(OTHER_OWNER, measurement("2026-07-05T08:00:00Z", 75.0));
    UUID otherOwnersId = repository.listWithIds(OTHER_OWNER).get(0).id();

    assertThat(repository.delete(OWNER, otherOwnersId)).isFalse();
    assertThat(repository.list(OTHER_OWNER)).hasSize(1);
  }

  private static BodyMeasurement measurement(String measuredAt, double weightKg) {
    return new BodyMeasurement(
        Instant.parse(measuredAt),
        MeasurementSource.MANUAL,
        weightKg,
        null,
        null,
        null,
        null,
        null);
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
            null,
            null,
            null);

    repository.save(OWNER, minimal);

    BodyMeasurement read = repository.list(OWNER).get(0);
    assertThat(read.bodyFatPercentage()).isNull();
    assertThat(read.bmi()).isNull();
    assertThat(read.notes()).isNull();
    // Without body fat, derived masses are absent (FOR-15 contract) rather than zero.
    assertThat(read.fatMassKg()).isEmpty();
    assertThat(read.leanMassKg()).isEmpty();
    // New FOR-100 columns default to null for rows that never set them (backward compatible).
    assertThat(read.muscleMassKg()).isNull();
    assertThat(read.waterPercentage()).isNull();
  }
}
