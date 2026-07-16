package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.HydrationProgress;
import java.time.LocalDate;

/**
 * Response body for {@code GET /api/v1/nutrition/hydration?date=} (FOR-130 api.md): total logged
 * volume vs the resolved daily goal, with progress.
 *
 * <p>Delivery read model, distinct from the application {@link HydrationProgress} view (ADR-005).
 * {@code goalMl}/{@code progress} are explicit JSON {@code null} (never omitted) only in the
 * documented fail-safe case where the goal cannot be determined (spec FOR-130: "progress is null
 * (not fabricated)") — not reachable today since {@link
 * dev.diegobarrioh.forma.application.HydrationService} always applies a fallback goal, matching the
 * FOR-127/128 {@code DayConsumptionResponse} fail-safe-null precedent. {@code progress} is
 * uncapped/raw (documented decision, spec FOR-130 api.md).
 */
public record HydrationProgressResponse(
    LocalDate date, double totalMl, Double goalMl, Double progress) {

  public static HydrationProgressResponse from(HydrationProgress view) {
    return new HydrationProgressResponse(
        view.date(), view.totalMl(), view.goalMl(), view.progress());
  }
}
