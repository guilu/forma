package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * Verifies the V28 migration (FOR-145b-2, ADR-012 design section 3, "Class B" PK reconstruction)
 * applies cleanly on a real, previously-populated single-user database and backfills every legacy
 * {@code owner_id = 'default-user'} row onto the placeholder UUID, mirroring {@link
 * UserProfilePersonalTargetsExistingProfileMigrationTest}'s "migrate a real pre-existing database"
 * pattern for V20. Plain Flyway + JDBC against an isolated H2 (PostgreSQL mode) — not the shared
 * {@code @SpringBootTest} instance other classes reuse/mutate (ADR-007), so this test's own
 * migrated database is unaffected by test execution order elsewhere in the suite.
 *
 * <p>Each of the 6 Class-B tables (user_profile, integration_connection, integration_token,
 * integration_oauth_state, integration_measure_marker, earned_achievement) is seeded with a legacy
 * {@code owner_id}-keyed row at V26 (after {@code users} exists, before V27/V28 run), then migrated
 * to V28 and asserted: the row survives under the placeholder UUID, {@code owner_id} is gone, and
 * the new primary key enforces the same idempotency/uniqueness guarantee the old one did.
 */
class ClassBUserIdMigrationTest {

  private static final String PLACEHOLDER_USER_ID = "00000000-0000-0000-0000-000000000000";

  @Test
  void v28MigratesEveryClassBTableAndBackfillsExistingRowsOntoThePlaceholderUser()
      throws Exception {
    String url = "jdbc:h2:mem:for145b2_classb_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    // Migrate up to V26 (users + placeholder seed exist; V27/V28 have not run yet).
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target("26")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT INTO user_profile (owner_id, name) VALUES ('default-user', 'Diego')");
      statement.executeUpdate(
          "INSERT INTO integration_connection (owner_id, provider, status) "
              + "VALUES ('default-user', 'WITHINGS', 'CONNECTED')");
      statement.executeUpdate(
          "INSERT INTO integration_token (owner_id, provider, access_token_ciphertext,"
              + " access_token_nonce, refresh_token_ciphertext, refresh_token_nonce,"
              + " access_token_expires_at, updated_at) VALUES ('default-user', 'WITHINGS',"
              + " x'01', x'02', x'03', x'04', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
      statement.executeUpdate(
          "INSERT INTO integration_oauth_state (owner_id, provider, state, code_verifier,"
              + " code_challenge, expires_at) VALUES ('default-user', 'WITHINGS', 's', 'v', 'c',"
              + " CURRENT_TIMESTAMP)");
      statement.executeUpdate(
          "INSERT INTO integration_measure_marker (owner_id, provider, grpid, imported_at)"
              + " VALUES ('default-user', 'WITHINGS', 123456789, CURRENT_TIMESTAMP)");
      statement.executeUpdate(
          "INSERT INTO earned_achievement (owner_id, achievement_id, earned_at)"
              + " VALUES ('default-user', 'FIRST_MEASUREMENT', CURRENT_TIMESTAMP)");
    }

    // Now run V28 (and V27, harmlessly -- no Class-A rows seeded here).
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      assertBackfilled(statement, "user_profile", "user_id = '" + PLACEHOLDER_USER_ID + "'");
      assertBackfilled(
          statement,
          "integration_connection",
          "user_id = '" + PLACEHOLDER_USER_ID + "' AND provider = 'WITHINGS'");
      assertBackfilled(
          statement,
          "integration_token",
          "user_id = '" + PLACEHOLDER_USER_ID + "' AND provider = 'WITHINGS'");
      assertBackfilled(
          statement,
          "integration_oauth_state",
          "user_id = '" + PLACEHOLDER_USER_ID + "' AND provider = 'WITHINGS'");
      assertBackfilled(
          statement,
          "integration_measure_marker",
          "user_id = '"
              + PLACEHOLDER_USER_ID
              + "' AND provider = 'WITHINGS' AND grpid = 123456789");
      assertBackfilled(
          statement,
          "earned_achievement",
          "user_id = '" + PLACEHOLDER_USER_ID + "' AND achievement_id = 'FIRST_MEASUREMENT'");

      assertColumnDoesNotExist(statement, "user_profile", "owner_id");
      assertColumnDoesNotExist(statement, "integration_connection", "owner_id");
      assertColumnDoesNotExist(statement, "integration_token", "owner_id");
      assertColumnDoesNotExist(statement, "integration_oauth_state", "owner_id");
      assertColumnDoesNotExist(statement, "integration_measure_marker", "owner_id");
      assertColumnDoesNotExist(statement, "earned_achievement", "owner_id");
    }
  }

  @Test
  void v28sNewPrimaryKeyStillEnforcesTheSameUniquenessGuaranteeAsTheOldOwnerIdKey()
      throws Exception {
    String url = "jdbc:h2:mem:for145b2_classb_pk_uniqueness;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      // user_profile: user_id alone is the PK (mirrors the old owner_id-alone PK).
      statement.executeUpdate(
          "INSERT INTO user_profile (user_id, name) VALUES ('" + PLACEHOLDER_USER_ID + "', 'A')");
      assertThatInsertViolatesPrimaryKey(
          connection,
          "INSERT INTO user_profile (user_id, name) VALUES ('" + PLACEHOLDER_USER_ID + "', 'B')");

      // earned_achievement: (user_id, achievement_id) composite PK, mirroring the old
      // (owner_id, achievement_id) PK -- the idempotency guarantee AchievementRepository relies on.
      statement.executeUpdate(
          "INSERT INTO earned_achievement (user_id, achievement_id, earned_at)"
              + " VALUES ('"
              + PLACEHOLDER_USER_ID
              + "', 'FIRST_MEASUREMENT', CURRENT_TIMESTAMP)");
      assertThatInsertViolatesPrimaryKey(
          connection,
          "INSERT INTO earned_achievement (user_id, achievement_id, earned_at)"
              + " VALUES ('"
              + PLACEHOLDER_USER_ID
              + "', 'FIRST_MEASUREMENT', CURRENT_TIMESTAMP)");
    }
  }

  private static void assertBackfilled(Statement statement, String table, String whereClause)
      throws Exception {
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
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> statement.executeQuery("SELECT " + column + " FROM " + table))
        .as("%s.%s must be dropped by V28", table, column)
        .isInstanceOf(java.sql.SQLException.class);
  }

  private static void assertThatInsertViolatesPrimaryKey(Connection connection, String sql) {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> {
              try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
              }
            })
        .isInstanceOf(java.sql.SQLException.class);
  }
}
