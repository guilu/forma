package dev.diegobarrioh.forma.delivery.body;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Request body for {@code POST /api/v1/body/measurements} (FOR-17).
 *
 * <p>A delivery-layer DTO, distinct from the FOR-15 domain type and the FOR-16 persistence row
 * (ADR-005: controllers never accept or return persistence/domain types directly). {@code source}
 * is intentionally absent — the server always records manual API entries as {@code MANUAL}.
 *
 * <p>Bounds match the FOR-15 domain contract so validation fails at the API boundary with a {@code
 * VALIDATION_ERROR} (per-field details) rather than surfacing as a domain exception. Wrapper types
 * ({@link Double}) are used so a missing field is caught by {@link NotNull} instead of defaulting
 * to {@code 0}.
 *
 * @param measuredAt when the measurement was taken (ISO-8601); required
 * @param weightKg body weight in kilograms; required, strictly positive
 * @param bodyFatPercentage body fat percentage; required, within {@code [0, 100]}
 * @param bmi body mass index; required, strictly positive
 * @param notes optional free-text note
 */
public record CreateBodyMeasurementRequest(
    @NotNull Instant measuredAt,
    @NotNull @Positive Double weightKg,
    @NotNull @DecimalMin("0") @DecimalMax("100") Double bodyFatPercentage,
    @NotNull @Positive Double bmi,
    String notes) {}
