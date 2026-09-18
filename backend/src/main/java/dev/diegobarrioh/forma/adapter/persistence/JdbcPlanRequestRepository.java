package dev.diegobarrioh.forma.adapter.persistence;

import dev.diegobarrioh.forma.application.PlanRequest;
import dev.diegobarrioh.forma.application.PlanRequestRepository;
import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.PlanRequestStatus;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter for {@code plan_request} (migration V63, ADR-015). Plain {@link JdbcTemplate}, no
 * ORM (ADR-003).
 *
 * <p>{@code training_weekdays} and {@code equipment} are stored as comma-separated enum names in
 * one {@code VARCHAR}, matching the {@code onboarding_equipment_items} TEXT-CSV precedent (V8) and
 * ADR-011's no-JSONB rule — the CSV encoding is a persistence detail and stays here, never leaking
 * into {@link PlanRequest}, which speaks {@link Set} of typed enums.
 *
 * <p>{@code open_marker} is never a constructor argument: like {@code JdbcNutritionPlanRepository}
 * does for {@code active_marker}, it is derived from {@code status.openMarker()} at write time and
 * never read back into the domain object — the column exists for the database's unique index, not
 * for the application to reason about.
 */
@Repository
public class JdbcPlanRequestRepository implements PlanRequestRepository {

  private static final String COLUMNS =
      "id, user_id, status, contract_version, catalog_version, sex, age_years, weight_kg,"
          + " height_cm, activity_level, main_goal, plan_objective, training_days_per_week,"
          + " training_weekdays, equipment, meals_per_day, diet_pattern, cuisine_style, plan_kcal,"
          + " target_protein_g, target_carbs_g, target_fat_g, request_payload, validation_report,"
          + " nutrition_plan_id, failure_code, failure_detail, attempt_count, requested_at,"
          + " dispatched_at, completed_at, updated_at";

  private final JdbcTemplate jdbcTemplate;

  public JdbcPlanRequestRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void insert(PlanRequest request) {
    jdbcTemplate.update(
        "INSERT INTO plan_request ("
            + COLUMNS
            + ", open_marker) VALUES ("
            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
            + " ?, ?, ?, ?, ?)",
        request.id(),
        request.userId(),
        request.status().name(),
        request.contractVersion(),
        request.catalogVersion(),
        request.sex().name(),
        request.ageYears(),
        request.weightKg(),
        request.heightCm(),
        request.activityLevel().name(),
        request.mainGoal().name(),
        request.planObjective().name(),
        request.trainingDaysPerWeek(),
        csvOfDays(request.trainingWeekdays()),
        csvOfEquipment(request.equipment()),
        request.mealsPerDay(),
        request.dietPattern().name(),
        request.cuisineStyle().name(),
        request.planKcal(),
        request.targetProteinG(),
        request.targetCarbsG(),
        request.targetFatG(),
        request.requestPayload(),
        request.validationReport(),
        request.nutritionPlanId(),
        request.failureCode(),
        request.failureDetail(),
        request.attemptCount(),
        request.requestedAt(),
        request.dispatchedAt(),
        request.completedAt(),
        request.updatedAt(),
        request.status().openMarker());
  }

  @Override
  public Optional<PlanRequest> findById(UUID id) {
    return jdbcTemplate
        .query(
            "SELECT " + COLUMNS + " FROM plan_request WHERE id = ?",
            JdbcPlanRequestRepository::mapRow,
            id)
        .stream()
        .findFirst();
  }

  private static PlanRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new PlanRequest(
        (UUID) rs.getObject("id"),
        (UUID) rs.getObject("user_id"),
        PlanRequestStatus.valueOf(rs.getString("status")),
        rs.getString("contract_version"),
        rs.getString("catalog_version"),
        Sex.valueOf(rs.getString("sex")),
        rs.getInt("age_years"),
        rs.getDouble("weight_kg"),
        rs.getDouble("height_cm"),
        ActivityLevel.valueOf(rs.getString("activity_level")),
        MainGoal.valueOf(rs.getString("main_goal")),
        PlanObjective.valueOf(rs.getString("plan_objective")),
        rs.getInt("training_days_per_week"),
        daysFromCsv(rs.getString("training_weekdays")),
        equipmentFromCsv(rs.getString("equipment")),
        rs.getInt("meals_per_day"),
        DietPattern.valueOf(rs.getString("diet_pattern")),
        CuisineStyle.valueOf(rs.getString("cuisine_style")),
        rs.getInt("plan_kcal"),
        nullableDouble(rs, "target_protein_g"),
        nullableDouble(rs, "target_carbs_g"),
        nullableDouble(rs, "target_fat_g"),
        rs.getString("request_payload"),
        rs.getString("validation_report"),
        (UUID) rs.getObject("nutrition_plan_id"),
        rs.getString("failure_code"),
        rs.getString("failure_detail"),
        rs.getInt("attempt_count"),
        rs.getObject("requested_at", OffsetDateTime.class),
        rs.getObject("dispatched_at", OffsetDateTime.class),
        rs.getObject("completed_at", OffsetDateTime.class),
        rs.getObject("updated_at", OffsetDateTime.class));
  }

  private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
    double value = rs.getDouble(column);
    return rs.wasNull() ? null : value;
  }

  private static String csvOfDays(Set<DayOfWeek> days) {
    if (days == null) {
      return null;
    }
    return days.stream().map(Enum::name).collect(Collectors.joining(","));
  }

  private static Set<DayOfWeek> daysFromCsv(String csv) {
    if (csv == null) {
      return null;
    }
    if (csv.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(csv.split(","))
        .map(DayOfWeek::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static String csvOfEquipment(Set<TrainingEquipment> equipment) {
    if (equipment == null) {
      return null;
    }
    return equipment.stream().map(Enum::name).collect(Collectors.joining(","));
  }

  private static Set<TrainingEquipment> equipmentFromCsv(String csv) {
    if (csv == null) {
      return null;
    }
    if (csv.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(csv.split(","))
        .map(TrainingEquipment::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
