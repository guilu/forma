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

/**
 * JDBC adapter that persists {@link WaterIntakeEntry} rows to the {@code water_intake_entry} table
 * (FOR-130, migration V14).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003), following {@link
 * JdbcMealLogRepository}'s per-owner list-of-rows shape. Append-only for this slice (spec FOR-130
 * Open Questions): only insert and owner+date lookup, no update/delete. Rows are ordered by {@code
 * logged_at} so entries always come back in the order they were logged, never re-ordered or
 * overwritten (spec FOR-130 edge case: "Multiple entries same day → summed").
 */
@Repository
public class JdbcWaterIntakeRepository implements WaterIntakeRepository {

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
  public List<StoredWaterIntakeEntry> findByOwnerAndDate(String ownerId, LocalDate date) {
    return jdbcTemplate.query(
        "SELECT id, log_date, volume_ml FROM water_intake_entry"
            + " WHERE owner_id = ? AND log_date = ? ORDER BY logged_at, id",
        ROW_MAPPER,
        ownerId,
        Date.valueOf(date));
  }

  @Override
  public StoredWaterIntakeEntry save(String ownerId, WaterIntakeEntry entry) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO water_intake_entry (id, owner_id, log_date, volume_ml) VALUES (?, ?, ?, ?)",
        id,
        ownerId,
        Date.valueOf(entry.date()),
        entry.volumeMl());
    return new StoredWaterIntakeEntry(id.toString(), entry);
  }
}
