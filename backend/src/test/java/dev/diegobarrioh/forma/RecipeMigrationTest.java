package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * V52: recipes as a named list of foods with amounts.
 *
 * <p>What matters is what is NOT here — no kcal, no macros, no totals — and that the constraints
 * refuse the states a curator must not be able to write.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate.
 */
class RecipeMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:recipe_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  private static Connection connection;

  @BeforeAll
  static void migrate() throws Exception {
    Flyway.configure()
        .dataSource(JDBC_URL, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();
    connection = DriverManager.getConnection(JDBC_URL, "sa", "");
  }

  @AfterAll
  static void closeConnection() throws Exception {
    connection.close();
  }

  @AfterEach
  void clear() throws Exception {
    execute("DELETE FROM recipe_ingredient");
    execute("DELETE FROM recipe");
  }

  /** Which dishes exist is editorial, and the one worked example states no amounts at all. */
  @Test
  void startsEmpty() throws Exception {
    assertThat(column("SELECT id FROM recipe")).isEmpty();
  }

  /**
   * The totals are the sum over the ingredients of what food_catalog holds. Storing them would
   * freeze an answer that has to move when somebody corrects a food.
   */
  @Test
  void storesNoNutritionOfItsOwn() {
    assertThatThrownBy(() -> column("SELECT kcal FROM recipe")).isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> column("SELECT protein_g FROM recipe"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void holdsADishWithItsIngredients() throws Exception {
    execute("INSERT INTO recipe (id, name) VALUES ('avena-overnight', 'Avena overnight')");
    ingredient("avena-overnight", "oats", "60.0", 1);
    ingredient("avena-overnight", "skim-milk", "200.0", 2);
    ingredient("avena-overnight", "whey-protein", "30.0", 3);

    assertThat(
            column(
                "SELECT food_id FROM recipe_ingredient WHERE recipe_id = 'avena-overnight'"
                    + " ORDER BY sort_order"))
        .containsExactly("oats", "skim-milk", "whey-protein");
  }

  /** A stew for four read as a meal for one makes every per-serving figure wrong fourfold. */
  @Test
  void remembersHowManyPortionsItMakes() throws Exception {
    execute("INSERT INTO recipe (id, name, servings) VALUES ('guiso', 'Guiso', 4)");

    assertThat(column("SELECT servings FROM recipe WHERE id = 'guiso'")).containsExactly("4");
    assertThat(column("SELECT servings FROM recipe WHERE id = 'guiso' AND servings > 0"))
        .hasSize(1);
  }

  @Test
  void refusesADishThatFeedsNobody() {
    assertThatThrownBy(
            () -> execute("INSERT INTO recipe (id, name, servings) VALUES ('x', 'X', 0)"))
        .isInstanceOf(SQLException.class);
  }

  /** Two dishes reading the same would make a list say one thing twice. */
  @Test
  void refusesTwoDishesWithTheSameName() throws Exception {
    execute("INSERT INTO recipe (id, name) VALUES ('a', 'Avena overnight')");

    assertThatThrownBy(
            () -> execute("INSERT INTO recipe (id, name) VALUES ('b', 'Avena overnight')"))
        .isInstanceOf(SQLException.class);
  }

  /** Listing oats twice is somebody having typed it twice; nobody would know to add the two. */
  @Test
  void refusesTheSameFoodTwiceInOneDish() throws Exception {
    execute("INSERT INTO recipe (id, name) VALUES ('r', 'R')");
    ingredient("r", "oats", "60.0", 1);

    assertThatThrownBy(() -> ingredient("r", "oats", "30.0", 2)).isInstanceOf(SQLException.class);
  }

  /** The same food across two dishes is two dishes using it, which is the normal case. */
  @Test
  void letsTwoDishesShareAnIngredient() throws Exception {
    execute("INSERT INTO recipe (id, name) VALUES ('a', 'A')");
    execute("INSERT INTO recipe (id, name) VALUES ('b', 'B')");

    assertThatCode(
            () -> {
              ingredient("a", "oats", "60.0", 1);
              ingredient("b", "oats", "40.0", 1);
            })
        .doesNotThrowAnyException();
  }

  @Test
  void refusesAnIngredientOfNothing() throws Exception {
    execute("INSERT INTO recipe (id, name) VALUES ('r', 'R')");

    assertThatThrownBy(() -> ingredient("r", "oats", "0", 1)).isInstanceOf(SQLException.class);
  }

  @Test
  void refusesAnIngredientThatIsNotInTheCatalog() throws Exception {
    execute("INSERT INTO recipe (id, name) VALUES ('r', 'R')");

    assertThatThrownBy(() -> ingredient("r", "unicornio", "60.0", 1))
        .isInstanceOf(SQLException.class);
  }

  /** A food used by a dish cannot vanish from under it. */
  @Test
  void refusesToDeleteAFoodSomeDishUses() throws Exception {
    execute("INSERT INTO recipe (id, name) VALUES ('r', 'R')");
    ingredient("r", "oats", "60.0", 1);

    assertThatThrownBy(() -> execute("DELETE FROM food_catalog WHERE id = 'oats'"))
        .isInstanceOf(SQLException.class);
  }

  private static void ingredient(String recipeId, String foodId, String grams, int order)
      throws SQLException {
    execute(
        "INSERT INTO recipe_ingredient (recipe_id, food_id, grams, sort_order) VALUES ('"
            + recipeId
            + "', '"
            + foodId
            + "', "
            + grams
            + ", "
            + order
            + ")");
  }

  private static List<String> column(String sql) throws SQLException {
    List<String> values = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql)) {
      while (rs.next()) {
        values.add(rs.getString(1));
      }
    }
    return values;
  }

  private static void execute(String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
