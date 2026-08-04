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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * V55: a logged meal can say which planned meal it was.
 *
 * <p>What matters is what is NOT here — no second log table, no status column — and that deleting a
 * plan leaves the history of what was eaten standing.
 */
class MealLogPlannedMealMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:meal_log_planned;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  private static final String USER = "00000000-0000-0000-0000-000000000000";
  private static final String PLAN = "aaaaaaaa-1111-4000-8000-000000000001";
  private static final String DAY = "aaaaaaaa-1111-4000-8000-000000000002";
  private static final String MEAL = "aaaaaaaa-1111-4000-8000-000000000003";
  private static final String ENTRY = "aaaaaaaa-1111-4000-8000-000000000004";

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
    execute("DELETE FROM meal_log_entry WHERE id = '" + ENTRY + "'");
    execute("DELETE FROM nutrition_plan_meal WHERE id = '" + MEAL + "'");
    execute("DELETE FROM nutrition_plan_day WHERE id = '" + DAY + "'");
    execute("DELETE FROM nutrition_plan WHERE id = '" + PLAN + "'");
  }

  /** The document asks for a second pair of tables; meal_log_entry already is one. */
  @Test
  void addsNoSecondLogTable() {
    assertThatThrownBy(() -> column("SELECT id FROM nutrition_meal_logs"))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> column("SELECT id FROM nutrition_meal_log_items"))
        .isInstanceOf(SQLException.class);
  }

  /**
   * PENDING / COMPLETED / SKIPPED are all answerable from the rows; storing one would freeze it.
   */
  @Test
  void storesNoStatus() {
    assertThatThrownBy(() -> column("SELECT status FROM meal_log_entry"))
        .isInstanceOf(SQLException.class);
  }

  /** The plan is reachable through the meal's day; a second column for it could disagree. */
  @Test
  void storesNoPlanIdBesideTheMeal() {
    assertThatThrownBy(() -> column("SELECT plan_id FROM meal_log_entry"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void remembersWhichPlannedMealAnEntryWas() throws Exception {
    plannedMeal();
    entry(MEAL);

    assertThat(
            column("SELECT nutrition_plan_meal_id FROM meal_log_entry WHERE id = '" + ENTRY + "'"))
        .containsExactly(MEAL);
  }

  /** The ordinary case: somebody ate something and no plan said to. */
  @Test
  void letsAnEntryPointAtNothing() throws Exception {
    entry(null);

    assertThat(
            column("SELECT nutrition_plan_meal_id FROM meal_log_entry WHERE id = '" + ENTRY + "'"))
        .containsExactly((String) null);
  }

  @Test
  void refusesAPlannedMealThatDoesNotExist() {
    assertThatThrownBy(() -> entry("bbbbbbbb-1111-4000-8000-000000000009"))
        .isInstanceOf(SQLException.class);
  }

  /**
   * What was eaten survives the plan being thrown away.
   *
   * <p>A log entry is history and stays true forever; a plan is an intention and intentions get
   * deleted. The entry only stops being a planned one, which is what it has become.
   */
  @Test
  void keepsTheEntryWhenThePlanIsDeleted() throws Exception {
    plannedMeal();
    entry(MEAL);

    execute("DELETE FROM nutrition_plan_meal WHERE id = '" + MEAL + "'");

    assertThat(column("SELECT kcal FROM meal_log_entry WHERE id = '" + ENTRY + "'"))
        .containsExactly("222");
    assertThat(
            column("SELECT nutrition_plan_meal_id FROM meal_log_entry WHERE id = '" + ENTRY + "'"))
        .containsExactly((String) null);
  }

  private static void plannedMeal() throws SQLException {
    execute(
        "INSERT INTO nutrition_plan (id, user_id, name, status) VALUES ('"
            + PLAN
            + "', '"
            + USER
            + "', 'Para el test', 'DRAFT')");
    execute(
        "INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number)"
            + " VALUES ('"
            + DAY
            + "', '"
            + PLAN
            + "', 1, 1)");
    execute(
        "INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order)"
            + " VALUES ('"
            + MEAL
            + "', '"
            + DAY
            + "', 'BREAKFAST', 'Desayuno', 0)");
  }

  private static void entry(String plannedMealId) throws SQLException {
    execute(
        "INSERT INTO meal_log_entry"
            + " (id, user_id, log_date, meal_type, name, kcal, protein_g, carbs_g, fat_g,"
            + " nutrition_plan_meal_id)"
            + " VALUES ('"
            + ENTRY
            + "', '"
            + USER
            + "', DATE '2026-08-04', 'BREAKFAST', 'Copos de avena', 222, 7.8, 36.0, 4.2, "
            + (plannedMealId == null ? "NULL" : "'" + plannedMealId + "'")
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
