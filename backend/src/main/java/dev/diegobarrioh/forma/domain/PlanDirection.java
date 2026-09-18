package dev.diegobarrioh.forma.domain;

/**
 * What direction a plan request asks for, in the wizard's own words: lose fat, gain muscle, or hold
 * steady.
 *
 * <p>Introduced by ADR-015 decision 5's amendment. The ADR's first draft proposed deriving this
 * from {@link MainGoal} ({@code COMPOSICION} defaulting to a 20% deficit, {@code RENDIMIENTO} and
 * {@code HABITO} to maintenance) — the product owner rejected that: somebody who picks
 * "composición" may mean gain muscle just as honestly as lose fat, and guessing somebody's calories
 * is exactly where guessing is unacceptable. This enum is the explicit question the wizard asks
 * instead, and it exists only because {@link PlanObjective} — the vocabulary that actually carries
 * the arithmetic — has a fourth value, {@link PlanObjective#HEALTHY_EATING}, that is not a
 * body-composition direction and has no honest answer to "lose, gain or hold". Asking {@link
 * PlanObjective} directly would let that value back in through a question that cannot mean it.
 *
 * <p>Each value maps 1:1 onto the {@link PlanObjective} that carries its {@link
 * PlanObjective#factor()}; this type carries no number of its own; see {@link #toPlanObjective()}.
 */
public enum PlanDirection {
  /** Déficit calórico para reducir grasa corporal. Maps to {@link PlanObjective#WEIGHT_LOSS}. */
  LOSE_FAT,
  /** Superávit para ganar masa muscular. Maps to {@link PlanObjective#MUSCLE_GAIN}. */
  GAIN_MUSCLE,
  /** Mantener peso y composición actual. Maps to {@link PlanObjective#MAINTENANCE}. */
  MAINTAIN;

  /**
   * The {@link PlanObjective} this direction resolves to — the value that actually crosses into
   * {@code plan_request.plan_objective} and into {@link EnergyRequirement#of}.
   */
  public PlanObjective toPlanObjective() {
    return switch (this) {
      case LOSE_FAT -> PlanObjective.WEIGHT_LOSS;
      case GAIN_MUSCLE -> PlanObjective.MUSCLE_GAIN;
      case MAINTAIN -> PlanObjective.MAINTENANCE;
    };
  }
}
