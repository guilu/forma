package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.StoredWaterIntakeEntry;
import dev.diegobarrioh.forma.domain.WaterIntakeEntry;
import java.time.LocalDate;

/**
 * Response body for {@code POST /api/v1/nutrition/hydration} (FOR-130 api.md).
 *
 * <p>Delivery read model, distinct from the application/domain {@link WaterIntakeEntry} (ADR-005).
 * Never includes internal fields the UI doesn't need, and never logs volumes (AGENTS.md — personal
 * health data).
 */
public record WaterIntakeResponse(String id, LocalDate date, double volumeMl) {

  public static WaterIntakeResponse from(StoredWaterIntakeEntry stored) {
    return new WaterIntakeResponse(stored.id(), stored.entry().date(), stored.entry().volumeMl());
  }
}
