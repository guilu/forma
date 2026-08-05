package dev.diegobarrioh.forma.domain;

/**
 * Self-reported general activity level on the user's profile (FOR-107, spec FOR-58's Ajustes
 * mockup). A standard five-band scale, ordered from least to most active; future stories (nutrition
 * calorie targets) may use it as an input without changing this contract.
 */
public enum ActivityLevel {
  SEDENTARY(1.2),
  LIGHT(1.375),
  MODERATE(1.55),
  ACTIVE(1.725),
  VERY_ACTIVE(1.9);

  private final double factor;

  ActivityLevel(double factor) {
    this.factor = factor;
  }

  /**
   * What the basal requirement is multiplied by to get the daily one.
   *
   * <p>The classic Harris-Benedict multipliers, the ones every calculator shows. They live on the
   * enum rather than in whatever screen happens to need them, because the funnel shows the figure
   * and the plan generator uses it: two readers, one number.
   */
  public double factor() {
    return factor;
  }
}
