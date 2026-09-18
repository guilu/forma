package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.PlanRequestStatus;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * A durable, structured record of what somebody asked a plan to do for them (ADR-015, migration
 * V63). One row per request — never overwritten, never resurrected; a retry is a new row (ADR-015
 * decision 2).
 *
 * <p>Every input below is what was resolved and frozen at request time, not what {@code
 * user_profile}/{@code body_measurement} say today (see the migration's header comment). This slice
 * carries the record, its port and a JDBC round-trip only — {@code PlanRequestService}, the capture
 * endpoint, the dispatcher and the ingest are ADR-015 slices 2 and 6, not built here.
 *
 * @param mainGoal the profile's standing goal (what an accepted plan later writes to {@code
 *     nutrition_plan.objective})
 * @param planObjective this plan's clinical objective — the one carrying {@link
 *     PlanObjective#factor()}, resolved from an explicit {@code
 *     dev.diegobarrioh.forma.domain.PlanDirection} answer, never derived from {@code mainGoal}
 *     (ADR-015 decision 5, amended)
 * @param trainingWeekdays {@code null} means nobody said (the wizard step is optional); an empty
 *     set is a different, deliberate answer
 * @param equipment {@code null} means nobody said; carried unused by this slice (ADR-015 decision
 *     11)
 * @param planKcal {@link dev.diegobarrioh.forma.domain.EnergyRequirement#planKcal()}, frozen once
 *     and never recomputed (ADR-015 decision 7) — audit, not a target
 * @param nutritionPlanId {@code null} until {@link PlanRequestStatus#READY}
 */
public record PlanRequest(
    UUID id,
    UUID userId,
    PlanRequestStatus status,
    String contractVersion,
    String catalogVersion,
    Sex sex,
    int ageYears,
    double weightKg,
    double heightCm,
    ActivityLevel activityLevel,
    MainGoal mainGoal,
    PlanObjective planObjective,
    int trainingDaysPerWeek,
    Set<DayOfWeek> trainingWeekdays,
    Set<TrainingEquipment> equipment,
    int mealsPerDay,
    DietPattern dietPattern,
    CuisineStyle cuisineStyle,
    int planKcal,
    Double targetProteinG,
    Double targetCarbsG,
    Double targetFatG,
    String requestPayload,
    String validationReport,
    UUID nutritionPlanId,
    String failureCode,
    String failureDetail,
    int attemptCount,
    OffsetDateTime requestedAt,
    OffsetDateTime dispatchedAt,
    OffsetDateTime completedAt,
    OffsetDateTime updatedAt) {}
