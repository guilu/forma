package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MainGoal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * What {@link PlanGenerationGateway#generate} hands back — the inbound half of {@code
 * docs/FORMA_Contrato_Agente_Plan.md} ("El plan"), parsed into FORMA's own application types rather
 * than any provider/JSON shape (ADR-004).
 *
 * <p><b>Deliberately the same shape {@code NutritionPlanRequest.toPlan} builds, minus what only the
 * caller can supply.</b> {@link #days} is a plain {@code List<PlanDay>} — the exact type {@link
 * NutritionPlan#days()} holds and {@link PlanToleranceAuditor#audit} already consumes — so slice 6
 * can construct a real {@link NutritionPlan} with one call: {@code new NutritionPlan(null, userId,
 * name(), description(), objective(), PlanStatus.DRAFT, startDate(), endDate(), targets(),
 * PlanGeneration_for_AI, days())}. This record carries no {@code userId}, no {@code id} and no
 * {@code status} on purpose: those are facts about the request and the ingest, not about what the
 * agent said, and {@link NutritionPlan}'s own constructor requires {@code userId} non-null — this
 * type cannot honestly claim one before slice 6 supplies it.
 *
 * <p>{@link #targets} carries the agent's CLAIMED figures exactly as sent, never corrected against
 * what {@link #days} actually add up to (ADR-015 decision 9: "the claimed targets are stored as
 * sent, never overwritten with the recomputed sum" — correcting them here would destroy the only
 * evidence a future {@link PlanToleranceAuditor#audit} run needs).
 *
 * @param contractVersion which shape of this message the agent spoke back
 * @param planRequestId the {@code plan_request} row this answers; the caller's own id, not
 *     necessarily re-read from the wire (see {@code PlanAgentAdapter#generate})
 * @param catalogVersion which catalog snapshot the agent says it used
 * @param name what to call the plan; required, non-blank
 * @param description free text
 * @param objective the profile's standing goal, in the plan JSON format's own vocabulary
 * @param startDate the day the plan begins; {@code null} for a template
 * @param endDate the day it ends; {@code null} when open-ended
 * @param targets what the agent claims the plan hits; never null, possibly unset
 * @param days the plan's days, exactly as {@link NutritionPlan#days()} would hold them
 * @param unmatched every catalog gap the agent hit while writing this plan; empty is normal and
 *     good
 * @param agentModel which model wrote this, as the agent identifies itself; may be {@code null}
 * @param agentGeneratedAt when the agent says it finished; may be {@code null}
 */
public record GeneratedNutritionPlan(
    String contractVersion,
    UUID planRequestId,
    String catalogVersion,
    String name,
    String description,
    MainGoal objective,
    LocalDate startDate,
    LocalDate endDate,
    PlanTargets targets,
    List<PlanDay> days,
    List<UnmatchedItem> unmatched,
    String agentModel,
    Instant agentGeneratedAt) {

  public GeneratedNutritionPlan {
    Objects.requireNonNull(planRequestId, "planRequestId must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    targets = targets == null ? PlanTargets.none() : targets;
    days = days == null ? List.of() : List.copyOf(days);
    unmatched = unmatched == null ? List.of() : List.copyOf(unmatched);
  }
}
