package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.FoodServing;
import dev.diegobarrioh.forma.application.FoodServingRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code food_serving} (V49). Plain JDBC via {@link JdbcTemplate} (no ORM, like
 * FOR-16).
 */
@Repository
public class JdbcFoodServingRepository implements FoodServingRepository {

  private static final String COLUMNS = "id, food_id, name, grams, default_marker, sort_order";

  private static final RowMapper<FoodServing> ROW_MAPPER =
      (rs, rowNum) ->
          new FoodServing(
              rs.getString("id"),
              rs.getString("food_id"),
              rs.getString("name"),
              rs.getBigDecimal("grams"),
              // The marker IS the fact; there is no is_default column beside it to disagree with.
              rs.getString("default_marker") != null,
              rs.getInt("sort_order"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcFoodServingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<FoodServing> findByFood(String foodId) {
    // Default first: it is the one a screen shows without being asked, and the one "one serving"
    // means. NULLS LAST would put it last on PostgreSQL and first on H2, so the marker is compared
    // rather than ordered by.
    return jdbcTemplate.query(
        "SELECT "
            + COLUMNS
            + " FROM food_serving WHERE food_id = ?"
            + " ORDER BY CASE WHEN default_marker IS NULL THEN 1 ELSE 0 END, sort_order, name",
        ROW_MAPPER,
        foodId);
  }

  @Override
  public Optional<FoodServing> findDefault(String foodId) {
    return jdbcTemplate
        .query(
            "SELECT " + COLUMNS + " FROM food_serving WHERE food_id = ? AND default_marker = 'Y'",
            ROW_MAPPER,
            foodId)
        .stream()
        .findFirst();
  }

  @Override
  public void save(FoodServing serving) {
    // Update-then-insert rather than a MERGE: H2 and PostgreSQL spell upserts differently.
    int updated =
        jdbcTemplate.update(
            "UPDATE food_serving SET food_id = ?, name = ?, grams = ?, default_marker = ?,"
                + " sort_order = ? WHERE id = ?",
            serving.foodId(),
            serving.name(),
            serving.grams(),
            marker(serving),
            serving.sortOrder(),
            serving.id());
    if (updated == 0) {
      jdbcTemplate.update(
          "INSERT INTO food_serving (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)",
          serving.id(),
          serving.foodId(),
          serving.name(),
          serving.grams(),
          marker(serving),
          serving.sortOrder());
    }
  }

  @Override
  public void deleteByFood(String foodId) {
    jdbcTemplate.update("DELETE FROM food_serving WHERE food_id = ?", foodId);
  }

  @Override
  public boolean deleteDefault(String foodId) {
    return jdbcTemplate.update(
            "DELETE FROM food_serving WHERE food_id = ? AND default_marker = 'Y'", foodId)
        > 0;
  }

  /** The sentinel: 'Y' for the default, absent for everything else (ADR-011). */
  private static String marker(FoodServing serving) {
    return serving.isDefault() ? "Y" : null;
  }
}
