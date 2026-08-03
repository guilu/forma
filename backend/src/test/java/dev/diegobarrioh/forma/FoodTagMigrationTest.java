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
 * V50: labels a food can carry.
 *
 * <p>The vocabulary is seeded and the labelling is not, which is the distinction worth pinning: a
 * tag existing claims nothing about any food, and saying which foods are vegan is twenty-three
 * separate claims nobody has made yet.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate.
 */
class FoodTagMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:food_tag_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

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
    execute("DELETE FROM food_tag");
    execute("DELETE FROM tag WHERE id LIKE 'test-%'");
  }

  @Test
  void seedsTheVocabularyInReadingOrder() throws Exception {
    assertThat(column("SELECT name FROM tag ORDER BY sort_order"))
        .containsExactly(
            "Vegano",
            "Vegetariano",
            "Sin gluten",
            "Sin lactosa",
            "Integral",
            "Fresco",
            "Congelado",
            "Procesado",
            "Desayuno",
            "Snack",
            "Cena",
            "Post entreno");
  }

  /**
   * The three the source document lists that are readings of the macros rather than facts about a
   * food. Seeding them would invite a curator to write down an opinion the numbers will outgrow.
   */
  @Test
  void doesNotSeedTheLabelsThatAreReallyArithmetic() throws Exception {
    assertThat(column("SELECT id FROM tag"))
        .doesNotContain("alto-en-proteina", "bajo-en-grasa", "rico-en-fibra");
  }

  /** Which foods are vegan is twenty-three separate claims, and nobody has made them. */
  @Test
  void labelsNoFood() throws Exception {
    assertThat(column("SELECT food_id FROM food_tag")).isEmpty();
  }

  @Test
  void letsAFoodCarrySeveralLabelsAndALabelDescribeSeveralFoods() throws Exception {
    assertThatCode(
            () -> {
              link("salad", "vegano");
              link("salad", "fresco");
              link("vegetables", "vegano");
            })
        .doesNotThrowAnyException();

    assertThat(column("SELECT tag_id FROM food_tag WHERE food_id = 'salad' ORDER BY tag_id"))
        .containsExactly("fresco", "vegano");
    assertThat(column("SELECT food_id FROM food_tag WHERE tag_id = 'vegano' ORDER BY food_id"))
        .containsExactly("salad", "vegetables");
  }

  /** Saying the same thing twice is saying it once. */
  @Test
  void refusesTheSameLabelOnTheSameFoodTwice() throws Exception {
    link("salad", "vegano");

    assertThatThrownBy(() -> link("salad", "vegano")).isInstanceOf(SQLException.class);
  }

  @Test
  void refusesALabelNobodyDefined() {
    assertThatThrownBy(() -> link("salad", "inventado")).isInstanceOf(SQLException.class);
  }

  @Test
  void refusesToLabelAFoodThatIsNotInTheCatalog() {
    assertThatThrownBy(() -> link("unicornio", "vegano")).isInstanceOf(SQLException.class);
  }

  /** Two labels reading the same would make a list of checkboxes say one thing twice. */
  @Test
  void refusesTwoLabelsWithTheSameName() {
    assertThatThrownBy(
            () -> execute("INSERT INTO tag (id, name, sort_order) VALUES ('test-x', 'Vegano', 99)"))
        .isInstanceOf(SQLException.class);
  }

  /** A label in use cannot vanish from under the foods carrying it. */
  @Test
  void refusesToDeleteALabelSomeFoodCarries() throws Exception {
    link("salad", "vegano");

    assertThatThrownBy(() -> execute("DELETE FROM tag WHERE id = 'vegano'"))
        .isInstanceOf(SQLException.class);
  }

  private static void link(String foodId, String tagId) throws SQLException {
    execute("INSERT INTO food_tag (food_id, tag_id) VALUES ('" + foodId + "', '" + tagId + "')");
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
