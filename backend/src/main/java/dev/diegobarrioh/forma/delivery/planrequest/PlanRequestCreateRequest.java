package dev.diegobarrioh.forma.delivery.planrequest;

import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.PlanDirection;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.util.Set;

/**
 * Request body for {@code POST /api/v1/plan-requests} (ADR-015 slice 2, {@code
 * docs/FORMA_Contrato_Agente_Plan.md}).
 *
 * <p>Only the wizard's own answers travel here. Everything {@link
 * dev.diegobarrioh.forma.domain.EnergyRequirement#of} needs — sex, birth date, height, activity
 * level, the resolved goal, weight — is read server-side from the caller's profile and newest body
 * measurement (ADR-015 decision 7); accepting them in this body would let a client freeze a number
 * that was never actually measured.
 *
 * <p>Bounds mirror {@code plan_request}'s own CHECK constraints (migration V63) so a caller gets a
 * {@code VALIDATION_ERROR} at the boundary instead of a constraint violation surfacing as a 500.
 *
 * @param direction lose fat, gain muscle, or hold steady — resolves {@code plan_objective}, never
 *     {@code main_goal} (ADR-015 decision 5); required
 * @param trainingDaysPerWeek 0-7; the wizard's training-days step is optional, unlike the public
 *     funnel's
 * @param trainingWeekdays {@code null} means nobody said; an empty set is a different, deliberate
 *     answer (ADR-015 decision 6); optional
 * @param equipment {@code null} means nobody said; carried unused by this slice (ADR-015 decision
 *     11); optional
 * @param mealsPerDay 3-6; required
 * @param dietPattern the exclusion rule; {@code UNSPECIFIED} is itself a valid, stored answer;
 *     required
 * @param cuisineStyle the cuisine; {@code UNSPECIFIED} is itself a valid, stored answer; required
 */
public record PlanRequestCreateRequest(
    @NotNull PlanDirection direction,
    @NotNull @Min(0) @Max(7) Integer trainingDaysPerWeek,
    Set<DayOfWeek> trainingWeekdays,
    Set<TrainingEquipment> equipment,
    @NotNull @Min(3) @Max(6) Integer mealsPerDay,
    @NotNull DietPattern dietPattern,
    @NotNull CuisineStyle cuisineStyle) {}
