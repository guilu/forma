package dev.diegobarrioh.forma.domain;

/**
 * How strongly a {@link Recommendation} is expressed (FOR-41).
 *
 * <ul>
 *   <li>{@link #INFO} — a neutral observation, no action implied.
 *   <li>{@link #WARNING} — worth attention, still non-alarming (docs/ui-guidelines.md).
 *   <li>{@link #ACTION} — a concrete suggested adjustment.
 * </ul>
 */
public enum RecommendationSeverity {
  INFO,
  WARNING,
  ACTION
}
