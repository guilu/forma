package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.SeededFoods;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Ties the {@code SeededFoods} test fixture to what the migrations actually put in {@code
 * food_catalog}.
 *
 * <p>Domain tests take their foods as a parameter and so must supply them, which recreates in test
 * code exactly the duplication that was just removed from production code — except here it is
 * checkable. This test checks it: field by field, for all 23 rows. Edit the seed without editing
 * the fixture (or the reverse) and this fails, instead of the domain tests quietly asserting
 * against numbers the application never serves.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate — the {@code
 * ShoppingCatalogSeedTest} style. Unpinned: it must track the current head, since that is what the
 * running application sees.
 */
class SeededFoodsMatchSeedTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:seeded_foods_match;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

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
  void theFixtureMatchesTheSeededCatalogRowForRow() throws Exception {
    Map<String, FoodItem> stored = readCatalog();

    assertThat(stored.keySet())
        .containsExactlyInAnyOrderElementsOf(SeededFoods.all().stream().map(FoodItem::id).toList());
    assertThat(SeededFoods.all())
        .allSatisfy(fixture -> assertThat(stored.get(fixture.id())).isEqualTo(fixture));
  }

  private static Map<String, FoodItem> readCatalog() throws Exception {
    Map<String, FoodItem> byId = new LinkedHashMap<>();
    String sql =
        // The serving moved into food_serving in V49; the fixture still describes a whole food, so
        // it is read back the way the application reads it.
        "SELECT f.id, f.name, f.kcal, f.protein_g, f.carbs_g, f.fat_g, f.fiber_g, f.sugars_g,"
            + " f.sodium_mg, f.saturated_fat_g,"
            + " (SELECT s.grams FROM food_serving s WHERE s.food_id = f.id"
            + " AND s.default_marker = 'Y') AS serving_size_g"
            + " FROM food_catalog f";
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql)) {
      while (rs.next()) {
        byId.put(
            rs.getString("id"),
            new FoodItem(
                rs.getString("id"),
                rs.getString("name"),
                rs.getInt("kcal"),
                rs.getBigDecimal("protein_g").doubleValue(),
                rs.getBigDecimal("carbs_g").doubleValue(),
                rs.getBigDecimal("fat_g").doubleValue(),
                serving(rs.getBigDecimal("serving_size_g")),
                nullableDouble(rs.getBigDecimal("fiber_g")),
                nullableDouble(rs.getBigDecimal("sugars_g")),
                nullableDouble(rs.getBigDecimal("sodium_mg")),
                nullableDouble(rs.getBigDecimal("saturated_fat_g"))));
      }
    }
    return byId;
  }

  // Same conversions FoodCatalogService applies when it reads a row, so a mismatch here means the
  // data really differs and not merely that NUMERIC(6,1) carries a trailing zero.
  private static Integer serving(BigDecimal value) {
    return value == null ? null : value.setScale(0, RoundingMode.HALF_UP).intValue();
  }

  private static Double nullableDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }
}
