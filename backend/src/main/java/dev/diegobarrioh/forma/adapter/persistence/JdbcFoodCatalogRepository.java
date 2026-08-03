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
      "id, name, serving_size_g, kcal, protein_g, carbs_g, fat_g, fiber_g, sugars_g, sodium_mg,"
          + " saturated_fat_g, food_group_id, primary_macro";

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
    return jdbcTemplate.query("SELECT " + COLUMNS + " FROM food_catalog ORDER BY id", ROW_MAPPER);
  }

  @Override
  public Optional<CatalogFood> findById(String id) {
    List<CatalogFood> rows =
        jdbcTemplate.query("SELECT " + COLUMNS + " FROM food_catalog WHERE id = ?", ROW_MAPPER, id);
    return rows.stream().findFirst();
  }

  @Override
  public void insert(CatalogFood food) {
    jdbcTemplate.update(
        "INSERT INTO food_catalog (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        food.id(),
        food.name(),
        food.servingSizeG(),
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
        "UPDATE food_catalog SET name = ?, serving_size_g = ?, kcal = ?, protein_g = ?,"
            + " carbs_g = ?, fat_g = ?, fiber_g = ?, sugars_g = ?, sodium_mg = ?,"
            + " saturated_fat_g = ?, food_group_id = ?, primary_macro = ? WHERE id = ?",
        food.name(),
        food.servingSizeG(),
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
