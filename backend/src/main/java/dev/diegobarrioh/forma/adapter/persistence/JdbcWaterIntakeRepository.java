package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.StoredWaterIntakeEntry;
import dev.diegobarrioh.forma.application.WaterIntakeRepository;
import dev.diegobarrioh.forma.domain.WaterIntakeEntry;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC adapter that persists {@link WaterIntakeEntry} rows to the {@code water_intake_entry} table
 * (FOR-130, migration V14).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003), following {@link
 * JdbcMealLogRepository}'s per-owner list-of-rows shape. Rows are ordered by {@code logged_at}; the
 * decrement operation works backwards through that order so a correction consumes the newest intake
 * first without affecting another owner or date.
 */
@Repository
public class JdbcWaterIntakeRepository implements WaterIntakeRepository {

  static final String LOCK_LATEST_ENTRIES_SQL =
      "SELECT id, log_date, volume_ml FROM water_intake_entry"
          + " WHERE user_id = ? AND log_date = ?"
          + " ORDER BY logged_at DESC, id DESC FOR UPDATE";

  private static final RowMapper<StoredWaterIntakeEntry> ROW_MAPPER =
      (rs, rowNum) ->
          new StoredWaterIntakeEntry(
              rs.getString("id"),
              new WaterIntakeEntry(
                  rs.getObject("log_date", LocalDate.class),
                  rs.getBigDecimal("volume_ml").doubleValue()));

  private final JdbcTemplate jdbcTemplate;

  public JdbcWaterIntakeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<StoredWaterIntakeEntry> findByOwnerAndDate(UUID userId, LocalDate date) {
    return jdbcTemplate.query(
        "SELECT id, log_date, volume_ml FROM water_intake_entry"
            + " WHERE user_id = ? AND log_date = ? ORDER BY logged_at, id",
        ROW_MAPPER,
        userId,
        Date.valueOf(date));
  }

  @Override
  public StoredWaterIntakeEntry save(UUID userId, WaterIntakeEntry entry) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO water_intake_entry (id, user_id, log_date, volume_ml)"
            + " VALUES (?, ?, ?, ?)",
        id,
        userId,
        Date.valueOf(entry.date()),
        entry.volumeMl());
    return new StoredWaterIntakeEntry(id.toString(), entry);
  }

  @Override
  @Transactional
  public double removeLatestVolume(UUID userId, LocalDate date, double volumeMl) {
    // The lock and mutations share this transaction. Two decrements for the same day therefore
    // serialize on the rows instead of both reading the same volume and losing one correction.
    List<StoredWaterIntakeEntry> entries =
        jdbcTemplate.query(LOCK_LATEST_ENTRIES_SQL, ROW_MAPPER, userId, Date.valueOf(date));
    double remaining = volumeMl;
    for (StoredWaterIntakeEntry stored : entries) {
      if (remaining <= 0) {
        break;
      }
      double amount = stored.entry().volumeMl();
      if (amount <= remaining) {
        jdbcTemplate.update(
            "DELETE FROM water_intake_entry WHERE id = ? AND user_id = ?",
            UUID.fromString(stored.id()),
            userId);
        remaining -= amount;
      } else {
        jdbcTemplate.update(
            "UPDATE water_intake_entry SET volume_ml = ? WHERE id = ? AND user_id = ?",
            amount - remaining,
            UUID.fromString(stored.id()),
            userId);
        remaining = 0;
      }
    }
    return volumeMl - remaining;
  }
}
