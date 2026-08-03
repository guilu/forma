package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.CatalogFood;
import dev.diegobarrioh.forma.application.FoodCatalogRepository;
import dev.diegobarrioh.forma.domain.PrimaryMacro;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter reading the read-only {@code food_catalog} table (FOR-173, V25). Plain JDBC via
 * {@link JdbcTemplate} (no ORM, like FOR-16). Single-table, no joins.
 */
@Repository
public class JdbcFoodCatalogRepository implements FoodCatalogRepository {

  private static final String COLUMNS =
      "id, name, kcal, protein_g, carbs_g, fat_g, fiber_g, sugars_g, sodium_mg,"
          + " saturated_fat_g, food_group_id, primary_macro";

  /**
   * The food's own columns plus its default portion, which stopped being one of them in V49.
   *
   * <p>A correlated subquery rather than a join: a food has at most one default portion and may
   * have none, so a LEFT JOIN would say the same thing while inviting somebody to add a second
   * serving and silently double every row.
   */
  private static final String SELECT_LIST =
      COLUMNS
          + ", (SELECT s.grams FROM food_serving s WHERE s.food_id = food_catalog.id"
          + " AND s.default_marker = 'Y') AS serving_size_g";

  private static final RowMapper<CatalogFood> ROW_MAPPER =
      (rs, rowNum) ->
          new CatalogFood(
              rs.getString("id"),
              rs.getString("name"),
              rs.getBigDecimal("serving_size_g"),
              rs.getInt("kcal"),
              rs.getBigDecimal("protein_g"),
              rs.getBigDecimal("carbs_g"),
              rs.getBigDecimal("fat_g"),
              rs.getBigDecimal("fiber_g"),
              rs.getBigDecimal("sugars_g"),
              rs.getBigDecimal("sodium_mg"),
              rs.getBigDecimal("saturated_fat_g"),
              // Nullable by design: a food nobody has classified yet (V35).
              rs.getString("food_group_id"),
              rs.getString("primary_macro") == null
                  ? null
                  : PrimaryMacro.valueOf(rs.getString("primary_macro")));

  private final JdbcTemplate jdbcTemplate;

  public JdbcFoodCatalogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<CatalogFood> findAll() {
    return jdbcTemplate.query(
        "SELECT " + SELECT_LIST + " FROM food_catalog ORDER BY id", ROW_MAPPER);
  }

  @Override
  public Optional<CatalogFood> findById(String id) {
    List<CatalogFood> rows =
        jdbcTemplate.query(
            "SELECT " + SELECT_LIST + " FROM food_catalog WHERE id = ?", ROW_MAPPER, id);
    return rows.stream().findFirst();
  }

  @Override
  public void insert(CatalogFood food) {
    jdbcTemplate.update(
        "INSERT INTO food_catalog (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        food.id(),
        food.name(),
        food.kcal(),
        food.proteinG(),
        food.carbsG(),
        food.fatG(),
        food.fiberG(),
        food.sugarsG(),
        food.sodiumMg(),
        food.saturatedFatG(),
        food.foodGroupId(),
        name(food.primaryMacro()));
  }

  @Override
  public void update(CatalogFood food) {
    jdbcTemplate.update(
        "UPDATE food_catalog SET name = ?, kcal = ?, protein_g = ?,"
            + " carbs_g = ?, fat_g = ?, fiber_g = ?, sugars_g = ?, sodium_mg = ?,"
            + " saturated_fat_g = ?, food_group_id = ?, primary_macro = ?,"
            // Stamped here rather than by a trigger: H2 and PostgreSQL spell those differently,
            // and this is the only place a food is ever rewritten.
            + " updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        food.name(),
        food.kcal(),
        food.proteinG(),
        food.carbsG(),
        food.fatG(),
        food.fiberG(),
        food.sugarsG(),
        food.sodiumMg(),
        food.saturatedFatG(),
        food.foodGroupId(),
        name(food.primaryMacro()),
        food.id());
  }

  /** The stored token of an enum that may be absent — a food whose macros decide nothing. */
  private static String name(PrimaryMacro macro) {
    return macro == null ? null : macro.name();
  }

  @Override
  public boolean delete(String id) {
    return jdbcTemplate.update("DELETE FROM food_catalog WHERE id = ?", id) > 0;
  }
}
