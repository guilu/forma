package dev.diegobarrioh.forma.domain;

/**
 * Type of a nutrition day (FOR-29), per docs/domain-model.md.
 *
 * <p>Closed classification so nutrition adapts to the day instead of a rigid weekly diet. New types
 * can be added later without breaking the contract, as with {@link MeasurementSource}.
 *
 * <ul>
 *   <li>{@link #RUNNING} — a running day (more carbohydrates, placed earlier).
 *   <li>{@link #STRENGTH} — a strength day.
 *   <li>{@link #REST} — a rest day (slightly fewer carbohydrates).
 * </ul>
 */
public enum NutritionDayType {
  RUNNING,
  STRENGTH,
  REST
}
