package dev.diegobarrioh.forma.delivery.training;

import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code PATCH /api/v1/training/sessions/{id}/schedule} (V60).
 *
 * <p>Delivery DTO. {@code day} is validated at the boundary so an invalid value yields {@code
 * VALIDATION_ERROR} with a per-field detail rather than a Jackson enum-parse failure surfacing as
 * 500 — the same treatment {@link UpdateSessionStatusRequest} gives {@code status}.
 *
 * <p>A {@code null} day is meaningful, not missing: it clears the override and puts the session
 * back on the day the weekly policy plans it for.
 *
 * @param day day of the current week to move the session to, or {@code null} to restore its planned
 *     day
 */
public record RescheduleSessionRequest(
    @Pattern(
            regexp = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY",
            message = "must be a day of the week in English upper case, or null")
        String day) {}
