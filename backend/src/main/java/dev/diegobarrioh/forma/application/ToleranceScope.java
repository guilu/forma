package dev.diegobarrioh.forma.application;

/**
 * Which level of a plan a {@link ToleranceEntry} compares (ADR-015 slice 4, section 11): a single
 * day against its own {@code target_*}, or the whole plan's aggregate against {@code
 * nutrition_plan.target_*}. The two are checked independently — a plan can drift at the day level
 * while its week-long average happens to land inside tolerance, and section 11 asks for both.
 */
public enum ToleranceScope {
  /** The whole plan: every day's recomputed totals summed, against the plan-level band/targets. */
  PLAN,
  /** One day: its meals' recomputed totals, against that day's own claimed {@code target_*}. */
  DAY
}
