package dev.diegobarrioh.forma.application;

/**
 * What a whole plan was asked to hit (V53).
 *
 * <p>Calories are a band rather than a number because that is how the objective is actually stated
 * — "2200 a 2400" — and collapsing it to one figure would invent a precision nobody asked for. The
 * macros are single targets, which is how they are stated.
 *
 * <p>All nullable. A plan with no targets of its own is not a plan with targets of zero: it is one
 * that defers to the figures already on the user's profile (V20 {@code base_calories_kcal}, {@code
 * protein_target_g}, {@code carbs_target_g}, {@code fat_target_g}).
 */
public record PlanTargets(
    Integer kcalMin, Integer kcalMax, Double proteinG, Double carbsG, Double fatG) {

  private static final PlanTargets NONE = new PlanTargets(null, null, null, null, null);

  public PlanTargets {
    if (kcalMin != null && kcalMax != null && kcalMax < kcalMin) {
      throw new IllegalArgumentException(
          "kcalMax must not be below kcalMin, was: " + kcalMin + ".." + kcalMax);
    }
  }

  /** Targets nobody has set; the profile's own figures stand instead. */
  public static PlanTargets none() {
    return NONE;
  }
}
