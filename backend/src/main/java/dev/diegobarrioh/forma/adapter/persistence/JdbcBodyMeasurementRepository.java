package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.BodyMeasurementRepository;
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
 */
@Repository
public class JdbcBodyMeasurementRepository implements BodyMeasurementRepository {

  private static final String INSERT_SQL =
      """
      INSERT INTO body_measurements
        (id, measured_at, source, weight_kg, body_fat_percentage, bmi, muscle_mass_kg,
         water_percentage, notes)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String LIST_SQL =
      """
      SELECT measured_at, source, weight_kg, body_fat_percentage, bmi, muscle_mass_kg,
        water_percentage, notes
      FROM body_measurements
      ORDER BY measured_at DESC
      """;

  private static final RowMapper<BodyMeasurement> ROW_MAPPER =
      (rs, rowNum) ->
          new BodyMeasurement(
              rs.getObject("measured_at", OffsetDateTime.class).toInstant(),
              MeasurementSource.valueOf(rs.getString("source")),
              rs.getBigDecimal("weight_kg").doubleValue(),
              toNullableDouble(rs.getBigDecimal("body_fat_percentage")),
              toNullableDouble(rs.getBigDecimal("bmi")),
              toNullableDouble(rs.getBigDecimal("muscle_mass_kg")),
              toNullableDouble(rs.getBigDecimal("water_percentage")),
              rs.getString("notes"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcBodyMeasurementRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void save(BodyMeasurement measurement) {
    jdbcTemplate.update(
        INSERT_SQL,
        UUID.randomUUID(),
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
  public List<BodyMeasurement> list() {
    return jdbcTemplate.query(LIST_SQL, ROW_MAPPER);
  }

  private static Double toNullableDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  private static BigDecimal toNullableBigDecimal(Double value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }
}
