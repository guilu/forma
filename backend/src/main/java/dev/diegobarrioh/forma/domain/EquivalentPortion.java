package dev.diegobarrioh.forma.domain;

/**
 * How much of one food stands in for a portion of another, and what that costs elsewhere (V47).
 *
 * <p>Computed, never stored. The weight is the RESULT of an equivalence: a curator states which
 * nutrient is being matched and how big a portion to talk about, and the grams follow from the
 * catalog. Persisting them would freeze an answer that has to move when somebody corrects a food's
 * macros — and the catalog has already shown it can, holding rice as raw where a source document
 * assumed cooked, a difference of nearly fivefold.
 *
 * @param targetReferenceG how many grams of the target carry as much of the chosen nutrient as the
 *     stated portion of the source
 * @param caloriesDeviationPct how far energy drifts, as a percentage of the source portion's, or
 *     {@code null} when calories are what is being matched (the drift is zero by construction) or
 *     when both sides are zero
 * @param proteinDeviationPct the same for protein
 * @param carbsDeviationPct the same for carbohydrate
 * @param fatDeviationPct the same for fat
 */
public record EquivalentPortion(
    double targetReferenceG,
    Double caloriesDeviationPct,
    Double proteinDeviationPct,
    Double carbsDeviationPct,
    Double fatDeviationPct) {

  /**
   * The equivalent portion of {@code target} for {@code sourceReferenceG} grams of {@code source}.
   *
   * @throws IllegalArgumentException when the two foods are the same, when the portion is not
   *     positive, or when either food carries none of the nutrient being matched. That last one is
   *     two failures wearing one coat: a target with none of it cannot supply it at any weight (the
   *     arithmetic divides by zero), and a source with none of it asks for nothing — "0 g of rice
   *     replaces 100 g of olive oil" computes cleanly and means nothing
   */
  public static EquivalentPortion of(
      FoodItem source, FoodItem target, EquivalenceBasis basis, double sourceReferenceG) {
    if (source.id().equals(target.id())) {
      throw new IllegalArgumentException("a food cannot stand in for itself: " + source.id());
    }
    if (sourceReferenceG <= 0) {
      throw new IllegalArgumentException(
          "sourceReferenceG must be strictly positive, was: " + sourceReferenceG);
    }
    double sourcePer100 = basis.per100gOf(source);
    double targetPer100 = basis.per100gOf(target);
    if (sourcePer100 <= 0 || targetPer100 <= 0) {
      throw new IllegalArgumentException(
          "both foods must carry "
              + basis
              + ": "
              + source.id()
              + " has "
              + sourcePer100
              + ", "
              + target.id()
              + " has "
              + targetPer100);
    }
    double targetReferenceG = sourceReferenceG * sourcePer100 / targetPer100;
    return new EquivalentPortion(
        targetReferenceG,
        deviation(
            basis, EquivalenceBasis.CALORIES, source, target, sourceReferenceG, targetReferenceG),
        deviation(
            basis, EquivalenceBasis.PROTEIN, source, target, sourceReferenceG, targetReferenceG),
        deviation(
            basis, EquivalenceBasis.CARBS, source, target, sourceReferenceG, targetReferenceG),
        deviation(basis, EquivalenceBasis.FAT, source, target, sourceReferenceG, targetReferenceG));
  }

  /**
   * Whether any reported drift exceeds {@code maxPct}, in either direction.
   *
   * <p>Never a reason to refuse the equivalence — the swap was asked for on one nutrient and it
   * delivers that nutrient. It is a warning for whoever, or whatever, is choosing between
   * substitutions: rice for potato keeps the carbohydrate and moves the fat by half.
   *
   * @param maxPct the tolerance, or {@code null} for "nobody has said what too far means", which is
   *     not the same as a breach
   */
  public boolean exceeds(Double maxPct) {
    if (maxPct == null) {
      return false;
    }
    return over(caloriesDeviationPct, maxPct)
        || over(proteinDeviationPct, maxPct)
        || over(carbsDeviationPct, maxPct)
        || over(fatDeviationPct, maxPct);
  }

  private static boolean over(Double deviation, double maxPct) {
    return deviation != null && Math.abs(deviation) > maxPct;
  }

  private static Double deviation(
      EquivalenceBasis basis,
      EquivalenceBasis of,
      FoodItem source,
      FoodItem target,
      double sourceReferenceG,
      double targetReferenceG) {
    if (of == basis) {
      // Zero by construction: it is the nutrient being held equal. Reporting it would be noise.
      return null;
    }
    double sourceAmount = of.per100gOf(source) * sourceReferenceG / 100.0;
    double targetAmount = of.per100gOf(target) * targetReferenceG / 100.0;
    if (sourceAmount == 0) {
      // A percentage of nothing is not a number. Two foods with no carbohydrate have not drifted
      // apart on carbohydrate, and a source with none of it has no baseline to drift from.
      return null;
    }
    return round1(100.0 * (targetAmount - sourceAmount) / sourceAmount);
  }

  private static double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }
}
