package dev.diegobarrioh.forma.application;

/**
 * One line of a planned meal (V53): a food or a dish, and how much of it.
 *
 * <p>ONE AMOUNT, whose unit the row's own fields name — not the quantity/unit/grams triple the
 * source document asks for, which is the same fact three times and free to disagree with itself:
 *
 * <ul>
 *   <li>{@code foodId} and no {@code servingId} — {@link #amount} is grams.
 *   <li>{@code foodId} and a {@code servingId} — {@link #amount} counts that portion (V49): 1.0 is
 *       one medium banana, 2.0 is two tablespoons.
 *   <li>{@code recipeId} — {@link #amount} counts servings of the dish (V52).
 * </ul>
 *
 * <p>Grams follow from the portion or the recipe rather than being stored beside them, so a portion
 * corrected from 120 g to 125 g moves every plan that says "one medium banana" — which is what
 * whoever wrote that meant.
 *
 * <p>There is no {@code sortOrder} field. A meal's lines are a list, and the position in that list
 * is the order; carrying a number beside it would be the same fact in two places, free to disagree
 * the first time somebody inserts a line without renumbering the rest. The column exists — a list
 * has to come back in the order it went in — and the repository fills it from the position.
 *
 * @param id the row's id; null before it has been written
 * @param foodId a catalog food, or null when this line is a dish
 * @param recipeId a recipe, or null when this line is a food
 * @param servingId which portion {@code amount} counts; null means grams
 * @param amount how much, in the unit the other fields name; strictly positive
 * @param preparationNotes free text ("a la plancha", "sin sal")
 * @param optional whether this line can be skipped
 */
public record PlanItem(
    java.util.UUID id,
    String foodId,
    String recipeId,
    String servingId,
    double amount,
    String preparationNotes,
    boolean optional) {

  public PlanItem {
    boolean isFood = foodId != null && !foodId.isBlank();
    boolean isRecipe = recipeId != null && !recipeId.isBlank();
    if (isFood == isRecipe) {
      throw new IllegalArgumentException(
          "an item is a food or a recipe, never both and never neither");
    }
    if (servingId != null && !isFood) {
      throw new IllegalArgumentException("a serving counts portions of a food, not of a recipe");
    }
    if (amount <= 0) {
      throw new IllegalArgumentException("amount must be strictly positive, was: " + amount);
    }
  }

  /** Whether this line names a dish rather than a food. */
  public boolean isRecipe() {
    return recipeId != null;
  }
}
