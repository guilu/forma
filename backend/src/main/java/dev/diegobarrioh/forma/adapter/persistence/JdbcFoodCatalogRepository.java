package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.CatalogFood;
import dev.diegobarrioh.forma.application.FoodCatalogRepository;
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
          + " saturated_fat_g";

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
              rs.getBigDecimal("saturated_fat_g"));

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
}
