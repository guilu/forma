package dev.diegobarrioh.forma.domain;

/**
 * Type of a planned running session (FOR-22).
 *
 * <p>Closed classification used by {@link RunningPlanSession} (docs/domain-model.md). New types can
 * be added later without breaking the contract, as with {@link MeasurementSource}.
 *
 * <ul>
 *   <li>{@link #EASY} — relaxed aerobic run.
 *   <li>{@link #LONG_RUN} — the week's longest, progressive run.
 *   <li>{@link #INTERVALS} — quality/controlled effort session.
 *   <li>{@link #RECOVERY} — very light recovery run.
 * </ul>
 */
public enum SessionType {
  EASY,
  LONG_RUN,
  INTERVALS,
  RECOVERY
}
