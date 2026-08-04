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
 * @param comparingStates whether the two foods agree about the kitchen (V51). False only when both
 *     states are known and differ — dry rice against boiled pasta is two questions wearing the same
 *     units. Reported, never enforced: the arithmetic is right either way and it is the meaning
 *     that is off, which is a thing to tell somebody rather than a thing to forbid
 */
public record ResolvedEquivalence(
    FoodEquivalence equivalence,
    EquivalentPortion portion,
    String targetName,
    boolean comparingStates) {

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
