package dev.diegobarrioh.forma.domain;

/**
 * The three adherence categories tracked by the FOR-129 read model: planned vs completed activity
 * over a rolling window (spec FOR-129, second implementable slice of FOR-104). Closed set for the
 * MVP; new categories (streaks, achievements, ...) are later FOR-104 slices, not added here.
 */
public enum AdherenceCategory {
  TRAINING,
  NUTRITION,
  MEASUREMENTS
}
