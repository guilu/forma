package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * Verifies the V29 migration (FOR-145b-2, ADR-012 design section 3, contract phase for "Class A")
 * applies cleanly on a real, previously-populated single-user database: every legacy {@code
 * owner_id VARCHAR} row (backfilled onto {@code user_id} by V27) survives under its {@code
 * user_id}, and {@code owner_id} is fully gone from all 5 Class-A tables (goal, meal_log_entry,
 * water_intake_entry, progress_photo, weekly_tracking_record). Mirrors {@link
 * ClassBUserIdMigrationTest}'s "migrate a real pre-existing database" pattern. Plain Flyway + JDBC
 * against an isolated H2 (PostgreSQL mode) — not the shared {@code @SpringBootTest} instance other
 * classes reuse/mutate (ADR-007).
 *
 * <p>Each table is seeded with a legacy {@code owner_id}-keyed row at V26 (after {@code users}
 * exists, before V27/V29 run), then migrated to the latest version and asserted: the row survives
 * under the placeholder UUID via {@code user_id}, and {@code owner_id} no longer exists as a
 * column.
 */
class ClassAOwnerIdDropMigrationTest {

  private static final String PLACEHOLDER_USER_ID = "00000000-0000-0000-0000-000000000000";

  @Test
  void v29DropsOwnerIdFromEveryClassATableAfterV27BackfillsUserId() throws Exception {
    String url = "jdbc:h2:mem:for145b2_classa_drop_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    // Migrate up to V26 (users + placeholder seed exist; V27/V29 have not run yet, so the
    // Class-A tables still only have the legacy owner_id column).
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target("26")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT INTO goal (id, owner_id, title, metric, target, status)"
              + " VALUES (random_uuid(), 'default-user', 'Loseit', 'WEIGHT_KG', 70.0, 'ACTIVE')");
      statement.executeUpdate(
          "INSERT INTO meal_log_entry"
              + " (id, owner_id, log_date, meal_type, name, kcal, protein_g, carbs_g, fat_g)"
              + " VALUES (random_uuid(), 'default-user', CURRENT_DATE, 'BREAKFAST', 'Avena', 300,"
              + " 12.0, 40.0, 8.0)");
      statement.executeUpdate(
          "INSERT INTO water_intake_entry (id, owner_id, log_date, volume_ml)"
              + " VALUES (random_uuid(), 'default-user', CURRENT_DATE, 500.0)");
      statement.executeUpdate(
          "INSERT INTO progress_photo (id, owner_id, content_type, size_bytes, storage_ref,"
              + " created_at) VALUES (random_uuid(), 'default-user', 'image/png', 1024,"
              + " 'ref-1', CURRENT_TIMESTAMP)");
      statement.executeUpdate(
          "INSERT INTO weekly_tracking_record (id, owner_id, week, record_date)"
              + " VALUES (random_uuid(), 'default-user', 1, CURRENT_DATE)");
    }

    // Now run every remaining migration, including V27 (backfill user_id) and V29 (drop
    // owner_id).
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      assertBackfilled(statement, "goal", "user_id = '" + PLACEHOLDER_USER_ID + "'");
      assertBackfilled(statement, "meal_log_entry", "user_id = '" + PLACEHOLDER_USER_ID + "'");
      assertBackfilled(statement, "water_intake_entry", "user_id = '" + PLACEHOLDER_USER_ID + "'");
      assertBackfilled(statement, "progress_photo", "user_id = '" + PLACEHOLDER_USER_ID + "'");
      assertBackfilled(
          statement, "weekly_tracking_record", "user_id = '" + PLACEHOLDER_USER_ID + "'");

      assertColumnDoesNotExist(statement, "goal", "owner_id");
      assertColumnDoesNotExist(statement, "meal_log_entry", "owner_id");
      assertColumnDoesNotExist(statement, "water_intake_entry", "owner_id");
      assertColumnDoesNotExist(statement, "progress_photo", "owner_id");
      assertColumnDoesNotExist(statement, "weekly_tracking_record", "owner_id");
    }
  }

  private static void assertBackfilled(Statement statement, String table, String whereClause)
      throws SQLException {
    try (ResultSet rs =
        statement.executeQuery(
            "SELECT COUNT(*) AS total FROM " + table + " WHERE " + whereClause)) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt("total"))
          .as("%s backfilled onto the placeholder user", table)
          .isEqualTo(1);
    }
  }

  private static void assertColumnDoesNotExist(Statement statement, String table, String column) {
    assertThatThrownBy(() -> statement.executeQuery("SELECT " + column + " FROM " + table))
        .as("%s.%s must be dropped by V29", table, column)
        .isInstanceOf(SQLException.class);
  }
}
