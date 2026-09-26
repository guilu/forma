package dev.diegobarrioh.forma.delivery.training;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request body for {@code PUT /api/v1/training/sessions/{sessionId}/sets/{exerciseId}/{setNumber}}
 * (training-progression-and-logging slice B, design D7).
 *
 * <p>{@code weightKg} and {@code reps} are each independently optional (spec "Registro parcial": a
 * user who logs only the weight must not have the write rejected for missing reps) but must be
 * {@code >= 0} when present — validated at the boundary so a negative value yields {@code
 * VALIDATION_ERROR} with a per-field detail rather than the domain's {@code
 * IllegalArgumentException} leaking as a 500. {@code done} is a plain {@code boolean}, defaulting
 * to {@code false} when omitted.
 *
 * @param weightKg logged weight in kilograms, or {@code null} if not recorded; must be {@code >= 0}
 *     when present
 * @param reps logged repetitions, or {@code null} if not recorded; must be {@code >= 0} when
 *     present
 * @param done whether the set is marked complete
 */
public record LogSetRequest(
    @PositiveOrZero Double weightKg, @PositiveOrZero Integer reps, boolean done) {}
