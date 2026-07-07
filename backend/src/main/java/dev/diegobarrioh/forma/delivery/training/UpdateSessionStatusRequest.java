package dev.diegobarrioh.forma.delivery.training;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code PATCH /api/v1/training/sessions/{id}/status} (FOR-27).
 *
 * <p>Delivery DTO. {@code status} is required and must be one of the known values — validated at
 * the boundary so an invalid value yields {@code VALIDATION_ERROR} with a per-field detail (rather
 * than a Jackson enum-parse failure surfacing as 500). {@code notes} is optional.
 *
 * @param status new status; required, one of {@code PLANNED} | {@code COMPLETED} | {@code SKIPPED}
 * @param notes optional completion note
 */
public record UpdateSessionStatusRequest(
    @NotNull
        @Pattern(
            regexp = "PLANNED|COMPLETED|SKIPPED",
            message = "must be one of PLANNED, COMPLETED, SKIPPED")
        String status,
    String notes) {}
