package dev.diegobarrioh.forma;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * V57: the legacy placeholder's data is handed to the one real account.
 *
 * <p>Each case gets its OWN database, migrated from nothing with a different starting population,
 * because what this migration does depends entirely on what it finds: one real account, none, or
 * several. A single shared instance could only ever test one of the three.
 */
class AdoptLegacyAccountMigrationTest {

  private static final String PLACEHOLDER = "00000000-0000-0000-0000-000000000000";
  private static final String DIEGO = "11111111-2222-4333-8444-555555555555";
  private static final String SOMEBODY_ELSE = "99999999-2222-4333-8444-555555555555";

  /**
   * Migrates up to V56, runs {@code before}, then applies V57.
   *
   * <p>Split at V56 so the fixture can create accounts and rows the way a real install would —
   * after everything was seeded onto the placeholder and before anybody adopted it.
   */
  private static Connection upToV56Then(String name, Sql before) throws Exception {
    String url = "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .target(org.flywaydb.core.api.MigrationVersion.fromVersion("56"))
        .load()
        .migrate();
    Connection connection = DriverManager.getConnection(url, "sa", "");
    before.run(connection);
    Flyway.configure()
        .dataSource(url, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();
    return connection;
  }

  @FunctionalInterface
  private interface Sql {
    void run(Connection connection) throws SQLException;
  }

  /** The whole point: the seeded plan ends up on the account somebody actually signs into. */
  @Test
  void handsTheSeededPlansToTheOneRealAccount() throws Exception {
    Connection db = upToV56Then("adopt_one", AdoptLegacyAccountMigrationTest::registerDiego);

    assertThat(column(db, "SELECT name FROM nutrition_plan WHERE user_id = '" + DIEGO + "'"))
        .contains("Dieta semanal — recomposición", "Plan base");
    assertThat(column(db, "SELECT id FROM nutrition_plan WHERE user_id = '" + PLACEHOLDER + "'"))
        .isEmpty();
  }

  /** And it is still the ACTIVE one, which is what the nutrition page reads. */
  @Test
  void keepsTheDietActiveUnderItsNewOwner() throws Exception {
    Connection db = upToV56Then("adopt_active", AdoptLegacyAccountMigrationTest::registerDiego);

    assertThat(
            column(
                db,
                "SELECT name FROM nutrition_plan WHERE user_id = '"
                    + DIEGO
                    + "' AND active_marker IS NOT NULL"))
        .containsExactly("Dieta semanal — recomposición");
  }

  /**
   * The profile goes too — height, baseline, targets.
   *
   * <p>Seeded here rather than relied upon: V20 wrote Diego's profile and V23 DELETES it again on
   * any install where onboarding was never completed, so a database migrated from nothing has none.
   * The case worth testing is the one where it survived.
   */
  @Test
  void handsOverTheProfileWithItsRealFigures() throws Exception {
    Connection db =
        upToV56Then(
            "adopt_profile",
            connection -> {
              registerDiego(connection);
              legacyProfile(connection);
            });

    assertThat(column(db, "SELECT height_cm FROM user_profile WHERE user_id = '" + DIEGO + "'"))
        .containsExactly("180.0");
  }

  /** Every user-scoped table, not just the ones somebody complained about. */
  @Test
  void leavesNothingOfConsequenceOnThePlaceholder() throws Exception {
    Connection db = upToV56Then("adopt_all", AdoptLegacyAccountMigrationTest::registerDiego);

    for (String table :
        List.of(
            "body_measurements",
            "goal",
            "meal_log_entry",
            "water_intake_entry",
            "progress_photo",
            "shopping_lists",
            "shopping_products",
            "weekly_tracking_record",
            "user_profile",
            "earned_achievement",
            "training_session_status",
            "integration_connection",
            "integration_token",
            "integration_measure_marker",
            "integration_oauth_state",
            "insight_history",
            "insight_history_recommendation",
            "nutrition_plan")) {
      assertThat(
              column(
                  db, "SELECT COUNT(*) FROM " + table + " WHERE user_id = '" + PLACEHOLDER + "'"))
          .describedAs("quedan filas del marcador en %s", table)
          .containsExactly("0");
    }
  }

  /**
   * What the account already had wins.
   *
   * <p>Somebody who registered and filled in the onboarding has a profile of their own, and it is
   * newer and more theirs than the one transcribed from a spreadsheet.
   */
  @Test
  void doesNotOverwriteAProfileTheAccountAlreadyHad() throws Exception {
    Connection db =
        upToV56Then(
            "adopt_keeps_own",
            connection -> {
              registerDiego(connection);
              legacyProfile(connection);
              execute(
                  connection,
                  "INSERT INTO user_profile (user_id, name, height_cm) VALUES ('"
                      + DIEGO
                      + "', 'Suyo', 172.0)");
            });

    assertThat(column(db, "SELECT name FROM user_profile WHERE user_id = '" + DIEGO + "'"))
        .containsExactly("Suyo");
    // The one that could not move stays where it is rather than being destroyed to tidy up.
    assertThat(column(db, "SELECT name FROM user_profile WHERE user_id = '" + PLACEHOLDER + "'"))
        .containsExactly("Diego");
  }

  /**
   * A plan the account was already following keeps following.
   *
   * <p>The seeded one still arrives — a plan that exists is worth more than one thrown away — but
   * as COMPLETED, because the unique index admits one active plan per account and the account's own
   * choice is the one that stands.
   */
  @Test
  void arrivesAsCompletedWhenTheAccountAlreadyFollowsAPlan() throws Exception {
    Connection db =
        upToV56Then(
            "adopt_own_plan",
            connection -> {
              registerDiego(connection);
              execute(
                  connection,
                  "INSERT INTO nutrition_plan (id, user_id, name, status, active_marker)"
                      + " VALUES ('aaaaaaaa-9999-4000-8000-000000000001', '"
                      + DIEGO
                      + "', 'El mío', 'ACTIVE', '1')");
            });

    assertThat(
            column(
                db,
                "SELECT name FROM nutrition_plan WHERE user_id = '"
                    + DIEGO
                    + "' AND active_marker IS NOT NULL"))
        .containsExactly("El mío");
    assertThat(
            column(
                db,
                "SELECT status FROM nutrition_plan WHERE name = 'Dieta semanal — recomposición'"))
        .containsExactly("COMPLETED");
  }

  /** The insights and their recommendations move together, composite foreign key and all. */
  @Test
  void movesTheInsightsWithTheirRecommendations() throws Exception {
    Connection db =
        upToV56Then(
            "adopt_insights",
            connection -> {
              registerDiego(connection);
              execute(
                  connection,
                  "INSERT INTO insight_history (user_id, week_start_date, planned_running_sessions,"
                      + " completed_running_sessions, planned_strength_sessions,"
                      + " completed_strength_sessions, generated_at) VALUES ('"
                      + PLACEHOLDER
                      + "', DATE '2026-07-20', 3, 2, 3, 3, CURRENT_TIMESTAMP)");
              execute(
                  connection,
                  "INSERT INTO insight_history_recommendation (user_id, week_start_date, sort_order,"
                      + " is_main, category, severity, message, reason, created_at) VALUES ('"
                      + PLACEHOLDER
                      + "', DATE '2026-07-20', 0, TRUE, 'NUTRITION', 'INFO', 'Come más',"
                      + " 'Porque sí', CURRENT_TIMESTAMP)");
            });

    assertThat(
            column(
                db,
                "SELECT message FROM insight_history_recommendation WHERE user_id = '"
                    + DIEGO
                    + "'"))
        .containsExactly("Come más");
    assertThat(
            column(
                db, "SELECT week_start_date FROM insight_history WHERE user_id = '" + DIEGO + "'"))
        .hasSize(1);
  }

  /**
   * No real account, nothing happens.
   *
   * <p>A fresh install that nobody has registered on yet: the data stays where it is, waiting for
   * whoever signs up. Adopting it into nothing would just delete it.
   */
  @Test
  void doesNothingWhenNobodyHasRegistered() throws Exception {
    Connection db = upToV56Then("adopt_none", connection -> {});

    assertThat(column(db, "SELECT name FROM nutrition_plan WHERE user_id = '" + PLACEHOLDER + "'"))
        .contains("Dieta semanal — recomposición");
  }

  /**
   * Several real accounts, nothing happens either.
   *
   * <p>The migration cannot know whose the legacy data is, and guessing would hand one person's
   * measurements to another.
   */
  @Test
  void doesNothingWhenThereIsMoreThanOneRealAccount() throws Exception {
    Connection db =
        upToV56Then(
            "adopt_many",
            connection -> {
              registerDiego(connection);
              execute(
                  connection,
                  "INSERT INTO users (id, email, password_hash) VALUES ('"
                      + SOMEBODY_ELSE
                      + "', 'otro@forma.test', 'x')");
            });

    assertThat(column(db, "SELECT name FROM nutrition_plan WHERE user_id = '" + PLACEHOLDER + "'"))
        .contains("Dieta semanal — recomposición");
    assertThat(column(db, "SELECT id FROM nutrition_plan WHERE user_id = '" + DIEGO + "'"))
        .isEmpty();
  }

  /** The profile V20 seeded and V23 removes unless onboarding was completed. */
  private static void legacyProfile(Connection connection) throws SQLException {
    execute(
        connection,
        "INSERT INTO user_profile (user_id, name, height_cm) VALUES ('"
            + PLACEHOLDER
            + "', 'Diego', 180.0)");
  }

  /**
   * Sin cuenta real, las recomendaciones siguen ahí.
   *
   * <p>El caso que se me escapó: para mover el padre hay que sacar las hijas, y el DELETE que las
   * sacaba no llevaba la condición de «hay exactamente una cuenta». En una instalación sin nadie
   * registrado las borraba y no volvían. Lo cazó el test de V34 antes de que esto llegara a nadie.
   */
  @Test
  void keepsTheRecommendationsWhenThereIsNobodyToAdoptThem() throws Exception {
    Connection db =
        upToV56Then("adopt_none_insights", AdoptLegacyAccountMigrationTest::legacyInsights);

    assertThat(
            column(
                db,
                "SELECT message FROM insight_history_recommendation WHERE user_id = '"
                    + PLACEHOLDER
                    + "'"))
        .containsExactly("Come más");
  }

  /**
   * Una semana que la cuenta real ya tenía: el padre no se mueve, y sus hijas vuelven con él.
   *
   * <p>Sacarlas para mover un padre que al final no se movió y no devolverlas sería destruirlas por
   * un motivo puramente mecánico.
   */
  @Test
  void putsBackTheRecommendationsWhoseWeekCouldNotMove() throws Exception {
    Connection db =
        upToV56Then(
            "adopt_insight_clash",
            connection -> {
              registerDiego(connection);
              legacyInsights(connection);
              execute(
                  connection,
                  "INSERT INTO insight_history (user_id, week_start_date, planned_running_sessions,"
                      + " completed_running_sessions, planned_strength_sessions,"
                      + " completed_strength_sessions, generated_at) VALUES ('"
                      + DIEGO
                      + "', DATE '2026-07-20', 1, 1, 1, 1, CURRENT_TIMESTAMP)");
            });

    assertThat(
            column(
                db,
                "SELECT message FROM insight_history_recommendation WHERE user_id = '"
                    + PLACEHOLDER
                    + "'"))
        .containsExactly("Come más");
    assertThat(
            column(
                db,
                "SELECT message FROM insight_history_recommendation WHERE user_id = '"
                    + DIEGO
                    + "'"))
        .isEmpty();
  }

  /** Una semana de insights del marcador, con su recomendación. */
  private static void legacyInsights(Connection connection) throws SQLException {
    execute(
        connection,
        "INSERT INTO insight_history (user_id, week_start_date, planned_running_sessions,"
            + " completed_running_sessions, planned_strength_sessions,"
            + " completed_strength_sessions, generated_at) VALUES ('"
            + PLACEHOLDER
            + "', DATE '2026-07-20', 3, 2, 3, 3, CURRENT_TIMESTAMP)");
    execute(
        connection,
        "INSERT INTO insight_history_recommendation (user_id, week_start_date, sort_order,"
            + " is_main, category, severity, message, reason, created_at) VALUES ('"
            + PLACEHOLDER
            + "', DATE '2026-07-20', 0, TRUE, 'NUTRITION', 'INFO', 'Come más',"
            + " 'Porque sí', CURRENT_TIMESTAMP)");
  }

  private static void registerDiego(Connection connection) throws SQLException {
    execute(
        connection,
        "INSERT INTO users (id, email, password_hash) VALUES ('"
            + DIEGO
            + "', 'diego@forma.test', 'x')");
  }

  private static List<String> column(Connection connection, String sql) throws SQLException {
    List<String> values = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql)) {
      while (rs.next()) {
        values.add(rs.getString(1));
      }
    }
    return values;
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
