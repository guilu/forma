package dev.diegobarrioh.forma.application;

/**
 * What {@link PlanToleranceAuditor} decided about one {@link ToleranceEntry} (ADR-015 slice 4).
 *
 * <p>Three values, not two, because "the plan claimed nothing here" and "the plan claimed something
 * and it checked out" are different facts. Collapsing {@link #NOT_CLAIMED} into {@link
 * #WITHIN_TOLERANCE} would report a silent pass for a target nobody set — exactly the ambiguity
 * {@link dev.diegobarrioh.forma.domain.MacroTargets}'s own javadoc warns against for a target of
 * {@code null} versus a target of zero.
 */
public enum ToleranceVerdict {
  /** Claimed and calculated agree within {@link ToleranceThresholds}. */
  WITHIN_TOLERANCE,
  /**
   * Claimed and calculated disagree beyond {@link ToleranceThresholds}: the drift section 11 asks
   * to catch.
   */
  EXCEEDS_TOLERANCE,
  /**
   * Nothing was claimed for this metric at this scope, so there is nothing to compare — not a pass.
   */
  NOT_CLAIMED
}
