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
 * Verifies the V60 migration, which re-keys {@code training_session_status} by week and stops using
 * the day of the week as the session's identity.
 *
 * <p>Before V60 the primary key was {@code (user_id, session_id)} with {@code session_id =
 * "MONDAY:RUNNING"}: nothing recorded which week a row belonged to, so a session completed once was
 * replayed onto every later week forever (the "Resumen semanal shows 2/6 on an untouched Monday"
 * bug), and a session could not be moved to another day without changing its identity.
 *
 * <p>Plain Flyway + JDBC against an isolated H2 (PostgreSQL mode) — not the shared {@code
 * SpringBootTest} instance other classes reuse/mutate (ADR-007), mirroring {@link
 * TrainingSessionStatusUserIdMigrationTest}'s pattern.
 */
class TrainingSessionWeekScopeMigrationTest {

  private static final String PLACEHOLDER_USER_ID = "00000000-0000-0000-0000-000000000000";

  @Test
  void v60DiscardsPreExistingRowsBecauseTheirWeekWasNeverRecorded() throws Exception {
    String url = "jdbc:h2:mem:v60_training_week_discard;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    // Migrate up to V59 (the old day-keyed table still exists) and seed a completed session.
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target("59")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT INTO training_session_status (user_id, session_id, status, notes)"
              + " VALUES ('"
              + PLACEHOLDER_USER_ID
              + "', 'SATURDAY:RUNNING', 'COMPLETED', 'stale')");
    }

    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery("SELECT COUNT(*) AS total FROM training_session_status")) {
      assertThat(rs.next()).isTrue();
      // Deliberately dropped: the row's week is not recoverable from anything in the schema, so
      // carrying it over would mean inventing a week_start rather than migrating one.
      assertThat(rs.getInt("total")).isZero();
    }
  }

  @Test
  void v60LetsTheSameSessionRepeatAcrossWeeksButNotWithinOne() throws Exception {
    String url = "jdbc:h2:mem:v60_training_week_pk;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      // The same session in two different weeks: the whole point of the migration. Under the old
      // PK this was impossible — one row per session, forever.
      statement.executeUpdate(
          "INSERT INTO training_session_status"
              + " (user_id, week_start, session_key, status, completed_at)"
              + " VALUES ('"
              + PLACEHOLDER_USER_ID
              + "', DATE '2026-08-10', 'RUNNING:LONG_RUN', 'COMPLETED', TIMESTAMP '2026-08-15 09:30:00')");
      statement.executeUpdate(
          "INSERT INTO training_session_status (user_id, week_start, session_key, status)"
              + " VALUES ('"
              + PLACEHOLDER_USER_ID
              + "', DATE '2026-08-17', 'RUNNING:LONG_RUN', 'PLANNED')");
    }

    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
      // Same user, same week, same session twice — still a primary key violation.
      assertThatThrownBy(
              () -> {
                try (Statement statement = connection.createStatement()) {
                  statement.executeUpdate(
                      "INSERT INTO training_session_status"
                          + " (user_id, week_start, session_key, status)"
                          + " VALUES ('"
                          + PLACEHOLDER_USER_ID
                          + "', DATE '2026-08-17', 'RUNNING:LONG_RUN', 'SKIPPED')");
                }
              })
          .isInstanceOf(SQLException.class);
    }
  }

  @Test
  void v60StoresTheDayOverrideThatMakesRescheduleWithinAWeekPossible() throws Exception {
    String url = "jdbc:h2:mem:v60_training_week_day;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      // Monday's easy run, moved to Tuesday for this week only.
      statement.executeUpdate(
          "INSERT INTO training_session_status"
              + " (user_id, week_start, session_key, scheduled_day, status)"
              + " VALUES ('"
              + PLACEHOLDER_USER_ID
              + "', DATE '2026-08-17', 'RUNNING:EASY', 'TUESDAY', 'PLANNED')");
    }

    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT scheduled_day FROM training_session_status"
                    + " WHERE session_key = 'RUNNING:EASY'")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString("scheduled_day")).isEqualTo("TUESDAY");
    }
  }
}
