package dev.diegobarrioh.forma.domain;

/**
 * What a day or a meal was asked to hit (V53).
 *
 * <p>Every field is nullable, and that is the point: {@code null} means nobody set a target for
 * that macro, which is a different thing from a target of zero (FOR-134). A day with only {@link
 * #calories} set is a day where somebody cared about calories and left the rest open.
 *
 * <p>These are TARGETS, never totals. What a day actually adds up to is computed from its items and
 * is deliberately not stored anywhere (ADR-011). The two being separate facts is what lets a plan
 * say "you asked for 2320 and this comes to 2100".
 *
 * @param calories kcal target; null when unset
 * @param proteinG protein target in grams; null when unset
 * @param carbsG carbohydrate target in grams; null when unset
 * @param fatG fat target in grams; null when unset
 */
public record MacroTargets(Integer calories, Double proteinG, Double carbsG, Double fatG) {

  private static final MacroTargets NONE = new MacroTargets(null, null, null, null);

  /** Targets nobody has set. */
  public static MacroTargets none() {
    return NONE;
  }

  /** Whether nobody set any of the four. */
  public boolean unset() {
    return calories == null && proteinG == null && carbsG == null && fatG == null;
  }
}
