package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.EquivalentPortion;

/**
 * A substitution together with the grams it works out to today (V47).
 *
 * <p>Two halves that are stored differently on purpose: {@link #equivalence} is the decision
 * somebody wrote down, {@link #portion} is arithmetic over the current catalog. Handing them over
 * separately would make every caller redo the calculation and eventually one of them would cache
 * it.
 *
 * @param targetName what the replacing food is called, so a screen need not look it up again
 */
public record ResolvedEquivalence(
    FoodEquivalence equivalence, EquivalentPortion portion, String targetName) {

  /**
   * Whether the collateral drift is worth mentioning, against the tolerance this substitution
   * states. False when it states none — that is nobody having decided, not a pass.
   */
  public boolean exceedsTolerance() {
    return portion.exceeds(
        equivalence.maxMacroDeviationPct() == null
            ? null
            : equivalence.maxMacroDeviationPct().doubleValue());
  }
}
