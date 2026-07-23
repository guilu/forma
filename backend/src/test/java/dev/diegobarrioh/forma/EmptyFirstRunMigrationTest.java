package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * Verifies FOR-169's V23 "empty first-run" cleanup: a freshly migrated database contains no active
 * user data (the known personal/demo seeds from V5/V20/V22 are removed), and an existing install's
 * real data is preserved. Plain Flyway + JDBC against an isolated H2 (PostgreSQL mode), matching
 * {@link ShoppingCatalogSeedTest}/{@code UserProfilePersonalTargetsSeedTest}.
 */
class EmptyFirstRunMigrationTest {

  private static int count(Connection connection, String table) throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT COUNT(*) AS total FROM " + table)) {
      assertThat(rs.next()).isTrue();
      return rs.getInt("total");
    }
  }

  @Test
  void freshDatabaseHasNoActiveUserDataAfterV23() throws Exception {
    String url = "jdbc:h2:mem:for169_fresh_empty;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
      // Seeds removed by V23.
      assertThat(count(connection, "shopping_products")).isZero();
      assertThat(count(connection, "shopping_lists")).isZero();
      assertThat(count(connection, "shopping_list_items")).isZero();
      assertThat(count(connection, "user_profile")).isZero();
      // Never seeded in the first place — still empty on a fresh DB.
      assertThat(count(connection, "body_measurements")).isZero();
      assertThat(count(connection, "goal")).isZero();
      assertThat(count(connection, "goal_milestone")).isZero();
      assertThat(count(connection, "weekly_tracking_record")).isZero();
    }
  }

  @Test
  void existingInstallRealDataIsPreservedOnlyKnownSeedsRemoved() throws Exception {
    String url = "jdbc:h2:mem:for169_preserve_real;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    // Migrate up to V22: the seeds (Diego profile + 23 Mercadona products + active list) are
    // present.
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target("22")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      // Simulate a real, onboarded install: the profile has completed onboarding
      // (must NOT be deleted), and the user created their own shopping product
      // (random id, not a seed id — must NOT be deleted).
      statement.executeUpdate(
          "UPDATE user_profile SET first_run_completed = TRUE WHERE owner_id = 'default-user'");
      statement.executeUpdate(
          "INSERT INTO shopping_products (id, name, estimated_price_eur, linked_food_item_id) "
              + "VALUES ('99999999-9999-9999-9999-999999999999', 'Producto del usuario', 1.00, 'oats')");
    }

    // Now run V23.
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
      // The onboarded profile survives (first_run_completed = true).
      assertThat(count(connection, "user_profile")).isEqualTo(1);
      // The user's own product survives; the 23 seed products are gone.
      assertThat(count(connection, "shopping_products")).isEqualTo(1);
      try (Statement statement = connection.createStatement();
          ResultSet rs =
              statement.executeQuery(
                  "SELECT name FROM shopping_products "
                      + "WHERE id = '99999999-9999-9999-9999-999999999999'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getString("name")).isEqualTo("Producto del usuario");
      }
    }
  }
}
