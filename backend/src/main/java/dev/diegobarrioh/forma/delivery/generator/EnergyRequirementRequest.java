package dev.diegobarrioh.forma.delivery.generator;

import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.EnergyFormula;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.Sex;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * What the public funnel needs to work out a daily requirement.
 *
 * <p>Bounds on every figure, and they are not decoration: this is an unauthenticated endpoint, so
 * anybody can post anything at it. The limits are generous enough to describe any real person and
 * tight enough that nothing arrives claiming to be nine metres tall.
 *
 * @param objective may be absent while somebody is still on step 1: the requirement is then
 *     reported without an adjustment, which is exactly what that screen shows
 */
public record EnergyRequirementRequest(
    @NotNull Sex sex,
    @NotNull @Min(14) @Max(120) Integer ageYears,
    @NotNull @Positive @Max(400) Double weightKg,
    @NotNull @Positive @Max(260) Double heightCm,
    @NotNull ActivityLevel activityLevel,
    PlanObjective objective,
    EnergyFormula formula) {}
