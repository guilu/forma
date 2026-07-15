package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.StoredMealLogEntry;
import dev.diegobarrioh.forma.domain.MealLogEntry;
import java.time.LocalDate;

/**
 * Response body for {@code POST /api/v1/nutrition/log} (FOR-127 api.md).
 *
 * <p>Delivery read model, distinct from the application/domain {@link MealLogEntry} (ADR-005).
 */
public record MealLogResponse(
    String id,
    LocalDate date,
    String mealType,
    String name,
    int kcal,
    double proteinG,
    double carbsG,
    double fatG) {

  public static MealLogResponse from(StoredMealLogEntry stored) {
    MealLogEntry entry = stored.entry();
    return new MealLogResponse(
        stored.id(),
        entry.date(),
        entry.mealType().name(),
        entry.name(),
        entry.totals().calories(),
        entry.totals().proteinG(),
        entry.totals().carbsG(),
        entry.totals().fatG());
  }
}
