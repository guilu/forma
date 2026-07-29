package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.BodyMeasurementRepository;
import dev.diegobarrioh.forma.application.StoredBodyMeasurement;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists {@link BodyMeasurement} to the {@code body_measurements} table
 * (FOR-16).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — the project has no JPA/ORM on purpose ({@code
 * backend/build.gradle}). Persistence concerns (row mapping, SQL types, id generation) live here
 * and never leak into the framework-free domain type (ADR-001, ADR-003).
 *
 * <p>The domain type carries no identity (FOR-15), so this adapter generates the row's UUID primary
 * key at save time. Derived masses are not stored; {@link BodyMeasurement} recomputes them on read.
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V30): every read/write is scoped by the
 * real {@code user_id UUID} column added to close this "gap table"'s zero owner-scoping.
 */
@Repository
public class JdbcBodyMeasurementRepository implements BodyMeasurementRepository {

  private static final String INSERT_SQL =
      """
      INSERT INTO body_measurements
        (id, user_id, measured_at, source, weight_kg, body_fat_percentage, bmi, muscle_mass_kg,
         water_percentage, notes)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String LIST_SQL =
      """
      SELECT id, measured_at, source, weight_kg, body_fat_percentage, bmi, muscle_mass_kg,
        water_percentage, notes
      FROM body_measurements
      WHERE user_id = ?
      ORDER BY measured_at DESC
      """;

  /**
   * Scoped by owner in the statement itself: another account's id matches no row, so a cross-user
   * delete removes nothing and reports as much (ADR-002 — no existence leak).
   */
  private static final String DELETE_SQL =
      """
      DELETE FROM body_measurements
      WHERE user_id = ? AND id = ?
      """;

  private static final RowMapper<StoredBodyMeasurement> ROW_MAPPER =
      (rs, rowNum) ->
          new StoredBodyMeasurement(
              rs.getObject("id", UUID.class),
              new BodyMeasurement(
                  rs.getObject("measured_at", OffsetDateTime.class).toInstant(),
                  MeasurementSource.valueOf(rs.getString("source")),
                  rs.getBigDecimal("weight_kg").doubleValue(),
                  toNullableDouble(rs.getBigDecimal("body_fat_percentage")),
                  toNullableDouble(rs.getBigDecimal("bmi")),
                  toNullableDouble(rs.getBigDecimal("muscle_mass_kg")),
                  toNullableDouble(rs.getBigDecimal("water_percentage")),
                  rs.getString("notes")));

  private final JdbcTemplate jdbcTemplate;

  public JdbcBodyMeasurementRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(UUID userId, BodyMeasurement measurement) {
    jdbcTemplate.update(
        INSERT_SQL,
        UUID.randomUUID(),
        userId,
        OffsetDateTime.ofInstant(measurement.measuredAt(), ZoneOffset.UTC),
        measurement.source().name(),
        BigDecimal.valueOf(measurement.weightKg()),
        toNullableBigDecimal(measurement.bodyFatPercentage()),
        toNullableBigDecimal(measurement.bmi()),
        toNullableBigDecimal(measurement.muscleMassKg()),
        toNullableBigDecimal(measurement.waterPercentage()),
        measurement.notes());
  }

  @Override
  public List<BodyMeasurement> list(UUID userId) {
    return listWithIds(userId).stream().map(StoredBodyMeasurement::measurement).toList();
  }

  @Override
  public List<StoredBodyMeasurement> listWithIds(UUID userId) {
    return jdbcTemplate.query(LIST_SQL, ROW_MAPPER, userId);
  }

  @Override
  public boolean delete(UUID userId, UUID id) {
    return jdbcTemplate.update(DELETE_SQL, userId, id) > 0;
  }

  private static Double toNullableDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  private static BigDecimal toNullableBigDecimal(Double value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }
}
