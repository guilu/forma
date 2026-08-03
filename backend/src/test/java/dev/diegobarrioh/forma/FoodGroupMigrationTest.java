package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * V43: food groups become a table with a foreign key instead of an enum guarded by a CHECK.
 *
 * <p>What matters here is that the move preserves everything and protects more: the 23 seeded foods
 * keep the group V35 gave them, the six original groups keep the name and icon V39 gave them, and
 * the referential integrity the CHECK used to approximate is now actually enforced in both
 * directions.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate — the {@code
 * ShoppingCatalogSeedTest} style.
 */
class FoodGroupMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:food_group_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

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

  @Test
  void seedsTheSixOriginalGroupsPlusTheFourThatWereUnreachable() throws Exception {
    assertThat(column("SELECT id FROM food_group ORDER BY sort_order"))
        .containsExactly(
            "CARBOHIDRATO",
            "PROTEINA",
            "FRUTA",
            "VERDURA",
            "GRASA",
            "LACTEO",
            "LEGUMBRE",
            "BEBIDA",
            "CONDIMENTO",
            "SUPLEMENTO");
  }

  /** Nobody should see a label or a glyph change: V39's FOOD rows move across verbatim. */
  @Test
  void theSixOriginalGroupsKeepTheNameAndIconTheyHadInCategoryDisplay() throws Exception {
    assertThat(column("SELECT name || ' ' || icon FROM food_group ORDER BY sort_order LIMIT 6"))
        .containsExactly(
            "Carbohidrato 🌾", "Proteína 🍗", "Fruta 🍎", "Verdura 🥦", "Grasa 🫒", "Lácteo 🥛");
  }

  @Test
  void everySeededFoodKeepsTheGroupV35GaveIt() throws Exception {
    assertThat(column("SELECT id FROM food_catalog WHERE food_group_id = 'CARBOHIDRATO'"))
        .containsExactlyInAnyOrder(
            "oats", "rice", "whole-wheat-pasta", "potato", "sweet-potato", "whole-wheat-bread");
    assertThat(column("SELECT id FROM food_catalog WHERE food_group_id = 'PROTEINA'")).hasSize(10);
    assertThat(column("SELECT id FROM food_catalog WHERE food_group_id = 'LACTEO'"))
        .containsExactly("skim-milk");
    assertThat(column("SELECT id FROM food_catalog WHERE food_group_id IS NULL")).isEmpty();
  }

  @Test
  void theOldCategoryColumnIsGone() throws Exception {
    assertThatThrownBy(() -> column("SELECT category FROM food_catalog"))
        .isInstanceOf(SQLException.class);
  }

  /** The half the CHECK could do, still done. */
  @Test
  void refusesAFoodFiledUnderAGroupThatDoesNotExist() {
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO food_catalog (id, name, kcal, protein_g, carbs_g, fat_g,"
                        + " food_group_id) VALUES ('x', 'X', 10, 1, 1, 1, 'NO_EXISTE')"))
        .isInstanceOf(SQLException.class);
  }

  /** The half it could not: a group with foods under it can no longer vanish. */
  @Test
  void refusesToDeleteAGroupSomeFoodStillPointsAt() {
    assertThatThrownBy(() -> execute("DELETE FROM food_group WHERE id = 'PROTEINA'"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void categoryDisplayKeepsOnlyTheShoppingAisles() throws Exception {
    assertThat(column("SELECT DISTINCT scope FROM category_display")).containsExactly("SHOPPING");
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO category_display (scope, code, label) VALUES ('FOOD', 'X', 'X')"))
        .isInstanceOf(SQLException.class);
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
