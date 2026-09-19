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
import java.time.LocalDate;
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
 * <p>{@code blockLengthWeeks}, {@code blockNumber} and {@code nextReviewDate} (migration V64,
 * ADR-015 decision 14) carry the programme shape the product owner decided: FORMA generates a
 * twelve-week programme as three four-week blocks, one {@code plan_request}/{@code nutrition_plan}
 * pair per block, not one row for the whole programme. Nothing beyond persisting them is built in
 * this slice — no dispatcher reads them and no regeneration writes {@code nextReviewDate} yet.
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
 * @param blockLengthWeeks the requested block's length in weeks — what {@code plan.weeks} means in
 *     the outbound contract (the block's length, never the programme's twelve); always {@code 4}
 *     today (ADR-015 decision 14)
 * @param blockNumber which of the programme's three four-week blocks this request is for, 1-3;
 *     always {@code 1} today — nothing yet decides a request is for block 2 or 3 (ADR-015 decision
 *     14)
 * @param nextReviewDate the next review's date, and nothing else — no alert, no measurement, no
 *     regeneration reads or writes this in this slice (ADR-015 decision 14); {@code null} until a
 *     later slice fixes the block's start date and can compute it
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
    int blockLengthWeeks,
    int blockNumber,
    LocalDate nextReviewDate,
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
