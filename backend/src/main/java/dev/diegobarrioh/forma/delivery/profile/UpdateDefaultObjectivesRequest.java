package dev.diegobarrioh.forma.delivery.profile;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request body for {@code PATCH /api/v1/profile/objectives} (FOR-107): default caloric deficit,
 * protein target and daily water target.
 *
 * <p>Every field is optional and partial (same merge-not-clobber contract as {@link
 * UpdateProfileFieldsRequest}).
 *
 * @param caloricDeficitKcal default caloric deficit target in kcal/day; non-negative when present
 * @param proteinTargetG default protein target in grams/day; non-negative when present
 * @param dailyWaterMl default daily water target in milliliters; non-negative when present
 */
public record UpdateDefaultObjectivesRequest(
    @PositiveOrZero Double caloricDeficitKcal,
    @PositiveOrZero Double proteinTargetG,
    @PositiveOrZero Double dailyWaterMl) {}
