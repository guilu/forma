package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.LogWaterIntakeCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Request body for {@code POST /api/v1/nutrition/hydration} (FOR-130 api.md).
 *
 * <p>Delivery DTO, distinct from the {@link dev.diegobarrioh.forma.domain.WaterIntakeEntry} domain
 * type (ADR-005), mirroring {@code LogMealRequest} (FOR-127). {@code @Positive} rejects a
 * zero/negative {@code volumeMl} at the bean-validation boundary; {@link
 * dev.diegobarrioh.forma.application.HydrationService} additionally validates the built command, so
 * calling the service directly (bypassing this DTO) is equally safe.
 *
 * @param date required, ISO-8601
 * @param volumeMl the logged volume in milliliters; required, must be strictly positive
 */
public record LogWaterIntakeRequest(@NotNull LocalDate date, @NotNull @Positive Double volumeMl) {

  /** Builds the application-layer command. */
  public LogWaterIntakeCommand toCommand() {
    return new LogWaterIntakeCommand(date, volumeMl);
  }
}
