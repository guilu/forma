package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.Recipe;
import dev.diegobarrioh.forma.application.RecipeIngredient;
import dev.diegobarrioh.forma.application.RecipeRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code recipe} and {@code recipe_ingredient} (V52). Plain JDBC via {@link
 * JdbcTemplate} (no ORM, like FOR-16).
 *
 * <p>A recipe is read with its ingredients in a second query rather than a join. A join would
 * repeat the dish's own columns once per ingredient and leave the caller to fold them back, which
 * is more code than the extra round trip saves on a list this size.
 */
@Repository
public class JdbcRecipeRepository implements RecipeRepository {

  private static final String COLUMNS =
      "id, name, servings, notes, enabled, created_at, updated_at";

  private final JdbcTemplate jdbcTemplate;

  public JdbcRecipeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final RowMapper<Recipe> rowMapper =
      (rs, rowNum) ->
          new Recipe(
              rs.getString("id"),
              rs.getString("name"),
              rs.getInt("servings"),
              rs.getString("notes"),
              rs.getBoolean("enabled"),
              ingredientsOf(rs.getString("id")),
              rs.getTimestamp("created_at").toInstant(),
              rs.getTimestamp("updated_at").toInstant());

  @Override
  public List<Recipe> findAll() {
    return jdbcTemplate.query("SELECT " + COLUMNS + " FROM recipe ORDER BY name", rowMapper);
  }

  @Override
  public Optional<Recipe> find(String id) {
    return jdbcTemplate
        .query("SELECT " + COLUMNS + " FROM recipe WHERE id = ?", rowMapper, id)
        .stream()
        .findFirst();
  }

  @Override
  public void save(Recipe recipe) {
    // Update-then-insert rather than a MERGE: H2 and PostgreSQL spell upserts differently.
    int updated =
        jdbcTemplate.update(
            "UPDATE recipe SET name = ?, servings = ?, notes = ?, enabled = ?,"
                + " updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            recipe.name(),
            recipe.servings(),
            recipe.notes(),
            recipe.enabled(),
            recipe.id());
    if (updated == 0) {
      jdbcTemplate.update(
          "INSERT INTO recipe (id, name, servings, notes, enabled) VALUES (?, ?, ?, ?, ?)",
          recipe.id(),
          recipe.name(),
          recipe.servings(),
          recipe.notes(),
          recipe.enabled());
    }
    // Replaced whole rather than diffed: the caller states the complete list, and working out which
    // lines changed would cost a query to save nothing on a handful of ingredients.
    jdbcTemplate.update("DELETE FROM recipe_ingredient WHERE recipe_id = ?", recipe.id());
    for (RecipeIngredient ingredient : recipe.ingredients()) {
      jdbcTemplate.update(
          "INSERT INTO recipe_ingredient (recipe_id, food_id, grams, sort_order)"
              + " VALUES (?, ?, ?, ?)",
          recipe.id(),
          ingredient.foodId(),
          ingredient.grams(),
          ingredient.sortOrder());
    }
  }

  @Override
  public boolean delete(String id) {
    // The ingredients go first: they are part of the dish, and the foreign key would otherwise
    // refuse the delete and surface as a server error on an ordinary request.
    jdbcTemplate.update("DELETE FROM recipe_ingredient WHERE recipe_id = ?", id);
    return jdbcTemplate.update("DELETE FROM recipe WHERE id = ?", id) > 0;
  }

  private List<RecipeIngredient> ingredientsOf(String recipeId) {
    List<RecipeIngredient> found = new ArrayList<>();
    jdbcTemplate
        .query(
            "SELECT food_id, grams, sort_order FROM recipe_ingredient WHERE recipe_id = ?"
                + " ORDER BY sort_order, food_id",
            (rs, rowNum) ->
                new RecipeIngredient(
                    rs.getString("food_id"), rs.getBigDecimal("grams"), rs.getInt("sort_order")),
            recipeId)
        .forEach(found::add);
    return found;
  }
}
