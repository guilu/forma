package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.CategoryDisplay;
import dev.diegobarrioh.forma.application.CategoryDisplayRepository;
import dev.diegobarrioh.forma.domain.CategoryScope;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code category_display} (FOR-197, V39). Plain JDBC via {@link JdbcTemplate}
 * (no ORM, like FOR-16). Single table, no joins.
 */
@Repository
public class JdbcCategoryDisplayRepository implements CategoryDisplayRepository {

  private static final String COLUMNS = "scope, code, label, icon";

  private static final RowMapper<CategoryDisplay> ROW_MAPPER =
      (rs, rowNum) ->
          new CategoryDisplay(
              CategoryScope.valueOf(rs.getString("scope")),
              rs.getString("code"),
              rs.getString("label"),
              rs.getString("icon"));

  private final JdbcTemplate jdbcTemplate;

  public JdbcCategoryDisplayRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<CategoryDisplay> findAll(CategoryScope scope) {
    // Ordered by label, which is what the screen is read by. The code is a token
    // and its alphabetical order means nothing to anybody.
    if (scope == null) {
      return jdbcTemplate.query(
          "SELECT " + COLUMNS + " FROM category_display ORDER BY scope, label", ROW_MAPPER);
    }
    return jdbcTemplate.query(
        "SELECT " + COLUMNS + " FROM category_display WHERE scope = ? ORDER BY label",
        ROW_MAPPER,
        scope.name());
  }

  @Override
  public Optional<CategoryDisplay> find(CategoryScope scope, String code) {
    return jdbcTemplate
        .query(
            "SELECT " + COLUMNS + " FROM category_display WHERE scope = ? AND code = ?",
            ROW_MAPPER,
            scope.name(),
            code)
        .stream()
        .findFirst();
  }

  @Override
  public void update(CategoryDisplay display) {
    jdbcTemplate.update(
        "UPDATE category_display SET label = ?, icon = ? WHERE scope = ? AND code = ?",
        display.label(),
        display.icon(),
        display.scope().name(),
        display.code());
  }
}
