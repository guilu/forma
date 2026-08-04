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
 * V53: nutrition plans as rows — plan, day, meal, item.
 *
 * <p>What matters is what is NOT here (no calculated_*, no snapshots, no calendar_date, no
 * quantity/unit/grams triple) and that the constraints refuse the states nobody should be able to
 * write: two active plans for one user, two meals in the same slot, an item that is both a food and
 * a dish, an item that is neither.
 *
 * <p>Plain Flyway + JDBC against its own H2 instance, isolated from the shared {@code
 * application-test.yml} database other {@code @SpringBootTest} classes mutate.
 */
class NutritionPlanMigrationTest {

  private static final String JDBC_URL =
      "jdbc:h2:mem:nutrition_plan_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

  /** The legacy single-user owner seeded by V26. */
  private static final String USER = "00000000-0000-0000-0000-000000000000";

  private static final String PLAN = "11111111-1111-1111-1111-111111111111";
  private static final String OTHER_PLAN = "22222222-2222-2222-2222-222222222222";
  private static final String DAY = "33333333-3333-3333-3333-333333333333";
  private static final String MEAL = "44444444-4444-4444-4444-444444444444";

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
    execute("DELETE FROM nutrition_plan_meal_item");
    execute("DELETE FROM nutrition_plan_meal");
    execute("DELETE FROM nutrition_plan_day");
    execute("DELETE FROM nutrition_plan");
    execute("DELETE FROM recipe");
  }

  /**
   * The three days that exist today are still constants in code; this slice only opens the door.
   */
  @Test
  void startsEmpty() throws Exception {
    assertThat(column("SELECT id FROM nutrition_plan")).isEmpty();
  }

  /**
   * A day's calculated totals are the sum of its items. Storing them would freeze an answer that
   * has to move when somebody corrects a food (ADR-011: computed on read, never stored).
   */
  @Test
  void storesNoCalculatedTotals() {
    assertThatThrownBy(() -> column("SELECT calculated_kcal FROM nutrition_plan_day"))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> column("SELECT calculated_protein_g FROM nutrition_plan_day"))
        .isInstanceOf(SQLException.class);
  }

  /**
   * A plan is an intention still in force, not history. The history that snapshots is
   * meal_log_entry.
   */
  @Test
  void storesNoMacroSnapshotsOnItems() {
    assertThatThrownBy(() -> column("SELECT kcal_snapshot FROM nutrition_plan_meal_item"))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> column("SELECT protein_g_snapshot FROM nutrition_plan_meal_item"))
        .isInstanceOf(SQLException.class);
  }

  /** The date follows from the plan's start_date plus the week and day numbers. */
  @Test
  void storesNoCalendarDateOnTheDay() {
    assertThatThrownBy(() -> column("SELECT calendar_date FROM nutrition_plan_day"))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> column("SELECT day_of_week FROM nutrition_plan_day"))
        .isInstanceOf(SQLException.class);
  }

  /** One amount, whose unit the row's own columns already name. */
  @Test
  void storesNoQuantityUnitGramsTriple() {
    assertThatThrownBy(() -> column("SELECT unit FROM nutrition_plan_meal_item"))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> column("SELECT grams FROM nutrition_plan_meal_item"))
        .isInstanceOf(SQLException.class);
  }

  /** V51 records preparation on the food, and the item's macros come from that food. */
  @Test
  void storesNoWeightStateOnTheItem() {
    assertThatThrownBy(() -> column("SELECT weight_state FROM nutrition_plan_meal_item"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void holdsAPlanWithItsDaysMealsAndItems() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);
    meal(MEAL, DAY, "BREAKFAST", "Desayuno", 1);
    item(MEAL, "oats", null, null, "60.0", 1);
    item(MEAL, "banana", null, null, "120.0", 2);

    assertThat(
            column(
                "SELECT food_id FROM nutrition_plan_meal_item WHERE nutrition_plan_meal_id = '"
                    + MEAL
                    + "' ORDER BY sort_order"))
        .containsExactly("oats", "banana");
  }

  /** Two plans a user could both be following is a question with two answers (ADR-011). */
  @Test
  void refusesTwoActivePlansForOneUser() throws Exception {
    plan(PLAN, "Uno", "ACTIVE", "1");

    assertThatThrownBy(() -> plan(OTHER_PLAN, "Dos", "ACTIVE", "1"))
        .isInstanceOf(SQLException.class);
  }

  /** Any number of plans may sit inactive beside the one being followed. */
  @Test
  void letsManyInactivePlansCoexist() throws Exception {
    plan(PLAN, "Activo", "ACTIVE", "1");

    assertThatCode(
            () -> {
              plan(OTHER_PLAN, "Borrador", "DRAFT", null);
              plan("55555555-5555-5555-5555-555555555555", "Viejo", "ARCHIVED", null);
            })
        .doesNotThrowAnyException();
  }

  /** The marker and the status are one fact; letting them disagree is how they end up lying. */
  @Test
  void refusesAMarkerThatContradictsTheStatus() {
    assertThatThrownBy(() -> plan(PLAN, "Mentiroso", "DRAFT", "1"))
        .isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> plan(PLAN, "Mudo", "ACTIVE", null)).isInstanceOf(SQLException.class);
  }

  @Test
  void refusesAnUnknownStatus() {
    assertThatThrownBy(() -> plan(PLAN, "Raro", "PAUSADO", null)).isInstanceOf(SQLException.class);
  }

  /** A plan that ends before it starts makes every date derived from it nonsense. */
  @Test
  void refusesAPlanThatEndsBeforeItStarts() throws Exception {
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO nutrition_plan (id, user_id, name, status, start_date, end_date)"
                        + " VALUES ('"
                        + PLAN
                        + "', '"
                        + USER
                        + "', 'Al revés', 'DRAFT', DATE '2026-08-10', DATE '2026-08-03')"))
        .isInstanceOf(SQLException.class);
  }

  /** Two rows claiming week 1 monday is two plans for the same morning. */
  @Test
  void refusesTwoDaysInTheSameSlot() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);

    assertThatThrownBy(() -> day("66666666-6666-6666-6666-666666666666", PLAN, 1, 1))
        .isInstanceOf(SQLException.class);
  }

  /** The same weekday in a later week is a different day, which is the point of week_number. */
  @Test
  void letsTheSameWeekdayRepeatInAnotherWeek() throws Exception {
    plan(PLAN, "Cuatro semanas", "DRAFT", null);
    day(DAY, PLAN, 1, 1);

    assertThatCode(() -> day("66666666-6666-6666-6666-666666666666", PLAN, 2, 1))
        .doesNotThrowAnyException();
  }

  @Test
  void refusesADayOutsideTheWeek() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);

    assertThatThrownBy(() -> day(DAY, PLAN, 1, 8)).isInstanceOf(SQLException.class);
    assertThatThrownBy(() -> day(DAY, PLAN, 0, 1)).isInstanceOf(SQLException.class);
  }

  /** An item is a food or a dish. Both is two things in one row; neither is nothing. */
  @Test
  void refusesAnItemThatIsBothAFoodAndARecipe() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);
    meal(MEAL, DAY, "LUNCH", "Comida", 1);
    execute("INSERT INTO recipe (id, name) VALUES ('guiso', 'Guiso')");

    assertThatThrownBy(() -> item(MEAL, "oats", "guiso", null, "1.0", 1))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void refusesAnItemThatIsNeither() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);
    meal(MEAL, DAY, "LUNCH", "Comida", 1);

    assertThatThrownBy(() -> item(MEAL, null, null, null, "1.0", 1))
        .isInstanceOf(SQLException.class);
  }

  /** A dish has servings, not portions of a food, so a serving on a recipe row means nothing. */
  @Test
  void refusesAServingOnARecipeRow() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);
    meal(MEAL, DAY, "LUNCH", "Comida", 1);
    execute("INSERT INTO recipe (id, name) VALUES ('guiso', 'Guiso')");

    assertThatThrownBy(() -> item(MEAL, null, "guiso", "oats", "1.0", 1))
        .isInstanceOf(SQLException.class);
  }

  /** An amount counts something; nothing is not an amount of it. */
  @Test
  void refusesAnItemOfNothing() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);
    meal(MEAL, DAY, "BREAKFAST", "Desayuno", 1);

    assertThatThrownBy(() -> item(MEAL, "oats", null, null, "0", 1))
        .isInstanceOf(SQLException.class);
  }

  /** An item counting portions points at the portion it counts (V49). */
  @Test
  void holdsAnItemMeasuredInPortions() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);
    meal(MEAL, DAY, "BREAKFAST", "Desayuno", 1);
    item(MEAL, "banana", null, "banana", "1.0", 1);

    assertThat(column("SELECT serving_id FROM nutrition_plan_meal_item")).containsExactly("banana");
  }

  /** A food used by a plan cannot vanish from under it. */
  @Test
  void refusesToDeleteAFoodSomePlanUses() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);
    meal(MEAL, DAY, "BREAKFAST", "Desayuno", 1);
    item(MEAL, "oats", null, null, "60.0", 1);

    assertThatThrownBy(() -> execute("DELETE FROM food_catalog WHERE id = 'oats'"))
        .isInstanceOf(SQLException.class);
  }

  /** Two meals at the same position in a day leaves the order of the day undecided. */
  @Test
  void refusesTwoMealsInTheSameSlot() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);
    meal(MEAL, DAY, "BREAKFAST", "Desayuno", 1);

    assertThatThrownBy(
            () -> meal("77777777-7777-7777-7777-777777777777", DAY, "LUNCH", "Comida", 1))
        .isInstanceOf(SQLException.class);
  }

  /**
   * Skippable is a property of the meal, not a rule about its type written into the delivery layer.
   */
  @Test
  void remembersThatAMealIsSkippable() throws Exception {
    plan(PLAN, "Semana base", "DRAFT", null);
    day(DAY, PLAN, 1, 1);
    execute(
        "INSERT INTO nutrition_plan_meal"
            + " (id, nutrition_plan_day_id, meal_type, name, sort_order, optional) VALUES ('"
            + MEAL
            + "', '"
            + DAY
            + "', 'POST_WORKOUT', 'Recuperación', 4, TRUE)");

    assertThat(column("SELECT optional FROM nutrition_plan_meal WHERE id = '" + MEAL + "'"))
        .containsExactly("TRUE");
  }

  private static void plan(String id, String name, String status, String marker)
      throws SQLException {
    execute(
        "INSERT INTO nutrition_plan (id, user_id, name, status, active_marker) VALUES ('"
            + id
            + "', '"
            + USER
            + "', '"
            + name
            + "', '"
            + status
            + "', "
            + (marker == null ? "NULL" : "'" + marker + "'")
            + ")");
  }

  private static void day(String id, String planId, int week, int dayNumber) throws SQLException {
    execute(
        "INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number) VALUES ('"
            + id
            + "', '"
            + planId
            + "', "
            + week
            + ", "
            + dayNumber
            + ")");
  }

  private static void meal(String id, String dayId, String type, String name, int order)
      throws SQLException {
    execute(
        "INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order)"
            + " VALUES ('"
            + id
            + "', '"
            + dayId
            + "', '"
            + type
            + "', '"
            + name
            + "', "
            + order
            + ")");
  }

  private static void item(
      String mealId, String foodId, String recipeId, String servingId, String amount, int order)
      throws SQLException {
    execute(
        "INSERT INTO nutrition_plan_meal_item"
            + " (id, nutrition_plan_meal_id, food_id, recipe_id, serving_id, amount, sort_order)"
            + " VALUES (RANDOM_UUID(), '"
            + mealId
            + "', "
            + quoted(foodId)
            + ", "
            + quoted(recipeId)
            + ", "
            + quoted(servingId)
            + ", "
            + amount
            + ", "
            + order
            + ")");
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
