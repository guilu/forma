package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the V22 migration's data reseed (FOR-152, epic FOR-148 slice 4) directly against a
 * freshly migrated database, isolated from the shared {@code application-test.yml} H2 instance
 * other {@code @SpringBootTest} classes reuse and mutate — matching {@code
 * UserProfilePersonalTargetsSeedTest}'s style (plain Flyway + JDBC, no Spring context).
 */
class ShoppingCatalogSeedTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:for152_shopping_catalog_seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  private static Connection connection;
  private static Flyway flyway;

  @BeforeAll
  static void migrate() throws Exception {
    flyway =
        Flyway.configure()
            .dataSource(JDBC_URL, "sa", "")
            .locations("classpath:db/migration")
            .load();
    flyway.migrate();
    connection = DriverManager.getConnection(JDBC_URL, "sa", "");
  }

  @AfterAll
  static void closeConnection() throws Exception {
    connection.close();
  }

  @Test
  void migrationHeadIsAtLeastV22() {
    MigrationInfo v22 =
        Arrays.stream(flyway.info().applied())
            .filter(info -> info.getVersion() != null)
            .filter(info -> "22".equals(info.getVersion().getVersion()))
            .findFirst()
            .orElse(null);

    assertThat(v22).isNotNull();
    assertThat(v22.getState().isApplied()).isTrue();
  }

  @Test
  void seedsExactlyTwentyThreeShoppingProducts() throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS total FROM shopping_products")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt("total")).isEqualTo(23);
    }
  }

  @Test
  void theFourDemoProductsFromV5AreGone() throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT name FROM shopping_products WHERE id IN ("
                    + "'11111111-1111-1111-1111-111111111111',"
                    + "'22222222-2222-2222-2222-222222222222',"
                    + "'33333333-3333-3333-3333-333333333333',"
                    + "'44444444-4444-4444-4444-444444444444')")) {
      assertThat(rs.next()).isFalse();
    }

    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT COUNT(*) AS total FROM shopping_products WHERE name IN ("
                    + "'Avena 1 kg', 'Pollo (pechuga) 1 kg', 'Arroz 1 kg', 'Plátano (manojo)')")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt("total")).isEqualTo(0);
    }
  }

  @Test
  void everyProductHasAUrlOrIsDocumentedAsExternalAndLinksToAFoodCatalogId() throws Exception {
    Set<String> linkedFoodIds = new HashSet<>();
    int withUrl = 0;
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT url, linked_food_item_id, notes FROM shopping_products")) {
      while (rs.next()) {
        String url = rs.getString("url");
        String linkedFoodItemId = rs.getString("linked_food_item_id");
        assertThat(linkedFoodItemId).isNotBlank();
        linkedFoodIds.add(linkedFoodItemId);
        if (url != null) {
          withUrl++;
        } else {
          // The only product without a real Mercadona URL is the whey protein supplement,
          // documented as external via its notes (never a fabricated link).
          assertThat(rs.getString("notes")).contains("No suele ser compra Mercadona");
        }
      }
    }

    assertThat(linkedFoodIds).hasSize(23); // every product links to a distinct FoodCatalog id
    assertThat(withUrl).isEqualTo(22);
  }

  @Test
  void copperPollo00KgProductHasRealMercadonaPriceAndCategory() throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT estimated_price_eur, price_per_unit_eur, category, linked_food_item_id "
                    + "FROM shopping_products WHERE name = 'Pechugas enteras de pollo'")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getBigDecimal("estimated_price_eur")).isEqualByComparingTo("14.40");
      assertThat(rs.getBigDecimal("price_per_unit_eur")).isEqualByComparingTo("7.20");
      assertThat(rs.getString("category")).isEqualTo("PROTEINAS");
      assertThat(rs.getString("linked_food_item_id")).isEqualTo("chicken");
    }
  }

  @Test
  void exactlyOneActiveShoppingListWithTwentyThreeItems() throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT COUNT(*) AS total FROM shopping_lists WHERE status = 'ACTIVE'")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt("total")).isEqualTo(1);
    }

    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery("SELECT COUNT(*) AS total FROM shopping_list_items")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt("total")).isEqualTo(23);
    }
  }

  @Test
  void theDerivedWeeklyTotalIsApproximatelyOneHundredFourEurosEleven() throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT SUM(estimated_price_eur) AS total FROM shopping_products")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getBigDecimal("total")).isEqualByComparingTo(new BigDecimal("104.11"));
    }
  }
}
