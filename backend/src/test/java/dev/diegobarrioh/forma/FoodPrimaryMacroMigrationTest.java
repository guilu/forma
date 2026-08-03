package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.PrimaryMacro;
import dev.diegobarrioh.forma.domain.SeededFoods;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * V44: every seeded food gets the macro it is mostly made of.
 *
 * <p>The rule lives twice — once as SQL in the migration, once as {@link PrimaryMacro#dominantOf}
 * in the domain — because a backfill runs in the database and new foods are classified in Java. Two
 * copies of a rule drift, so this test runs one against the other over all 23 rows rather than
 * asserting a hand-written list nobody would notice going stale.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate.
 */
class FoodPrimaryMacroMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:food_primary_macro;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

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
  void theBackfillAgreesWithTheDomainRuleForEverySeededFood() throws Exception {
    Map<String, String> stored = storedMacros();

    assertThat(SeededFoods.all())
        .allSatisfy(
            food ->
                assertThat(stored.get(food.id()))
                    .as("primary macro of %s", food.id())
                    .isEqualTo(expected(food)));
  }

  /**
   * Two results worth writing down because they look wrong and are not: a whole egg carries more
   * protein than fat by weight and is still mostly fat by calories, and skimmed milk's lactose
   * outweighs its protein.
   */
  @Test
  void classifiesByCaloriesNotByGrams() throws Exception {
    assertThat(storedMacros()).containsEntry("eggs", "FAT").containsEntry("skim-milk", "CARBS");
  }

  @Test
  void refusesAMacroThatIsNotOneOfTheThree() {
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO food_catalog (id, name, kcal, protein_g, carbs_g, fat_g,"
                        + " primary_macro) VALUES ('x', 'X', 10, 1, 1, 1, 'FIBRA')"))
        .isInstanceOf(SQLException.class);
  }

  private static String expected(FoodItem food) {
    return PrimaryMacro.dominantOf(food.proteinPer100g(), food.carbsPer100g(), food.fatPer100g())
        .map(Enum::name)
        .orElse(null);
  }

  private static Map<String, String> storedMacros() throws SQLException {
    Map<String, String> byId = new LinkedHashMap<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT id, primary_macro FROM food_catalog")) {
      while (rs.next()) {
        byId.put(rs.getString("id"), rs.getString("primary_macro"));
      }
    }
    return byId;
  }

  private static void execute(String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
