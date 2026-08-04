package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.NutritionPlan;
import dev.diegobarrioh.forma.application.NutritionPlanRepository;
import dev.diegobarrioh.forma.application.PlanDay;
import dev.diegobarrioh.forma.application.PlanGeneration;
import dev.diegobarrioh.forma.application.PlanItem;
import dev.diegobarrioh.forma.application.PlanMeal;
import dev.diegobarrioh.forma.application.PlanTargets;
import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import dev.diegobarrioh.forma.domain.PlanOrigin;
import dev.diegobarrioh.forma.domain.PlanStatus;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over the four V53 plan tables. Plain JDBC via {@link JdbcTemplate} (no ORM, like
 * FOR-16).
 *
 * <p>A plan is read in FOUR flat queries — plan, then all its days, then all their meals, then all
 * their items — and assembled in memory. Not a join, which would repeat the plan's own columns once
 * per item and leave the caller to fold them back; and emphatically not a query per level, which
 * for a four-week plan would be one query for the plan, twenty-eight for its days and a hundred-odd
 * for their meals.
 *
 * <p>Everything is scoped by {@code user_id} at the top. The children carry no user column — they
 * are reachable only through a plan, and duplicating the owner onto every item would be the same
 * fact in four places, free to disagree once somebody writes an UPDATE that forgets one.
 */
@Repository
public class JdbcNutritionPlanRepository implements NutritionPlanRepository {

  private static final String PLAN_COLUMNS =
      "id, user_id, name, description, objective, status, start_date, end_date,"
          + " target_kcal_min, target_kcal_max, target_protein_g, target_carbs_g, target_fat_g,"
          + " generated_by, generation_prompt, generation_metadata";

  private final JdbcTemplate jdbcTemplate;

  public JdbcNutritionPlanRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<NutritionPlan> findAllByUser(UUID userId) {
    return hydrate(
        jdbcTemplate.query(
            "SELECT "
                + PLAN_COLUMNS
                + " FROM nutrition_plan WHERE user_id = ?"
                + " ORDER BY created_at DESC",
            (rs, rowNum) -> header(rs),
            userId));
  }

  @Override
  public Optional<NutritionPlan> findById(UUID userId, UUID planId) {
    return hydrate(
            jdbcTemplate.query(
                "SELECT " + PLAN_COLUMNS + " FROM nutrition_plan WHERE user_id = ? AND id = ?",
                (rs, rowNum) -> header(rs),
                userId,
                planId))
        .stream()
        .findFirst();
  }

  @Override
  public Optional<NutritionPlan> findActive(UUID userId) {
    // Read by the marker rather than by status = 'ACTIVE'. They cannot disagree — V53's CHECK ties
    // them — but the marker is what the unique index is on, so this is the one that cannot ever
    // return two rows.
    return hydrate(
            jdbcTemplate.query(
                "SELECT "
                    + PLAN_COLUMNS
                    + " FROM nutrition_plan WHERE user_id = ? AND active_marker IS NOT NULL",
                (rs, rowNum) -> header(rs),
                userId))
        .stream()
        .findFirst();
  }

  @Override
  public NutritionPlan save(NutritionPlan plan) {
    UUID planId = plan.id() == null ? UUID.randomUUID() : plan.id();
    int updated =
        jdbcTemplate.update(
            "UPDATE nutrition_plan SET name = ?, description = ?, objective = ?,"
                + " start_date = ?, end_date = ?, target_kcal_min = ?, target_kcal_max = ?,"
                + " target_protein_g = ?, target_carbs_g = ?, target_fat_g = ?,"
                + " generated_by = ?, generation_prompt = ?, generation_metadata = ?,"
                + " updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?",
            plan.name(),
            plan.description(),
            name(plan.objective()),
            plan.startDate(),
            plan.endDate(),
            plan.targets().kcalMin(),
            plan.targets().kcalMax(),
            plan.targets().proteinG(),
            plan.targets().carbsG(),
            plan.targets().fatG(),
            plan.generation().by().name(),
            plan.generation().prompt(),
            plan.generation().metadata(),
            planId,
            plan.userId());
    if (updated == 0) {
      jdbcTemplate.update(
          "INSERT INTO nutrition_plan (id, user_id, name, description, objective, status,"
              + " active_marker, start_date, end_date, target_kcal_min, target_kcal_max,"
              + " target_protein_g, target_carbs_g, target_fat_g, generated_by,"
              + " generation_prompt, generation_metadata)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          planId,
          plan.userId(),
          plan.name(),
          plan.description(),
          name(plan.objective()),
          plan.status().name(),
          plan.status().marker(),
          plan.startDate(),
          plan.endDate(),
          plan.targets().kcalMin(),
          plan.targets().kcalMax(),
          plan.targets().proteinG(),
          plan.targets().carbsG(),
          plan.targets().fatG(),
          plan.generation().by().name(),
          plan.generation().prompt(),
          plan.generation().metadata());
    }
    // The structure is replaced whole rather than diffed. The caller states the complete plan, and
    // working out which of a hundred items moved would cost more queries than rewriting them.
    // Status is NOT touched here: it moves through changeStatus, which is the only place that has
    // to
    // reason about the one-active-plan rule.
    deleteStructure(planId);
    writeStructure(planId, plan.days());
    return findById(plan.userId(), planId).orElseThrow();
  }

  @Override
  public void changeStatus(UUID userId, UUID planId, PlanStatus status) {
    if (status == PlanStatus.ACTIVE) {
      // Whatever the user was following stops being followed first. Without this the unique index
      // would refuse the second row and the user would be told their own plan already exists.
      jdbcTemplate.update(
          "UPDATE nutrition_plan SET status = ?, active_marker = NULL,"
              + " updated_at = CURRENT_TIMESTAMP"
              + " WHERE user_id = ? AND active_marker IS NOT NULL AND id <> ?",
          PlanStatus.COMPLETED.name(),
          userId,
          planId);
    }
    jdbcTemplate.update(
        "UPDATE nutrition_plan SET status = ?, active_marker = ?, updated_at = CURRENT_TIMESTAMP"
            + " WHERE id = ? AND user_id = ?",
        status.name(),
        status.marker(),
        planId,
        userId);
  }

  @Override
  public void delete(UUID userId, UUID planId) {
    // Ownership is checked before anything is removed: the children have no user column, so a
    // delete that started at the items would already have destroyed somebody else's plan by the
    // time the final statement found the wrong owner.
    if (findById(userId, planId).isEmpty()) {
      return;
    }
    deleteStructure(planId);
    jdbcTemplate.update("DELETE FROM nutrition_plan WHERE id = ? AND user_id = ?", planId, userId);
  }

  @Override
  public boolean ownsPlannedMeal(UUID userId, UUID plannedMealId) {
    Integer found =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM nutrition_plan_meal m"
                + " JOIN nutrition_plan_day d ON m.nutrition_plan_day_id = d.id"
                + " JOIN nutrition_plan p ON d.nutrition_plan_id = p.id"
                + " WHERE m.id = ? AND p.user_id = ?",
            Integer.class,
            plannedMealId,
            userId);
    return found != null && found > 0;
  }

  private void deleteStructure(UUID planId) {
    jdbcTemplate.update(
        "DELETE FROM nutrition_plan_meal_item WHERE nutrition_plan_meal_id IN"
            + " (SELECT m.id FROM nutrition_plan_meal m JOIN nutrition_plan_day d"
            + " ON m.nutrition_plan_day_id = d.id WHERE d.nutrition_plan_id = ?)",
        planId);
    jdbcTemplate.update(
        "DELETE FROM nutrition_plan_meal WHERE nutrition_plan_day_id IN"
            + " (SELECT id FROM nutrition_plan_day WHERE nutrition_plan_id = ?)",
        planId);
    jdbcTemplate.update("DELETE FROM nutrition_plan_day WHERE nutrition_plan_id = ?", planId);
  }

  private void writeStructure(UUID planId, List<PlanDay> days) {
    for (PlanDay day : days) {
      UUID dayId = UUID.randomUUID();
      jdbcTemplate.update(
          "INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number,"
              + " day_type, target_kcal, target_protein_g, target_carbs_g, target_fat_g, notes)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          dayId,
          planId,
          day.weekNumber(),
          day.dayNumber(),
          name(day.dayType()),
          day.targets().calories(),
          day.targets().proteinG(),
          day.targets().carbsG(),
          day.targets().fatG(),
          day.notes());
      List<PlanMeal> meals = day.meals();
      for (int mealAt = 0; mealAt < meals.size(); mealAt++) {
        PlanMeal meal = meals.get(mealAt);
        UUID mealId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name,"
                + " sort_order, scheduled_time, target_kcal, target_protein_g, target_carbs_g,"
                + " target_fat_g, instructions, optional)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            mealId,
            dayId,
            meal.mealType().name(),
            meal.name(),
            mealAt,
            meal.scheduledTime() == null ? null : Time.valueOf(meal.scheduledTime()),
            meal.targets().calories(),
            meal.targets().proteinG(),
            meal.targets().carbsG(),
            meal.targets().fatG(),
            meal.instructions(),
            meal.optional());
        List<PlanItem> items = meal.items();
        for (int itemAt = 0; itemAt < items.size(); itemAt++) {
          PlanItem item = items.get(itemAt);
          jdbcTemplate.update(
              "INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id,"
                  + " recipe_id, serving_id, amount, sort_order, preparation_notes, optional)"
                  + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
              UUID.randomUUID(),
              mealId,
              item.foodId(),
              item.recipeId(),
              item.servingId(),
              item.amount(),
              itemAt,
              item.preparationNotes(),
              item.optional());
        }
      }
    }
  }

  /** Fills the days, meals and items of already-read plan headers, in three flat queries. */
  private List<NutritionPlan> hydrate(List<NutritionPlan> headers) {
    if (headers.isEmpty()) {
      return headers;
    }
    List<UUID> planIds = headers.stream().map(NutritionPlan::id).toList();
    String placeholders = String.join(", ", planIds.stream().map(id -> "?").toList());

    Map<UUID, List<PlanMeal>> mealsByDay = new LinkedHashMap<>();
    Map<UUID, List<PlanItem>> itemsByMeal = new LinkedHashMap<>();
    List<UUID> dayIds = new ArrayList<>();
    Map<UUID, List<DayRow>> dayRowsByPlan = new LinkedHashMap<>();

    jdbcTemplate
        .query(
            "SELECT d.id, d.nutrition_plan_id, d.week_number, d.day_number, d.day_type,"
                + " d.target_kcal, d.target_protein_g, d.target_carbs_g, d.target_fat_g, d.notes"
                + " FROM nutrition_plan_day d WHERE d.nutrition_plan_id IN ("
                + placeholders
                + ") ORDER BY d.week_number, d.day_number",
            (rs, rowNum) -> new DayRow(uuid(rs, "id"), uuid(rs, "nutrition_plan_id"), day(rs)),
            planIds.toArray())
        .forEach(
            row -> {
              dayIds.add(row.id());
              dayRowsByPlan.computeIfAbsent(row.planId(), key -> new ArrayList<>()).add(row);
            });

    if (!dayIds.isEmpty()) {
      String dayPlaceholders = String.join(", ", dayIds.stream().map(id -> "?").toList());
      List<UUID> mealIds = new ArrayList<>();
      Map<UUID, List<MealRow>> mealRows = new LinkedHashMap<>();
      jdbcTemplate
          .query(
              "SELECT m.id, m.nutrition_plan_day_id, m.meal_type, m.name, m.sort_order,"
                  + " m.scheduled_time, m.target_kcal, m.target_protein_g, m.target_carbs_g,"
                  + " m.target_fat_g, m.instructions, m.optional FROM nutrition_plan_meal m"
                  + " WHERE m.nutrition_plan_day_id IN ("
                  + dayPlaceholders
                  + ") ORDER BY m.sort_order",
              (rs, rowNum) -> new MealRow(uuid(rs, "id"), uuid(rs, "nutrition_plan_day_id"), rs),
              dayIds.toArray())
          .forEach(
              row -> {
                mealIds.add(row.id());
                mealRows.computeIfAbsent(row.dayId(), key -> new ArrayList<>()).add(row);
              });

      if (!mealIds.isEmpty()) {
        String mealPlaceholders = String.join(", ", mealIds.stream().map(id -> "?").toList());
        jdbcTemplate
            .query(
                "SELECT i.nutrition_plan_meal_id, i.id, i.food_id, i.recipe_id, i.serving_id,"
                    + " i.amount, i.sort_order, i.preparation_notes, i.optional"
                    + " FROM nutrition_plan_meal_item i WHERE i.nutrition_plan_meal_id IN ("
                    + mealPlaceholders
                    + ") ORDER BY i.sort_order",
                (rs, rowNum) -> Map.entry(uuid(rs, "nutrition_plan_meal_id"), item(rs)),
                mealIds.toArray())
            .forEach(
                entry ->
                    itemsByMeal
                        .computeIfAbsent(entry.getKey(), key -> new ArrayList<>())
                        .add(entry.getValue()));
      }

      mealRows.forEach(
          (dayId, rows) ->
              mealsByDay.put(
                  dayId,
                  rows.stream()
                      .map(row -> row.toMeal(itemsByMeal.getOrDefault(row.id(), List.of())))
                      .toList()));
    }

    return headers.stream()
        .map(
            plan ->
                withDays(
                    plan,
                    dayRowsByPlan.getOrDefault(plan.id(), List.of()).stream()
                        .map(
                            row ->
                                withMeals(row.day(), mealsByDay.getOrDefault(row.id(), List.of())))
                        .toList()))
        .toList();
  }

  private static NutritionPlan withDays(NutritionPlan plan, List<PlanDay> days) {
    return new NutritionPlan(
        plan.id(),
        plan.userId(),
        plan.name(),
        plan.description(),
        plan.objective(),
        plan.status(),
        plan.startDate(),
        plan.endDate(),
        plan.targets(),
        plan.generation(),
        days);
  }

  private static PlanDay withMeals(PlanDay day, List<PlanMeal> meals) {
    return new PlanDay(
        day.id(),
        day.weekNumber(),
        day.dayNumber(),
        day.dayType(),
        day.targets(),
        day.notes(),
        meals);
  }

  private record DayRow(UUID id, UUID planId, PlanDay day) {}

  /**
   * A meal read from the database before its items are known. The {@link ResultSet} is consumed in
   * the constructor rather than kept, because it is closed by the time the items arrive.
   */
  private record MealRow(
      UUID id,
      UUID dayId,
      MealType mealType,
      String name,
      LocalTime scheduledTime,
      MacroTargets targets,
      String instructions,
      boolean optional) {

    MealRow(UUID id, UUID dayId, ResultSet rs) throws SQLException {
      this(
          id,
          dayId,
          MealType.valueOf(rs.getString("meal_type")),
          rs.getString("name"),
          time(rs.getTime("scheduled_time")),
          // Qualified: this record's own `targets` accessor would otherwise shadow the reader.
          JdbcNutritionPlanRepository.targets(rs),
          rs.getString("instructions"),
          rs.getBoolean("optional"));
    }

    PlanMeal toMeal(List<PlanItem> items) {
      return new PlanMeal(
          id, mealType, name, scheduledTime, targets, instructions, optional, items);
    }
  }

  private static NutritionPlan header(ResultSet rs) throws SQLException {
    return new NutritionPlan(
        uuid(rs, "id"),
        uuid(rs, "user_id"),
        rs.getString("name"),
        rs.getString("description"),
        enumOrNull(MainGoal.class, rs.getString("objective")),
        PlanStatus.valueOf(rs.getString("status")),
        date(rs.getDate("start_date")),
        date(rs.getDate("end_date")),
        new PlanTargets(
            integer(rs, "target_kcal_min"),
            integer(rs, "target_kcal_max"),
            decimal(rs, "target_protein_g"),
            decimal(rs, "target_carbs_g"),
            decimal(rs, "target_fat_g")),
        new PlanGeneration(
            PlanOrigin.valueOf(rs.getString("generated_by")),
            rs.getString("generation_prompt"),
            rs.getString("generation_metadata")),
        List.of());
  }

  private static PlanDay day(ResultSet rs) throws SQLException {
    return new PlanDay(
        uuid(rs, "id"),
        rs.getInt("week_number"),
        rs.getInt("day_number"),
        enumOrNull(NutritionDayType.class, rs.getString("day_type")),
        targets(rs),
        rs.getString("notes"),
        List.of());
  }

  private static PlanItem item(ResultSet rs) throws SQLException {
    return new PlanItem(
        uuid(rs, "id"),
        rs.getString("food_id"),
        rs.getString("recipe_id"),
        rs.getString("serving_id"),
        rs.getDouble("amount"),
        rs.getString("preparation_notes"),
        rs.getBoolean("optional"));
  }

  private static MacroTargets targets(ResultSet rs) throws SQLException {
    return new MacroTargets(
        integer(rs, "target_kcal"),
        decimal(rs, "target_protein_g"),
        decimal(rs, "target_carbs_g"),
        decimal(rs, "target_fat_g"));
  }

  private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
    return value == null ? null : Enum.valueOf(type, value);
  }

  private static UUID uuid(ResultSet rs, String column) throws SQLException {
    Object value = rs.getObject(column);
    return value == null ? null : UUID.fromString(value.toString());
  }

  private static LocalDate date(Date value) {
    return value == null ? null : value.toLocalDate();
  }

  private static LocalTime time(Time value) {
    return value == null ? null : value.toLocalTime();
  }

  /**
   * {@code null} rather than 0 for an unset target: nobody aiming for nothing is not aiming for 0.
   */
  private static Integer integer(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  private static Double decimal(ResultSet rs, String column) throws SQLException {
    double value = rs.getDouble(column);
    return rs.wasNull() ? null : value;
  }

  private static String name(Enum<?> value) {
    return value == null ? null : value.name();
  }
}
