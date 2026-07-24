package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.MealLogRepository;
import dev.diegobarrioh.forma.application.StoredMealLogEntry;
import dev.diegobarrioh.forma.domain.KeyNutrientTotals;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter that persists {@link MealLogEntry} rows to the {@code meal_log_entry} table
 * (FOR-127, migration V13; key-nutrient columns added by FOR-134, migration V17).
 *
 * <p>Plain JDBC via {@link JdbcTemplate} — no ORM (ADR-003), following {@link JdbcGoalRepository}'s
 * per-owner list-of-rows shape. Append-only for this slice (spec FOR-127 Open Questions): only
 * insert and owner+date lookup, no update/delete. Rows are ordered by {@code logged_at} so entries
 * always come back in the order they were logged, never re-ordered or overwritten (spec FOR-127
 * edge case: "Multiple entries same meal/day → all counted; never overwrite").
 *
 * <p><b>Key nutrients (FOR-134, V17).</b> {@link MealLogEntry#keyNutrients()} is a snapshot
 * computed once at logging time (from the FOR-30 catalog food, or the free entry's provided values)
 * and is persisted alongside the macro snapshot into the V17 {@code fiber_g}/{@code
 * sugars_g}/{@code sodium_mg}/{@code saturated_fat_g} columns, then read back by {@link
 * #ROW_MAPPER}. Each column is nullable: a nutrient a food genuinely lacks is stored and reloaded
 * as {@code null} ("unknown"), never fabricated as 0 — so the day-consumption read model's
 * documented null/partial-total rule holds against the real persisted rows. A change to the in-code
 * catalog never rewrites logged history, exactly as for the macro snapshot.
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
                  new KeyNutrientTotals(
                      nullableDouble(rs, "fiber_g"),
                      nullableDouble(rs, "sugars_g"),
                      nullableInteger(rs, "sodium_mg"),
                      nullableDouble(rs, "saturated_fat_g"))));

  private final JdbcTemplate jdbcTemplate;

  public JdbcMealLogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<StoredMealLogEntry> findByOwnerAndDate(UUID userId, LocalDate date) {
    return jdbcTemplate.query(
        "SELECT id, log_date, meal_type, food_item_id, name, kcal, protein_g, carbs_g, fat_g,"
            + " fiber_g, sugars_g, sodium_mg, saturated_fat_g"
            + " FROM meal_log_entry WHERE user_id = ? AND log_date = ? ORDER BY logged_at, id",
        ROW_MAPPER,
        userId,
        Date.valueOf(date));
  }

  @Override
  public StoredMealLogEntry save(UUID userId, MealLogEntry entry) {
    UUID id = UUID.randomUUID();
    KeyNutrientTotals keyNutrients = entry.keyNutrients();
    jdbcTemplate.update(
        "INSERT INTO meal_log_entry"
            + " (id, owner_id, user_id, log_date, meal_type, food_item_id, name, kcal, protein_g,"
            + " carbs_g, fat_g, fiber_g, sugars_g, sodium_mg, saturated_fat_g)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        (ps) -> {
          ps.setObject(1, id);
          ps.setString(2, userId.toString());
          ps.setObject(3, userId);
          ps.setDate(4, Date.valueOf(entry.date()));
          ps.setString(5, entry.mealType().name());
          ps.setString(6, entry.foodItemId());
          ps.setString(7, entry.name());
          ps.setInt(8, entry.totals().calories());
          ps.setBigDecimal(9, BigDecimal.valueOf(entry.totals().proteinG()));
          ps.setBigDecimal(10, BigDecimal.valueOf(entry.totals().carbsG()));
          ps.setBigDecimal(11, BigDecimal.valueOf(entry.totals().fatG()));
          setNullableDouble(ps, 12, keyNutrients.fiberG());
          setNullableDouble(ps, 13, keyNutrients.sugarsG());
          setNullableInteger(ps, 14, keyNutrients.sodiumMg());
          setNullableDouble(ps, 15, keyNutrients.saturatedFatG());
        });
    return new StoredMealLogEntry(id.toString(), entry);
  }

  private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
    BigDecimal value = rs.getBigDecimal(column);
    return value == null ? null : value.doubleValue();
  }

  private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  private static void setNullableDouble(java.sql.PreparedStatement ps, int index, Double value)
      throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.NUMERIC);
    } else {
      ps.setBigDecimal(index, BigDecimal.valueOf(value));
    }
  }

  private static void setNullableInteger(java.sql.PreparedStatement ps, int index, Integer value)
      throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.INTEGER);
    } else {
      ps.setInt(index, value);
    }
  }
}
