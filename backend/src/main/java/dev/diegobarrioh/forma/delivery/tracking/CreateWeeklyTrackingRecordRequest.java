package dev.diegobarrioh.forma.delivery.tracking;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Request body for {@code POST /api/v1/tracking/weekly} (FOR-155): create or upsert the week's
 * *Seguimiento* row.
 *
 * <p>A delivery-layer DTO, distinct from the FOR-155 domain type and persistence row (ADR-005).
 * Only {@code week} and {@code date} are required — every body/training/plan field is optional so a
 * partial weekly entry is valid (spec FOR-155 Edge Cases), matching {@link
 * dev.diegobarrioh.forma.domain.WeeklyTrackingRecord}'s own optionality. {@code fatMassKg}/{@code
 * leanMassKg} are intentionally absent: they are always derived server-side from {@code weightKg}
 * and {@code bodyFatPercentage} (see the domain type's javadoc), never accepted as input.
 *
 * @param week the Seguimiento "Semana" number; required, strictly positive
 * @param date the week's reference date; required
 * @param weightKg body weight in kilograms; optional, strictly positive when present
 * @param bodyFatPercentage body fat percentage; optional, within {@code [0, 100]}
 * @param bmi body mass index; optional, strictly positive when present
 * @param runningKm running distance for the week in kilometers; optional, non-negative when present
 * @param pace4kmMinPerKm the 4 km pace, formatted {@code mm:ss} (minutes:seconds per km); optional,
 *     must match {@code mm:ss} with seconds in {@code [0, 59]} when present. Validated here (bean
 *     validation) rather than only in the domain constructor so a malformed value fails at the API
 *     boundary with {@code VALIDATION_ERROR} (spec FOR-155 api.md: "malformed pace" → 400),
 *     mirroring {@code CreateBodyMeasurementRequest}'s bounds-match-domain-contract convention
 * @param recommendedKcal recommended daily kcal for the week; optional, non-negative when present
 *     (user-entered in this slice, see the domain type's javadoc)
 * @param comment optional free-text weekly note
 */
public record CreateWeeklyTrackingRecordRequest(
    @NotNull @Positive Integer week,
    @NotNull LocalDate date,
    @Positive Double weightKg,
    @DecimalMin("0") @DecimalMax("100") Double bodyFatPercentage,
    @Positive Double bmi,
    @PositiveOrZero Double runningKm,
    @Pattern(regexp = "^\\d{1,2}:[0-5]\\d$", message = "must be in mm:ss format, e.g. \"6:00\"")
        String pace4kmMinPerKm,
    @PositiveOrZero Double recommendedKcal,
    String comment) {}
