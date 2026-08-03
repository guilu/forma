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
 * V47: which food stands in for which, and on what grounds.
 *
 * <p>The table stores a decision and nothing derived from it, so what there is to check is that the
 * constraints refuse the states a curator must not be able to write — and that the two columns the
 * arithmetic would have produced are genuinely not there.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate.
 */
class FoodEquivalenceMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:food_equivalence_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

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
    execute("DELETE FROM food_equivalence");
  }

  /** Which foods may stand in for which is editorial; nothing here is invented. */
  @Test
  void startsEmpty() throws Exception {
    assertThat(column("SELECT id FROM food_equivalence")).isEmpty();
  }

  /**
   * The arithmetic is not a column. Both would be a function of food_catalog, and the catalog is
   * allowed to change.
   */
  @Test
  void storesNoRatioAndNoTargetWeight() {
    assertThatThrownBy(() -> column("SELECT ratio FROM food_equivalence"))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> column("SELECT target_reference_g FROM food_equivalence"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void holdsAnEquivalenceBetweenTwoRealFoods() throws Exception {
    insert("rice", "potato", "CARBS", "100.0", "25.0");

    assertThat(column("SELECT basis FROM food_equivalence")).containsExactly("CARBS");
    assertThat(column("SELECT enabled FROM food_equivalence")).containsExactly("TRUE");
  }

  /** Rice for potato on carbohydrate and on calories are two different pieces of advice. */
  @Test
  void allowsTheSamePairOnDifferentGrounds() throws Exception {
    insert("rice", "potato", "CARBS", "100.0", null);

    assertThatCode(() -> insert("rice", "potato", "CALORIES", "100.0", null))
        .doesNotThrowAnyException();
  }

  @Test
  void refusesTheSamePairTwiceOnTheSameGrounds() throws Exception {
    insert("rice", "potato", "CARBS", "100.0", null);

    assertThatThrownBy(() -> insert("rice", "potato", "CARBS", "150.0", null))
        .isInstanceOf(SQLException.class);
  }

  /** Advice in one direction is not advice in the other; both may exist and neither is implied. */
  @Test
  void keepsTheTwoDirectionsApart() throws Exception {
    insert("rice", "potato", "CARBS", "100.0", null);

    assertThatCode(() -> insert("potato", "rice", "CARBS", "250.0", null))
        .doesNotThrowAnyException();
    assertThat(column("SELECT source_food_id FROM food_equivalence ORDER BY source_food_id"))
        .containsExactly("potato", "rice");
  }

  @Test
  void refusesAFoodStandingInForItself() {
    assertThatThrownBy(() -> insert("rice", "rice", "CARBS", "100.0", null))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesAGroundsThatIsNotOneOfTheFour() {
    assertThatThrownBy(() -> insert("rice", "potato", "FIBRE", "100.0", null))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesAPortionThatIsNotAPortion() {
    assertThatThrownBy(() -> insert("rice", "potato", "CARBS", "0.0", null))
        .isInstanceOf(SQLException.class);
  }

  /** A tolerance of zero marks everything as excessive, which is a way of saying nothing. */
  @Test
  void refusesAToleranceOfZero() {
    assertThatThrownBy(() -> insert("rice", "potato", "CARBS", "100.0", "0.0"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesAFoodThatIsNotInTheCatalog() {
    assertThatThrownBy(() -> insert("rice", "unicornio", "CARBS", "100.0", null))
        .isInstanceOf(SQLException.class);
  }

  /** A food with equivalences pointing at it cannot vanish under them. */
  @Test
  void refusesToDeleteAFoodSomeEquivalencePointsAt() throws Exception {
    insert("rice", "potato", "CARBS", "100.0", null);

    assertThatThrownBy(() -> execute("DELETE FROM food_catalog WHERE id = 'potato'"))
        .isInstanceOf(SQLException.class);
  }

  private static void insert(
      String source, String target, String basis, String portion, String tolerance)
      throws SQLException {
    execute(
        "INSERT INTO food_equivalence"
            + " (id, source_food_id, target_food_id, basis, source_reference_g,"
            + " max_macro_deviation_pct) VALUES (RANDOM_UUID(), '"
            + source
            + "', '"
            + target
            + "', '"
            + basis
            + "', "
            + portion
            + ", "
            + (tolerance == null ? "NULL" : tolerance)
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
