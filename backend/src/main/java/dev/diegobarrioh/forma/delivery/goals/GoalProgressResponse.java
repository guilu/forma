package dev.diegobarrioh.forma.delivery.goals;

import dev.diegobarrioh.forma.domain.GoalProgress;

/**
 * Delivery read model for {@link GoalProgress} (FOR-125 api.md). {@code current}/{@code ratio} are
 * explicit JSON {@code null}s (not omitted, unlike {@code ApiError}'s {@code NON_NULL} convention)
 * when the metric has no data yet — spec FOR-125 api.md: "progress is null (or current: null,
 * ratio: null)"; this slice always returns the object with explicit nulls since every current
 * {@link dev.diegobarrioh.forma.domain.GoalMetric} maps to a real source.
 */
public record GoalProgressResponse(Double current, double target, Double ratio, String source) {

  public static GoalProgressResponse from(GoalProgress progress) {
    return new GoalProgressResponse(
        progress.current(), progress.target(), progress.ratio(), progress.source().name());
  }
}
