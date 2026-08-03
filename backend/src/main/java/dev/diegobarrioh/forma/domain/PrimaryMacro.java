package dev.diegobarrioh.forma.domain;

import java.util.Optional;

/**
 * Which macronutrient a food is mostly made of (V44).
 *
 * <p>A closed set, and unlike {@code food_group} it stays closed: there are three macronutrients
 * because there are three, not because a curator drew the line there. Nothing an admin does will
 * ever produce a fourth, so this is an enum and the food groups are a table.
 *
 * <p>Deliberately separate from the group a food is filed under. A group answers "what shelf is
 * this on" — lácteos, legumbres — and is an editorial decision; this answers "what is it made of"
 * and is arithmetic. Skimmed milk is a lácteo whose calories are mostly carbohydrate, and a plan
 * that swaps foods by macro needs the second answer, not the first.
 */
public enum PrimaryMacro {
  PROTEIN,
  CARBS,
  FAT;

  /** Atwater factors: the calories a gram of each macronutrient carries. */
  private static final double KCAL_PER_G_PROTEIN = 4.0;

  private static final double KCAL_PER_G_CARBS = 4.0;
  private static final double KCAL_PER_G_FAT = 9.0;

  /**
   * The macro contributing the most calories per 100 g, if one of them clearly does.
   *
   * <p>By calories rather than by grams, which is the whole reason it is computed: a whole egg
   * carries more protein than fat by weight and is still mostly fat on the plate, because a gram of
   * fat is worth more than twice a gram of protein.
   *
   * <p>Empty rather than a guess in three cases: a macro is unknown (nothing is known, so nothing
   * is claimed), every macro is zero (water has no dominant macro), or two tie (nothing decides it,
   * and picking whichever is compared first would dress an arbitrary choice as a measurement). The
   * caller stores the empty answer as "nobody has decided", which someone who knows the food can
   * then decide.
   */
  public static Optional<PrimaryMacro> dominantOf(Double proteinG, Double carbsG, Double fatG) {
    if (proteinG == null || carbsG == null || fatG == null) {
      return Optional.empty();
    }
    double protein = proteinG * KCAL_PER_G_PROTEIN;
    double carbs = carbsG * KCAL_PER_G_CARBS;
    double fat = fatG * KCAL_PER_G_FAT;
    if (protein <= 0 && carbs <= 0 && fat <= 0) {
      return Optional.empty();
    }
    // Strictly greater than both others, so a tie falls through to empty.
    if (protein > carbs && protein > fat) {
      return Optional.of(PROTEIN);
    }
    if (carbs > protein && carbs > fat) {
      return Optional.of(CARBS);
    }
    if (fat > protein && fat > carbs) {
      return Optional.of(FAT);
    }
    return Optional.empty();
  }
}
