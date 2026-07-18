package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.WeeklyTrackingRecordRepository;
import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists {@link WeeklyTrackingRecord} to the {@code weekly_tracking_record}
 * table (FOR-155, migration V21).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003). {@code upsert} uses a portable
 * update-then-insert rather than a database-specific {@code ON CONFLICT}/{@code MERGE}, so it works
 * on both PostgreSQL and the H2 test database, following {@code
 * JdbcTrainingSessionStatusRepository} (FOR-27).
 *
 * <p>Derived masses ({@code fatMassKg}/{@code leanMassKg}) are not stored; {@link
 * WeeklyTrackingRecord} recomputes them on read from {@code weightKg}/{@code bodyFatPercentage}.
 */
@Repository
public class JdbcWeeklyTrackingRecordRepository implements WeeklyTrackingRecordRepository {

  private static final String SELECT_COLUMNS =
      "week, record_date, weight_kg, body_fat_percentage, bmi, running_km,"
          + " pace_4km_min_per_km, recommended_kcal, comment";

  private static final RowMapper<WeeklyTrackingRecord> ROW_MAPPER =
      (rs, rowNum) ->
          new WeeklyTrackingRecord(
              rs.getInt("week"),
              rs.getObject("record_date", LocalDate.class),
              toNullableDouble(rs.getBigDecimal("weight_kg")),
              toNullableDouble(rs.getBigDecimal("body_fat_percentage")),
              toNullableDouble(rs.getBigDecimal("bmi")),
              toNullableDouble(rs.getBigDecimal("running_km")),
              rs.getString("pace_4km_min_per_km"),
              toNullableDouble(rs.getBigDecimal("recommended_kcal")),
              rs.getString("comment"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcWeeklyTrackingRecordRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<WeeklyTrackingRecord> findAllByOwner(String ownerId) {
    return jdbcTemplate.query(
        "SELECT "
            + SELECT_COLUMNS
            + " FROM weekly_tracking_record"
            + " WHERE owner_id = ? ORDER BY week DESC",
        ROW_MAPPER,
        ownerId);
  }

  @Override
  public Optional<WeeklyTrackingRecord> findByOwnerAndWeek(String ownerId, int week) {
    try {
      WeeklyTrackingRecord record =
          jdbcTemplate.queryForObject(
              "SELECT "
                  + SELECT_COLUMNS
                  + " FROM weekly_tracking_record"
                  + " WHERE owner_id = ? AND week = ?",
              ROW_MAPPER,
              ownerId,
              week);
      return Optional.ofNullable(record);
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  @Override
  public WeeklyTrackingRecord upsert(String ownerId, WeeklyTrackingRecord record) {
    int updated =
        jdbcTemplate.update(
            """
            UPDATE weekly_tracking_record
            SET record_date = ?, weight_kg = ?, body_fat_percentage = ?, bmi = ?,
                running_km = ?, pace_4km_min_per_km = ?, recommended_kcal = ?, comment = ?
            WHERE owner_id = ? AND week = ?
            """,
            Date.valueOf(record.date()),
            toNullableBigDecimal(record.weightKg()),
            toNullableBigDecimal(record.bodyFatPercentage()),
            toNullableBigDecimal(record.bmi()),
            toNullableBigDecimal(record.runningKm()),
            record.pace4kmMinPerKm(),
            toNullableBigDecimal(record.recommendedKcal()),
            record.comment(),
            ownerId,
            record.week());
    if (updated == 0) {
      jdbcTemplate.update(
          """
          INSERT INTO weekly_tracking_record
            (id, owner_id, week, record_date, weight_kg, body_fat_percentage, bmi, running_km,
             pace_4km_min_per_km, recommended_kcal, comment)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
          UUID.randomUUID(),
          ownerId,
          record.week(),
          Date.valueOf(record.date()),
          toNullableBigDecimal(record.weightKg()),
          toNullableBigDecimal(record.bodyFatPercentage()),
          toNullableBigDecimal(record.bmi()),
          toNullableBigDecimal(record.runningKm()),
          record.pace4kmMinPerKm(),
          toNullableBigDecimal(record.recommendedKcal()),
          record.comment());
    }
    return findByOwnerAndWeek(ownerId, record.week()).orElseThrow();
  }

  private static Double toNullableDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  private static BigDecimal toNullableBigDecimal(Double value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }
}
