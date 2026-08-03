package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.FoodGroup;
import dev.diegobarrioh.forma.application.FoodGroupRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code food_group} (V43). Plain JDBC via {@link JdbcTemplate} (no ORM, like
 * FOR-16). Single table, no joins.
 */
@Repository
public class JdbcFoodGroupRepository implements FoodGroupRepository {

  private static final String COLUMNS = "id, name, icon, color, sort_order, enabled";

  private static final RowMapper<FoodGroup> ROW_MAPPER =
      (rs, rowNum) ->
          new FoodGroup(
              rs.getString("id"),
              rs.getString("name"),
              rs.getString("icon"),
              rs.getString("color"),
              rs.getInt("sort_order"),
              rs.getBoolean("enabled"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcFoodGroupRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<FoodGroup> findAll() {
    // Ordered by sort_order, not by name: groups have a conventional reading order
    // (carbohydrate, protein, fruit...) that alphabetising would scramble.
    return jdbcTemplate.query(
        "SELECT " + COLUMNS + " FROM food_group ORDER BY sort_order", ROW_MAPPER);
  }

  @Override
  public Optional<FoodGroup> find(String id) {
    return jdbcTemplate
        .query("SELECT " + COLUMNS + " FROM food_group WHERE id = ?", ROW_MAPPER, id)
        .stream()
        .findFirst();
  }

  @Override
  public void update(FoodGroup group) {
    jdbcTemplate.update(
        "UPDATE food_group SET name = ?, icon = ?, color = ?, sort_order = ?, enabled = ?"
            + " WHERE id = ?",
        group.name(),
        group.icon(),
        group.color(),
        group.sortOrder(),
        group.enabled(),
        group.id());
  }
}
