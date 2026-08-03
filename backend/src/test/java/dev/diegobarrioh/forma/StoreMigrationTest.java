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
 * V45: supermarket chains become a table with a foreign key instead of an enum guarded by a CHECK.
 *
 * <p>The move has to be invisible to the products already filed under a chain — the column keeps
 * the tokens it always held — and has to enforce in both directions what the CHECK could only do in
 * one.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate.
 */
class StoreMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:store_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

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
  void seedsTheThreeChainsTheEnumHeld() throws Exception {
    assertThat(column("SELECT id FROM store ORDER BY sort_order"))
        .containsExactly("MERCADONA", "CARREFOUR", "OTRAS");
  }

  /** Every product keeps the chain it was filed under; the column changed meaning, not contents. */
  @Test
  void everyStoredProductStillPointsAtItsChain() throws Exception {
    assertThat(column("SELECT DISTINCT store FROM store_product"))
        .isSubsetOf("MERCADONA", "CARREFOUR", "OTRAS");
    assertThat(column("SELECT id FROM store_product WHERE store IS NULL")).isEmpty();
  }

  /** The half the CHECK could do, still done. */
  @Test
  void refusesAProductFiledUnderAChainThatDoesNotExist() {
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO store_product (id, store, name, category)"
                        + " VALUES ('x', 'LIDL', 'X', 'OTROS')"))
        .isInstanceOf(SQLException.class);
  }

  /** The half it could not: a chain with products under it can no longer vanish. */
  @Test
  void refusesToDeleteAChainSomeProductStillPointsAt() {
    assertThatThrownBy(() -> execute("DELETE FROM store WHERE id = 'MERCADONA'"))
        .isInstanceOf(SQLException.class);
  }

  /**
   * Adding a chain is now an insert. It buys somewhere to file a product, not the ability to import
   * one — that still needs a StoreCatalogSource, which no row can conjure.
   */
  @Test
  void acceptsAChainNobodyCompiledIn() throws Exception {
    execute(
        "INSERT INTO store (id, name, website, sort_order)"
            + " VALUES ('LIDL', 'Lidl', 'https://www.lidl.es', 3)");
    try {
      execute(
          "INSERT INTO store_product (id, store, name, category)"
              + " VALUES ('lidl-x', 'LIDL', 'Avena Lidl', 'OTROS')");

      assertThat(column("SELECT store FROM store_product WHERE id = 'lidl-x'"))
          .containsExactly("LIDL");
    } finally {
      execute("DELETE FROM store_product WHERE id = 'lidl-x'");
      execute("DELETE FROM store WHERE id = 'LIDL'");
    }
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
