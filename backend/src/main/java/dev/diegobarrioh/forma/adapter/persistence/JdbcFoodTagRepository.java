package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.FoodTagRepository;
import dev.diegobarrioh.forma.application.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code tag} and {@code food_tag} (V50). Plain JDBC via {@link JdbcTemplate} (no
 * ORM, like FOR-16).
 */
@Repository
public class JdbcFoodTagRepository implements FoodTagRepository {

  private static final String COLUMNS = "id, name, sort_order, enabled";

  private static final RowMapper<Tag> ROW_MAPPER =
      (rs, rowNum) ->
          new Tag(
              rs.getString("id"),
              rs.getString("name"),
              rs.getInt("sort_order"),
              rs.getBoolean("enabled"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcFoodTagRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<Tag> findAll() {
    return jdbcTemplate.query(
        "SELECT " + COLUMNS + " FROM tag WHERE enabled = TRUE ORDER BY sort_order, name",
        ROW_MAPPER);
  }

  @Override
  public Optional<Tag> find(String id) {
    return jdbcTemplate
        .query("SELECT " + COLUMNS + " FROM tag WHERE id = ?", ROW_MAPPER, id)
        .stream()
        .findFirst();
  }

  @Override
  public List<Tag> findByFood(String foodId) {
    // Ordered by the vocabulary rather than by the join, so a food's labels read the same way the
    // checkbox list that set them did.
    return jdbcTemplate.query(
        "SELECT t.id, t.name, t.sort_order, t.enabled FROM tag t"
            + " JOIN food_tag ft ON ft.tag_id = t.id"
            + " WHERE ft.food_id = ? ORDER BY t.sort_order, t.name",
        ROW_MAPPER,
        foodId);
  }

  @Override
  public void replaceTagsOf(String foodId, List<String> tagIds) {
    // Delete-then-insert rather than a diff: the caller states the complete answer, and working out
    // which rows changed would cost two queries to save nothing on a set this small.
    jdbcTemplate.update("DELETE FROM food_tag WHERE food_id = ?", foodId);
    for (String tagId : tagIds) {
      jdbcTemplate.update("INSERT INTO food_tag (food_id, tag_id) VALUES (?, ?)", foodId, tagId);
    }
  }
}
