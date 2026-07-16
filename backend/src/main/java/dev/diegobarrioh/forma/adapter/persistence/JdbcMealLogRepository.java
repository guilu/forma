package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.MealLogRepository;
import dev.diegobarrioh.forma.application.StoredMealLogEntry;
import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists {@link MealLogEntry} rows to the {@code meal_log_entry} table
 * (FOR-127, migration V13).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003), following {@link JdbcGoalRepository}'s
 * per-owner list-of-rows shape. Append-only for this slice (spec FOR-127 Open Questions): only
 * insert and owner+date lookup, no update/delete. Rows are ordered by {@code logged_at} so entries
 * always come back in the order they were logged, never re-ordered or overwritten (spec FOR-127
 * edge case: "Multiple entries same meal/day → all counted; never overwrite").
 *
 * <p><b>Known limitation (FOR-134).</b> {@code meal_log_entry} has no key-nutrient columns, and
 * FOR-134 adds no migration (in-code reference data only, head stays V16) — so {@link
 * MealLogEntry#keyNutrients()} is NOT written by {@link #save} and always reconstructs as {@link
 * KeyNutrientTotals#empty()} in {@link #ROW_MAPPER}. This is an explicit, honest limitation (never
 * fabricated data), not a silent bug — see {@code JdbcMealLogRepositoryTest} and the FOR-134 PR's
 * "Known limitations". A follow-up story with a migration is needed to persist per-entry key
 * nutrients.
 */
@Repository
public class JdbcMealLogRepository implements MealLogRepository {

  private static final RowMapper<StoredMealLogEntry> ROW_MAPPER =
      (rs, rowNum) ->
          new StoredMealLogEntry(
              rs.getString("id"),
              new MealLogEntry(
                  rs.getObject("log_date", LocalDate.class),
                  MealType.valueOf(rs.getString("meal_type")),
                  rs.getString("name"),
                  rs.getString("food_item_id"),
                  new NutritionTotals(
                      rs.getInt("kcal"),
                      rs.getBigDecimal("protein_g").doubleValue(),
                      rs.getBigDecimal("carbs_g").doubleValue(),
                      rs.getBigDecimal("fat_g").doubleValue()),
                  // FOR-134: not persisted (see class javadoc) -> always reconstructed as unknown.
                  KeyNutrientTotals.empty()));

  private final JdbcTemplate jdbcTemplate;

  public JdbcMealLogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<StoredMealLogEntry> findByOwnerAndDate(String ownerId, LocalDate date) {
    return jdbcTemplate.query(
        "SELECT id, log_date, meal_type, food_item_id, name, kcal, protein_g, carbs_g, fat_g"
            + " FROM meal_log_entry WHERE owner_id = ? AND log_date = ? ORDER BY logged_at, id",
        ROW_MAPPER,
        ownerId,
        Date.valueOf(date));
  }

  @Override
  public StoredMealLogEntry save(String ownerId, MealLogEntry entry) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO meal_log_entry"
            + " (id, owner_id, log_date, meal_type, food_item_id, name, kcal, protein_g, carbs_g,"
            + " fat_g)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        ownerId,
        Date.valueOf(entry.date()),
        entry.mealType().name(),
        entry.foodItemId(),
        entry.name(),
        entry.totals().calories(),
        entry.totals().proteinG(),
        entry.totals().carbsG(),
        entry.totals().fatG());
    return new StoredMealLogEntry(id.toString(), entry);
  }
}
