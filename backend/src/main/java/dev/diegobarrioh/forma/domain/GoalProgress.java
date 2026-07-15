package dev.diegobarrioh.forma.domain;

/**
 * A {@link Goal}'s derived progress (FOR-125). Never persisted — spec FOR-125 Data Model Notes:
 * "Progress is a read-model concern (derived), not a stored column" — always recomputed from a
 * fresh {@link WeeklyBodySummary} at read time.
 *
 * <p><b>Ratio formula (spec FOR-125 Open Questions, resolved):</b> the spec does not define an
 * exact ratio formula, and the {@code Goal} aggregate stores no starting/baseline value (only a
 * target), so a normalized "percent of the way there" cannot be computed without inventing a
 * baseline. This implementation uses the simplest honest reading of the two numbers actually
 * available: {@code ratio = current / target}, unclamped and direction-agnostic (it does not know
 * whether the goal means "reach at least this" or "get down to this" — that is a property of the
 * metric/UI, not of this value). A future slice could add a stored baseline for a true
 * "distance-closed" percentage.
 *
 * @param current the metric's latest known value, or {@code null} when there is no data yet (never
 *     fabricated as {@code 0})
 * @param target the goal's target, copied here so the progress payload is self-contained
 * @param ratio {@code current / target}, or {@code null} when {@code current} is unknown or {@code
 *     target} is {@code 0} (division by zero is never attempted)
 * @param source where {@code current} was (or would be) derived from
 */
public record GoalProgress(Double current, double target, Double ratio, ProgressSource source) {

  /**
   * Derives progress for {@code metric}/{@code target} from an existing {@link WeeklyBodySummary} —
   * reusing its already-derived values rather than recomputing from raw {@code BodyMeasurement}s
   * (spec FOR-125: "do NOT duplicate their math").
   */
  public static GoalProgress derive(GoalMetric metric, double target, WeeklyBodySummary summary) {
    Double current = metric.valueFrom(summary);
    Double ratio = (current == null || target == 0.0) ? null : current / target;
    return new GoalProgress(current, target, ratio, metric.source());
  }
}
