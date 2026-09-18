package dev.diegobarrioh.forma.delivery.planrequest;

import dev.diegobarrioh.forma.application.PlanRequest;
import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.PlanRequestStatus;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Response body for {@code POST /api/v1/plan-requests} and {@code GET
 * /api/v1/plan-requests/current} (ADR-015 slice 2).
 *
 * <p>A read model, not {@link PlanRequest} itself (ADR-005: controllers never return persistence/
 * domain types directly). Deliberately narrower than the full row: {@code requestPayload}, {@code
 * validationReport}, {@code nutritionPlanId}, {@code failureCode} and {@code failureDetail} are
 * always {@code null} for a request this endpoint can return — an open (PENDING/GENERATING) request
 * never has them (ADR-015 decision 2/9) — so exposing them here would be noise, not information.
 */
public record PlanRequestResponse(
    UUID id,
    PlanRequestStatus status,
    MainGoal mainGoal,
    PlanObjective planObjective,
    int planKcal,
    int trainingDaysPerWeek,
    Set<DayOfWeek> trainingWeekdays,
    Set<TrainingEquipment> equipment,
    int mealsPerDay,
    DietPattern dietPattern,
    CuisineStyle cuisineStyle,
    OffsetDateTime requestedAt) {

  public static PlanRequestResponse from(PlanRequest request) {
    return new PlanRequestResponse(
        request.id(),
        request.status(),
        request.mainGoal(),
        request.planObjective(),
        request.planKcal(),
        request.trainingDaysPerWeek(),
        request.trainingWeekdays(),
        request.equipment(),
        request.mealsPerDay(),
        request.dietPattern(),
        request.cuisineStyle(),
        request.requestedAt());
  }
}
