package dev.diegobarrioh.forma.domain;

/**
 * The measurable dimension a {@link Goal} tracks (FOR-125).
 *
 * <p>Closed enum resolving spec FOR-125's open question ("exact initial metric enum set and each
 * metric's source mapping"): the initial set covers the body-composition values already derived by
 * {@link WeeklyBodySummary} (FOR-21) from {@code BodyMeasurement} history (FOR-15/FOR-100), so
 * progress derivation reuses that existing math instead of duplicating it. A metric added later
 * without a source mapping is out of scope for this slice (no speculative abstraction) — every
 * value defined here currently maps to {@link ProgressSource#BODY_MEASUREMENT}.
 */
public enum GoalMetric {
  /** Body fat percentage, from {@link WeeklyBodySummary#latestBodyFatPercentage()}. */
  BODY_FAT_PCT,
  /** Body weight in kilograms, from {@link WeeklyBodySummary#latestWeightKg()}. */
  WEIGHT_KG,
  /** Lean mass in kilograms, from {@link WeeklyBodySummary#latestLeanMassKg()}. */
  LEAN_MASS_KG;

  /** The read-model source this metric derives its progress from. */
  public ProgressSource source() {
    return ProgressSource.BODY_MEASUREMENT;
  }

  /**
   * Reads this metric's latest value out of an existing {@link WeeklyBodySummary}, reusing its
   * derivation rather than recomputing from raw measurements.
   *
   * @return the latest value, or {@code null} when the summary has none yet (never fabricated)
   */
  public Double valueFrom(WeeklyBodySummary summary) {
    return switch (this) {
      case BODY_FAT_PCT -> summary.latestBodyFatPercentage();
      case WEIGHT_KG -> summary.latestWeightKg();
      case LEAN_MASS_KG -> summary.latestLeanMassKg();
    };
  }
}
