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
 * V63: {@code plan_request}, ADR-015 slice 1.
 *
 * <p>What matters here is that the constraints refuse the states nobody should be able to write: an
 * unknown status, an {@code open_marker} that disagrees with the status in either direction, a
 * second open request for one user, a {@code READY} row with no plan, and every numeric CHECK the
 * ADR table lists. Mirrors {@code NutritionPlanMigrationTest}'s style — plain Flyway + JDBC against
 * its own isolated H2 instance, one raw {@link SQLException} per rejected write.
 */
class PlanRequestMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:plan_request_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  /**
   * The legacy single-user owner seeded by V26 — same row {@code NutritionPlanMigrationTest} uses.
   */
  private static final String USER = "00000000-0000-0000-0000-000000000000";

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
    execute("DELETE FROM plan_request");
    execute("DELETE FROM users WHERE id <> '" + USER + "'");
  }

  @Test
  void holdsAFullRequest() throws Exception {
    request(uuid(), "PENDING", "1", null, 38, 73.6, 180.0, 5, 5, 2078);

    assertThat(column("SELECT status FROM plan_request")).containsExactly("PENDING");
  }

  @Test
  void refusesAnUnknownStatus() {
    assertThatThrownBy(() -> request(uuid(), "REQUESTED", "1", null, 38, 73.6, 180.0, 5, 5, 2078))
        .isInstanceOf(SQLException.class);
  }

  /**
   * V65: DELETED is now a legal terminal status, with no plan pointer and no open marker — exactly
   * what {@code NutritionPlanService#delete} writes once a READY request's plan is removed.
   */
  @Test
  void acceptsTheDeletedStatusWithNoPlanAndNoOpenMarker() throws Exception {
    request(uuid(), "DELETED", null, null, 38, 73.6, 180.0, 5, 5, 2078);

    assertThat(column("SELECT status FROM plan_request")).containsExactly("DELETED");
  }

  /**
   * DELETED with a plan pointer still set is not what the application ever writes (it clears
   * nutrition_plan_id in the same UPDATE as the status), but nothing in the schema forbids it —
   * chk_plan_request_ready_has_plan only constrains READY. This documents that DELETED is not
   * subject to the same "must point at a plan" rule READY is.
   */
  @Test
  void deletedDoesNotRequireANullPlanPointerAtTheSchemaLevel() throws Exception {
    java.util.UUID plan = java.util.UUID.randomUUID();
    execute(
        "INSERT INTO nutrition_plan (id, user_id, name, status, active_marker)"
            + " VALUES ('"
            + plan
            + "', '"
            + USER
            + "', 'Plan de prueba V65', 'DRAFT', NULL)");

    assertThatCode(
            () -> request(uuid(), "DELETED", null, plan.toString(), 38, 73.6, 180.0, 5, 5, 2078))
        .doesNotThrowAnyException();

    // plan_request is cleared by @AfterEach; the plan itself is this test's own to remove, and
    // must go after the row still pointing at it or the FK (ON DELETE RESTRICT) refuses it.
    execute("DELETE FROM plan_request WHERE nutrition_plan_id = '" + plan + "'");
    execute("DELETE FROM nutrition_plan WHERE id = '" + plan + "'");
  }

  /** The marker and the status are one fact; letting them disagree either way is how they lie. */
  @Test
  void refusesAMarkerThatContradictsTheStatus() {
    assertThatThrownBy(() -> request(uuid(), "PENDING", null, null, 38, 73.6, 180.0, 5, 5, 2078))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> request(uuid(), "FAILED", "1", null, 38, 73.6, 180.0, 5, 5, 2078))
        .isInstanceOf(SQLException.class);
  }

  /**
   * A READY row always points at a stored plan; the CHECK is what makes that a fact, not a hope.
   */
  @Test
  void refusesReadyWithNoNutritionPlan() {
    assertThatThrownBy(() -> request(uuid(), "READY", null, null, 38, 73.6, 180.0, 5, 5, 2078))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void letsManyClosedRequestsCoexist() throws Exception {
    request(uuid(), "FAILED", null, null, 38, 73.6, 180.0, 5, 5, 2078);
    request(uuid(), "FAILED", null, null, 40, 80.0, 175.0, 4, 3, 2200);

    assertThatCode(() -> request(uuid(), "PENDING", "1", null, 38, 73.6, 180.0, 5, 5, 2078))
        .doesNotThrowAnyException();
  }

  @Test
  void refusesASecondOpenRequestForTheSameUser() throws Exception {
    request(uuid(), "PENDING", "1", null, 38, 73.6, 180.0, 5, 5, 2078);

    assertThatThrownBy(() -> request(uuid(), "GENERATING", "1", null, 38, 73.6, 180.0, 5, 5, 2078))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesAgeOutsideTheAllowedRange() {
    assertThatThrownBy(() -> request(uuid(), "PENDING", "1", null, 13, 73.6, 180.0, 5, 5, 2078))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> request(uuid(), "PENDING", "1", null, 121, 73.6, 180.0, 5, 5, 2078))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesNonPositiveWeightOrHeight() {
    assertThatThrownBy(() -> request(uuid(), "PENDING", "1", null, 38, 0, 180.0, 5, 5, 2078))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> request(uuid(), "PENDING", "1", null, 38, 73.6, 0, 5, 5, 2078))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesMealsPerDayOutsideTheAllowedRange() {
    assertThatThrownBy(() -> request(uuid(), "PENDING", "1", null, 38, 73.6, 180.0, 2, 5, 2078))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> request(uuid(), "PENDING", "1", null, 38, 73.6, 180.0, 7, 5, 2078))
        .isInstanceOf(SQLException.class);
  }

  /** 0 is deliberately allowed here — the wizard's training-days step is optional. */
  @Test
  void allowsZeroTrainingDaysButRefusesMoreThanSeven() throws Exception {
    assertThatCode(() -> request(uuid(), "PENDING", "1", null, 38, 73.6, 180.0, 5, 0, 2078))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> request(uuid(), "PENDING", "1", null, 38, 73.6, 180.0, 5, 8, 2078))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesNonPositivePlanKcal() {
    assertThatThrownBy(() -> request(uuid(), "PENDING", "1", null, 38, 73.6, 180.0, 5, 5, 0))
        .isInstanceOf(SQLException.class);
  }

  /**
   * V64 (ADR-015 decision 14): block_length_weeks and block_number both default without being named
   * in the insert, and next_review_date defaults to NULL -- proving the migration's DEFAULTs apply
   * to a row written the same way every V63 test already writes one, with no back-fill needed.
   */
  @Test
  void defaultsTheProgrammeColumnsWhenNotNamedInTheInsert() throws Exception {
    String id = uuid();
    request(id, "PENDING", "1", null, 38, 73.6, 180.0, 5, 5, 2078);

    assertThat(column("SELECT block_length_weeks FROM plan_request WHERE id = '" + id + "'"))
        .containsExactly("4");
    assertThat(column("SELECT block_number FROM plan_request WHERE id = '" + id + "'"))
        .containsExactly("1");
    assertThat(column("SELECT next_review_date FROM plan_request WHERE id = '" + id + "'"))
        .containsExactly((String) null);
  }

  /**
   * The CHECK rules out the impossible, not the undecided: a block of zero weeks, or one longer
   * than the twelve-week programme it belongs to. That blocks are four weeks today lives in the
   * column's DEFAULT and in decision 14, not in a constraint that would need a migration to allow a
   * six-week block for one user.
   */
  @Test
  void refusesABlockLengthOutsideTheProgramme() {
    for (int weeks : new int[] {0, 13}) {
      int outOfRange = weeks;
      assertThatThrownBy(() -> execute(insertWithBlockLength(outOfRange)))
          .as("block_length_weeks = %d", outOfRange)
          .isInstanceOf(SQLException.class);
    }
  }

  /**
   * A block length other than the default is accepted: the decision is not welded into the schema.
   */
  @Test
  void acceptsABlockLengthInsideTheProgramme() throws Exception {
    String id = uuid();
    execute(insertWithBlockLength(6, id));

    assertThat(column("SELECT block_length_weeks FROM plan_request WHERE id = '" + id + "'"))
        .containsExactly("6");
  }

  private String insertWithBlockLength(int weeks) {
    return insertWithBlockLength(weeks, uuid());
  }

  private String insertWithBlockLength(int weeks, String id) {
    return "INSERT INTO plan_request (id, user_id, status, open_marker,"
        + " contract_version, sex, age_years, weight_kg, height_cm,"
        + " activity_level, main_goal, plan_objective, training_days_per_week,"
        + " meals_per_day, diet_pattern, cuisine_style, plan_kcal,"
        + " block_length_weeks)"
        + " VALUES ('"
        + id
        + "', '"
        + USER
        + "', 'PENDING', '1', '1', 'MALE', 38, 73.6, 180.0, 'MODERATE',"
        + " 'COMPOSICION', 'WEIGHT_LOSS', 5, 5, 'OMNIVORE', 'ESPANOLA', 2078, "
        + weeks
        + ")";
  }

  @Test
  void refusesABlockNumberOutsideOneToThree() {
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO plan_request (id, user_id, status, open_marker,"
                        + " contract_version, sex, age_years, weight_kg, height_cm,"
                        + " activity_level, main_goal, plan_objective, training_days_per_week,"
                        + " meals_per_day, diet_pattern, cuisine_style, plan_kcal, block_number)"
                        + " VALUES ('"
                        + uuid()
                        + "', '"
                        + USER
                        + "', 'PENDING', '1', '1', 'MALE', 38, 73.6, 180.0, 'MODERATE',"
                        + " 'COMPOSICION', 'WEIGHT_LOSS', 5, 5, 'OMNIVORE', 'ESPANOLA', 2078, 4)"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void acceptsAnExplicitNextReviewDate() throws Exception {
    String id = uuid();
    execute(
        "INSERT INTO plan_request (id, user_id, status, open_marker, contract_version, sex,"
            + " age_years, weight_kg, height_cm, activity_level, main_goal, plan_objective,"
            + " training_days_per_week, meals_per_day, diet_pattern, cuisine_style, plan_kcal,"
            + " next_review_date)"
            + " VALUES ('"
            + id
            + "', '"
            + USER
            + "', 'PENDING', '1', '1', 'MALE', 38, 73.6, 180.0, 'MODERATE', 'COMPOSICION',"
            + " 'WEIGHT_LOSS', 5, 5, 'OMNIVORE', 'ESPANOLA', 2078, DATE '2026-10-05')");

    assertThat(column("SELECT next_review_date FROM plan_request WHERE id = '" + id + "'"))
        .containsExactly("2026-10-05");
  }

  @Test
  void refusesAnUnknownUser() {
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO plan_request (id, user_id, status, open_marker,"
                        + " contract_version, sex, age_years, weight_kg, height_cm,"
                        + " activity_level, main_goal, plan_objective, training_days_per_week,"
                        + " meals_per_day, diet_pattern, cuisine_style, plan_kcal)"
                        + " VALUES ('"
                        + uuid()
                        + "', '99999999-9999-9999-9999-999999999999', 'PENDING', '1', '1',"
                        + " 'MALE', 38, 73.6, 180.0, 'MODERATE', 'COMPOSICION', 'WEIGHT_LOSS', 5,"
                        + " 5, 'OMNIVORE', 'ESPANOLA', 2078)"))
        .isInstanceOf(SQLException.class);
  }

  /**
   * No declared retention period of its own (unlike plan_lead): it lives and dies with the account.
   */
  @Test
  void cascadesWhenTheOwningUserIsDeleted() throws Exception {
    String throwawayUser = uuid();
    execute(
        "INSERT INTO users (id, email, password_hash) VALUES ('"
            + throwawayUser
            + "', 'throwaway@plan-request.test', '{argon2}x')");
    String requestId = uuid();
    execute(
        "INSERT INTO plan_request (id, user_id, status, open_marker, contract_version, sex,"
            + " age_years, weight_kg, height_cm, activity_level, main_goal, plan_objective,"
            + " training_days_per_week, meals_per_day, diet_pattern, cuisine_style, plan_kcal)"
            + " VALUES ('"
            + requestId
            + "', '"
            + throwawayUser
            + "', 'PENDING', '1', '1', 'MALE', 38, 73.6, 180.0, 'MODERATE', 'COMPOSICION',"
            + " 'WEIGHT_LOSS', 5, 5, 'OMNIVORE', 'ESPANOLA', 2078)");

    execute("DELETE FROM users WHERE id = '" + throwawayUser + "'");

    assertThat(column("SELECT id FROM plan_request WHERE id = '" + requestId + "'")).isEmpty();
  }

  private static void request(
      String id,
      String status,
      String marker,
      String nutritionPlanId,
      int ageYears,
      double weightKg,
      double heightCm,
      int mealsPerDay,
      int trainingDaysPerWeek,
      int planKcal)
      throws SQLException {
    execute(
        "INSERT INTO plan_request (id, user_id, status, open_marker, contract_version, sex,"
            + " age_years, weight_kg, height_cm, activity_level, main_goal, plan_objective,"
            + " training_days_per_week, meals_per_day, diet_pattern, cuisine_style, plan_kcal,"
            + " nutrition_plan_id)"
            + " VALUES ('"
            + id
            + "', '"
            + USER
            + "', '"
            + status
            + "', "
            + quoted(marker)
            + ", '1', 'MALE', "
            + ageYears
            + ", "
            + weightKg
            + ", "
            + heightCm
            + ", 'MODERATE', 'COMPOSICION', 'WEIGHT_LOSS', "
            + trainingDaysPerWeek
            + ", "
            + mealsPerDay
            + ", 'OMNIVORE', 'ESPANOLA', "
            + planKcal
            + ", "
            + quoted(nutritionPlanId)
            + ")");
  }

  private static String uuid() {
    return java.util.UUID.randomUUID().toString();
  }

  private static String quoted(String value) {
    return value == null ? "NULL" : "'" + value + "'";
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
