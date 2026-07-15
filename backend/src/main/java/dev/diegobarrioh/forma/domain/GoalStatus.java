package dev.diegobarrioh.forma.domain;

/**
 * A goal's lifecycle status (FOR-125).
 *
 * <p>Resolves spec FOR-125's open question ("include a minimal status now, or defer?") by shipping
 * the smallest meaningful set. Status is always user-set (via create/PATCH), never auto-derived
 * from progress — auto-completion/auto-archival would require picking and testing a derivation
 * rule, which is speculative beyond this slice's scope.
 */
public enum GoalStatus {
  /** The default status; the goal is being actively tracked. */
  ACTIVE,
  /** The user marked the goal as achieved. */
  ACHIEVED,
  /** The user archived the goal without achieving it. */
  ARCHIVED
}
