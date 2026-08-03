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
 * V49: a food's portions move out of food_catalog and into a table that can hold more than one.
 *
 * <p>Two things matter. That nothing is lost — every food keeps the portion it had, as its default
 * — and that the one-default-per-food rule really holds, because it is enforced by a trick (a
 * nullable sentinel) rather than by the partial index H2 cannot express.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate.
 */
class FoodServingMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:food_serving_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

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
    execute("DELETE FROM food_serving WHERE id LIKE 'test-%'");
  }

  /** Every seeded food had a portion, and every one of them still has it. */
  @Test
  void movesEveryFoodsPortionAcrossUntouched() throws Exception {
    assertThat(column("SELECT grams FROM food_serving WHERE food_id = 'oats'"))
        .containsExactly("60.0");
    assertThat(column("SELECT grams FROM food_serving WHERE food_id = 'olive-oil'"))
        .containsExactly("10.0");
    assertThat(column("SELECT COUNT(*) FROM food_serving")).containsExactly("23");
  }

  /**
   * The portion a food starts with is its default, and it has no name until somebody gives it one.
   */
  @Test
  void bringsThemAcrossAsTheDefaultAndUnnamed() throws Exception {
    assertThat(column("SELECT default_marker FROM food_serving")).containsOnly("Y");
    assertThat(column("SELECT name FROM food_serving")).containsOnlyNulls();
  }

  @Test
  void theOldColumnIsGone() {
    assertThatThrownBy(() -> column("SELECT serving_size_g FROM food_catalog"))
        .isInstanceOf(SQLException.class);
  }

  /** The point of the table: a banana is small, medium or large. */
  @Test
  void holdsSeveralNamedPortionsForOneFood() throws Exception {
    insert("test-s", "banana", "'Pequeño'", "90.0", null, 1);
    insert("test-l", "banana", "'Grande'", "150.0", null, 2);

    assertThat(
            column(
                "SELECT name FROM food_serving WHERE food_id = 'banana'"
                    + " AND name IS NOT NULL ORDER BY sort_order"))
        .containsExactly("Pequeño", "Grande");
  }

  /**
   * The rule the sentinel exists for. A second default would make "one serving of banana"
   * ambiguous, and H2 cannot express the partial index that would be the obvious way to forbid it.
   */
  @Test
  void refusesASecondDefaultForTheSameFood() {
    assertThatThrownBy(() -> insert("test-d", "banana", "'Grande'", "150.0", "'Y'", 1))
        .isInstanceOf(SQLException.class);
  }

  /** And any number of non-default portions, which is what NULLS DISTINCT buys. */
  @Test
  void allowsManyPortionsWithoutADefaultMarker() {
    assertThatCode(
            () -> {
              insert("test-1", "banana", "'Pequeño'", "90.0", null, 1);
              insert("test-2", "banana", "'Grande'", "150.0", null, 2);
              insert("test-3", "banana", "'Enorme'", "200.0", null, 3);
            })
        .doesNotThrowAnyException();
  }

  /** Two foods each having a default is the normal case, not a clash. */
  @Test
  void letsEveryFoodHaveItsOwnDefault() throws Exception {
    assertThat(column("SELECT COUNT(*) FROM food_serving WHERE default_marker = 'Y'"))
        .containsExactly("23");
  }

  @Test
  void refusesAPortionOfNothing() {
    assertThatThrownBy(() -> insert("test-z", "banana", "'Nada'", "0", null, 1))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesAMarkerThatIsNotTheOne() {
    assertThatThrownBy(() -> insert("test-x", "banana", "'Raro'", "100.0", "'X'", 1))
        .isInstanceOf(SQLException.class);
  }

  /** A portion belongs to a food, and cannot outlive it or precede it. */
  @Test
  void refusesAPortionOfAFoodThatDoesNotExist() {
    assertThatThrownBy(() -> insert("test-g", "unicornio", "'Ala'", "100.0", null, 1))
        .isInstanceOf(SQLException.class);
  }

  private static void insert(
      String id, String foodId, String name, String grams, String marker, int order)
      throws SQLException {
    execute(
        "INSERT INTO food_serving (id, food_id, name, grams, default_marker, sort_order) VALUES ('"
            + id
            + "', '"
            + foodId
            + "', "
            + name
            + ", "
            + grams
            + ", "
            + (marker == null ? "NULL" : marker)
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
