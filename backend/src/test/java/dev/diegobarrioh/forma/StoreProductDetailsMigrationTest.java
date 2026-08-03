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
 * V48: what the shop says about a product beyond its name and price.
 *
 * <p>Most of these columns exist because the data was already arriving and being dropped, so what
 * matters is that the defaults tell the truth about the rows that predate them, and that the one
 * pair which only makes sense together cannot be half filled.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate.
 */
class StoreProductDetailsMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:store_product_details;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

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
    execute("DELETE FROM store_product WHERE id LIKE 'test-%'");
  }

  /**
   * A product that was on sale when its row was written is not "unknown availability". Defaulting
   * to unavailable would retire the whole catalog in one migration.
   */
  @Test
  void treatsEveryExistingProductAsStillOnSale() throws Exception {
    assertThat(column("SELECT id FROM store_product WHERE available = FALSE")).isEmpty();
  }

  /** Nobody has been checked against a shop yet, and pretending otherwise would date the prices. */
  @Test
  void leavesTheLastSyncUnknownUntilSomethingSyncs() throws Exception {
    assertThat(column("SELECT last_synced_at FROM store_product")).containsOnlyNulls();
  }

  @Test
  void holdsTheAmountAndTheUnitTheShopStated() throws Exception {
    insert("test-oil", "package_amount, package_unit", "5.0, 'l'");

    assertThat(
            column(
                "SELECT package_amount || ' ' || package_unit FROM store_product"
                    + " WHERE id = 'test-oil'"))
        .containsExactly("5.000 l");
  }

  /** An amount with no unit is a number nobody can read; a unit with no amount says nothing. */
  @Test
  void refusesHalfAPackageSize() {
    assertThatThrownBy(() -> insert("test-a", "package_amount", "5.0"))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> insert("test-b", "package_unit", "'l'"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesAPackageOfNothing() {
    assertThatThrownBy(() -> insert("test-c", "package_amount, package_unit", "0, 'l'"))
        .isInstanceOf(SQLException.class);
  }

  /** Both absent is the normal state: a product typed by hand states no size in any usable form. */
  @Test
  void acceptsAProductWithNoStatedPackageSize() {
    assertThatCode(() -> insert("test-d", "package_size", "'Un puñado'"))
        .doesNotThrowAnyException();
  }

  @Test
  void holdsABarcodeAndABrand() throws Exception {
    insert("test-e", "ean, brand", "'8480000123456', 'Hacendado'");

    assertThat(column("SELECT ean FROM store_product WHERE id = 'test-e'"))
        .containsExactly("8480000123456");
    assertThat(column("SELECT brand FROM store_product WHERE id = 'test-e'"))
        .containsExactly("Hacendado");
  }

  /**
   * food_catalog is the table everything else derives from — an equivalence computes its grams from
   * these numbers — so when they last changed is a question somebody will ask.
   */
  @Test
  void givesEveryFoodAnUpdatedAtMatchingWhenItWasCreated() throws Exception {
    assertThat(column("SELECT id FROM food_catalog WHERE updated_at IS NULL")).isEmpty();
    assertThat(column("SELECT id FROM food_catalog WHERE updated_at <> created_at")).isEmpty();
  }

  private static void insert(String id, String extraColumns, String extraValues)
      throws SQLException {
    execute(
        "INSERT INTO store_product (id, store, name, category, "
            + extraColumns
            + ") VALUES ('"
            + id
            + "', 'MERCADONA', 'X', 'OTROS', "
            + extraValues
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
