package dev.diegobarrioh.forma.domain;

/**
 * What somebody is asking a plan to do for them.
 *
 * <p>Distinct from {@link MainGoal}, and deliberately: that one is the profile's standing answer to
 * "what are you training for" (composición, rendimiento, hábito) and this is the clinical objective
 * of one plan. A plan for losing weight and a body-recomposition goal are not the same statement,
 * and collapsing them would make the funnel's four options into three that do not fit.
 *
 * <p>The adjustment each one applies to the daily requirement is EDITORIAL, not arithmetic: a
 * twenty per cent deficit is a widely used starting point, not a derivation. They are written here
 * so there is one place to argue about them, rather than a number buried in whichever screen shows
 * a total.
 */
public enum PlanObjective {
  /** Déficit calórico para reducir grasa corporal. */
  WEIGHT_LOSS(0.80),
  /** Superávit para ganar masa muscular. */
  MUSCLE_GAIN(1.10),
  /** Mantener peso y composición actual. */
  MAINTENANCE(1.0),
  /** Mejorar hábitos sin mover el peso. */
  HEALTHY_EATING(1.0);

  private final double factor;

  PlanObjective(double factor) {
    this.factor = factor;
  }

  /** What the daily requirement is multiplied by. */
  public double factor() {
    return factor;
  }
}
