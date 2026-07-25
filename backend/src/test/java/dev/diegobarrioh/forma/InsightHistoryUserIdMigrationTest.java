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
 * Verifies the V34 migration (FOR-145c, ADR-012 design section 3, "5 GAP tables") applies cleanly
 * on a real, previously-populated single-user database and fixes the {@code insight_history} /
 * {@code insight_history_recommendation} cross-user PK collision: {@code week_start_date} alone
 * used to be the whole primary key of {@code insight_history} (and part of {@code
 * insight_history_recommendation}'s), a period key that is IDENTICAL for every user. This test
 * seeds a legacy pre-145a period with one main + one secondary recommendation, migrates to V34, and
 * asserts both tables' rows survive under the placeholder UUID and the new composite primary keys —
 * not the bare {@code week_start_date} — are what is actually enforced.
 *
 * <p>Plain Flyway + JDBC against an isolated H2 (PostgreSQL mode) — not the shared
 * {@code @SpringBootTest} instance other classes reuse/mutate (ADR-007), mirroring {@link
 * ClassBUserIdMigrationTest} / {@link TrainingSessionStatusUserIdMigrationTest}'s pattern.
 */
class InsightHistoryUserIdMigrationTest {

  private static final String PLACEHOLDER_USER_ID = "00000000-0000-0000-0000-000000000000";

  @Test
  void v34BackfillsExistingPeriodAndRecommendationsOntoThePlaceholderUser() throws Exception {
    String url = "jdbc:h2:mem:for145c_insight_history_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    // Migrate up to V33 (users + placeholder seed exist; V34 has not run yet).
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target("33")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT INTO insight_history"
              + " (week_start_date, planned_running_sessions, completed_running_sessions,"
              + " planned_strength_sessions, completed_strength_sessions, generated_at)"
              + " VALUES (DATE '2026-07-20', 3, 2, 3, 1, CURRENT_TIMESTAMP)");
      statement.executeUpdate(
          "INSERT INTO insight_history_recommendation"
              + " (week_start_date, sort_order, is_main, category, severity, message, reason,"
              + " created_at)"
              + " VALUES (DATE '2026-07-20', 0, TRUE, 'TRAINING', 'INFO', 'msg', 'reason',"
              + " CURRENT_TIMESTAMP)");
      statement.executeUpdate(
          "INSERT INTO insight_history_recommendation"
              + " (week_start_date, sort_order, is_main, category, severity, message, reason,"
              + " created_at)"
              + " VALUES (DATE '2026-07-20', 1, FALSE, 'NUTRITION', 'WARNING', 'msg2', 'reason2',"
              + " CURRENT_TIMESTAMP)");
    }

    // Now run V34.
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT COUNT(*) AS total FROM insight_history WHERE user_id = '"
                  + PLACEHOLDER_USER_ID
                  + "' AND week_start_date = DATE '2026-07-20'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt("total")).isEqualTo(1);
      }
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT COUNT(*) AS total FROM insight_history_recommendation WHERE user_id = '"
                  + PLACEHOLDER_USER_ID
                  + "' AND week_start_date = DATE '2026-07-20'")) {
        assertThat(rs.next()).isTrue();
        assertThat(rs.getInt("total")).isEqualTo(2);
      }
    }
  }

  @Test
  void v34sCompositePrimaryKeysLetDifferentUsersShareAWeekButNotDuplicateTheirOwn()
      throws Exception {
    String url = "jdbc:h2:mem:for145c_insight_history_pk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
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

      // Same week_start_date, two different users -- must be allowed (this is exactly the bug
      // V34 fixes: previously these two INSERTs would collide on the same bare-date PK).
      statement.executeUpdate(
          "INSERT INTO insight_history"
              + " (user_id, week_start_date, planned_running_sessions, completed_running_sessions,"
              + " planned_strength_sessions, completed_strength_sessions, generated_at)"
              + " VALUES ('"
              + userA
              + "', DATE '2026-07-20', 3, 2, 3, 1, CURRENT_TIMESTAMP)");
      statement.executeUpdate(
          "INSERT INTO insight_history"
              + " (user_id, week_start_date, planned_running_sessions, completed_running_sessions,"
              + " planned_strength_sessions, completed_strength_sessions, generated_at)"
              + " VALUES ('"
              + userB
              + "', DATE '2026-07-20', 3, 0, 3, 0, CURRENT_TIMESTAMP)");
    }

    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
      // Same user, same week_start_date twice -- must still violate the
      // (user_id, week_start_date) PK.
      assertThatThrownBy(
              () -> {
                try (Statement statement = connection.createStatement()) {
                  statement.executeUpdate(
                      "INSERT INTO insight_history"
                          + " (user_id, week_start_date, planned_running_sessions,"
                          + " completed_running_sessions, planned_strength_sessions,"
                          + " completed_strength_sessions, generated_at)"
                          + " VALUES ('"
                          + userA
                          + "', DATE '2026-07-20', 1, 1, 1, 1, CURRENT_TIMESTAMP)");
                }
              })
          .isInstanceOf(SQLException.class);
    }
  }
}
