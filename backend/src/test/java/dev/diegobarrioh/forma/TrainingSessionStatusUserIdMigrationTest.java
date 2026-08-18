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
 * Verifies the V31 migration (FOR-145c, ADR-012 design section 3, "5 GAP tables") applies cleanly
 * on a real, previously-populated single-user database and fixes the {@code
 * training_session_status} cross-user PK collision: {@code session_id} alone (e.g. {@code
 * "SATURDAY:RUNNING"}) used to be the whole primary key, a day-of-week-keyed id that is IDENTICAL
 * for every user. This test seeds a legacy pre-145a row, migrates to V31, and asserts the row
 * survives under the placeholder UUID, {@code user_id} is present, and the new composite primary
 * key (user_id, session_id) — not the bare session_id — is what is actually enforced (two different
 * users CAN now share the same session_id; the same user CANNOT duplicate it).
 *
 * <p>Plain Flyway + JDBC against an isolated H2 (PostgreSQL mode) — not the shared
 * {@code @SpringBootTest} instance other classes reuse/mutate (ADR-007), mirroring {@link
 * ClassBUserIdMigrationTest}'s pattern.
 */
class TrainingSessionStatusUserIdMigrationTest {

  private static final String PLACEHOLDER_USER_ID = "00000000-0000-0000-0000-000000000000";

  @Test
  void v31BackfillsExistingRowsOntoThePlaceholderUserAndDropsTheOldBareSessionIdPrimaryKey()
      throws Exception {
    String url = "jdbc:h2:mem:for145c_training_status_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    // Migrate up to V30 (users + placeholder seed exist; V31 has not run yet).
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target("30")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT INTO training_session_status (session_id, status, notes)"
              + " VALUES ('SATURDAY:RUNNING', 'COMPLETED', 'felt good')");
    }

    // Now run V31. Pinned to 31 rather than migrating to latest: this test is about the state V31
    // leaves behind, and V60 later drops this table wholesale to re-key it by week.
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target("31")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT COUNT(*) AS total FROM training_session_status WHERE user_id = '"
                  + PLACEHOLDER_USER_ID
                  + "' AND session_id = 'SATURDAY:RUNNING' AND status = 'COMPLETED'"
                  + " AND notes = 'felt good'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt("total")).isEqualTo(1);
      }
    }
  }

  @Test
  void v31sCompositePrimaryKeyLetsDifferentUsersShareASessionIdButNotDuplicateTheirOwn()
      throws Exception {
    String url = "jdbc:h2:mem:for145c_training_status_pk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    // Pinned to 31 for the same reason as the test above: V60 re-keys this table by week.
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target("31")
        .load()
        .migrate();

    String userA = "11111111-1111-1111-1111-111111111111";
    String userB = "22222222-2222-2222-2222-222222222222";

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT INTO users (id, email, password_hash, is_active)"
              + " VALUES ('"
              + userA
              + "', 'a@example.com', '!', TRUE)");
      statement.executeUpdate(
          "INSERT INTO users (id, email, password_hash, is_active)"
              + " VALUES ('"
              + userB
              + "', 'b@example.com', '!', TRUE)");

      // Same session_id, two different users -- must be allowed (this is exactly the bug V31
      // fixes: previously these two INSERTs would collide on the same bare-session_id PK).
      statement.executeUpdate(
          "INSERT INTO training_session_status (user_id, session_id, status)"
              + " VALUES ('"
              + userA
              + "', 'SATURDAY:RUNNING', 'COMPLETED')");
      statement.executeUpdate(
          "INSERT INTO training_session_status (user_id, session_id, status)"
              + " VALUES ('"
              + userB
              + "', 'SATURDAY:RUNNING', 'PLANNED')");
    }

    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
      // Same user, same session_id twice -- must still violate the (user_id, session_id) PK.
      assertThatThrownBy(
              () -> {
                try (Statement statement = connection.createStatement()) {
                  statement.executeUpdate(
                      "INSERT INTO training_session_status (user_id, session_id, status)"
                          + " VALUES ('"
                          + userA
                          + "', 'SATURDAY:RUNNING', 'SKIPPED')");
                }
              })
          .isInstanceOf(SQLException.class);
    }
  }
}
