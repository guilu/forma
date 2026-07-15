package dev.diegobarrioh.forma.domain;

/**
 * Where a {@link GoalProgress} value was derived from (FOR-125). Only one source exists in this
 * slice — every current {@link GoalMetric} maps to body-composition data — but the type keeps the
 * API response explainable/auditable (spec FOR-125 Non-Functional Requirements) rather than
 * hardcoding a string.
 */
public enum ProgressSource {
  /** Derived from {@code BodyMeasurement} history via {@link WeeklyBodySummary} (FOR-21). */
  BODY_MEASUREMENT
}
