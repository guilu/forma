package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.MealItem;
import dev.diegobarrioh.forma.domain.NutritionCalculator;
import dev.diegobarrioh.forma.domain.NutritionTotals;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Curating recipes (V52).
 *
 * <p>A recipe stores no nutrition, so every read sums it again from the catalog. A dish written a
 * year ago against a food whose macros were corrected last week reports the corrected total, which
 * is the whole reason the sum is not a column.
 *
 * <p>The summing itself is {@link NutritionCalculator}'s, not a second copy: an ingredient is a
 * food and an amount, which is exactly a {@link MealItem}, so the same per-100g arithmetic that
 * computes a logged meal computes a dish. Writing the sum again here is how the two would drift.
 */
@Service
public class RecipeService {

  private final RecipeRepository repository;
  private final FoodCatalogService foods;

  public RecipeService(RecipeRepository repository, FoodCatalogService foods) {
    this.repository = repository;
    this.foods = foods;
  }

  /** Every recipe, each with what it works out to. */
  public List<ResolvedRecipe> findAll() {
    return repository.findAll().stream().map(this::resolve).toList();
  }

  /**
   * One recipe with its totals.
   *
   * @throws NotFoundException when nobody wrote it
   */
  public ResolvedRecipe findById(String id) {
    return resolve(
        repository.find(id).orElseThrow(() -> new NotFoundException("No existe la receta: " + id)));
  }

  /**
   * Adds a recipe (admin only).
   *
   * @throws ConflictException when the id is taken — it is the dish's stable handle
   * @throws ValidationException when it lists no foods, or lists one the catalog does not have. A
   *     dish with nothing in it totals zero and means nothing
   */
  public ResolvedRecipe create(Recipe recipe) {
    if (repository.find(recipe.id()).isPresent()) {
      throw new ConflictException("Ya existe una receta con ese identificador: " + recipe.id());
    }
    requireUsableIngredients(recipe);
    repository.save(recipe);
    return resolve(recipe);
  }

  /**
   * Replaces the recipe at {@code id} (admin only).
   *
   * <p>The ingredients are replaced whole rather than merged: the form shows the complete list, so
   * what it leaves out is what somebody removed.
   *
   * @throws NotFoundException when nobody wrote it
   */
  public ResolvedRecipe update(String id, Recipe recipe) {
    findById(id);
    requireUsableIngredients(recipe);
    Recipe stored =
        new Recipe(
            id,
            recipe.name(),
            recipe.servings(),
            recipe.notes(),
            recipe.enabled(),
            recipe.ingredients(),
            recipe.createdAt(),
            recipe.updatedAt());
    repository.save(stored);
    return resolve(stored);
  }

  /**
   * Removes a recipe (admin only).
   *
   * @throws NotFoundException when nobody wrote it
   */
  public void delete(String id) {
    if (!repository.delete(id)) {
      throw new NotFoundException("No existe la receta: " + id);
    }
  }

  /**
   * The recipe with its totals worked out from today's catalog.
   *
   * <p>An ingredient whose food has gone is carried in {@code unknownFoodIds} rather than thrown:
   * the foreign key should make it impossible, and if it ever happens a dish with one bad line
   * should still render the rest instead of disappearing.
   */
  private ResolvedRecipe resolve(Recipe recipe) {
    List<MealItem> items = new ArrayList<>();
    List<String> unknown = new ArrayList<>();
    for (RecipeIngredient ingredient : recipe.ingredients()) {
      Optional<FoodItem> food = foods.findById(ingredient.foodId());
      if (food.isEmpty()) {
        unknown.add(ingredient.foodId());
        continue;
      }
      // Grams are rounded to whole for the calculation: MealItem counts in whole grams, and a
      // tenth of a gram of oats changes nothing anybody can weigh.
      items.add(
          new MealItem(
              ingredient.foodId(),
              Math.max(
                  1, ingredient.grams().setScale(0, java.math.RoundingMode.HALF_UP).intValue())));
    }
    NutritionTotals total = totalsOf(items);
    return new ResolvedRecipe(
        recipe, total, perServing(total, recipe.servings()), List.copyOf(unknown));
  }

  private NutritionTotals totalsOf(List<MealItem> items) {
    int calories = 0;
    double protein = 0;
    double carbs = 0;
    double fat = 0;
    for (MealItem item : items) {
      // One item at a time through the same calculator a logged meal uses, so a dish and a meal
      // built from the same foods agree to the gram.
      NutritionTotals one = NutritionCalculator.itemTotals(item, foods);
      calories += one.calories();
      protein += one.proteinG();
      carbs += one.carbsG();
      fat += one.fatG();
    }
    return new NutritionTotals(calories, round1(protein), round1(carbs), round1(fat));
  }

  private static NutritionTotals perServing(NutritionTotals total, int servings) {
    return new NutritionTotals(
        Math.round((float) total.calories() / servings),
        round1(total.proteinG() / servings),
        round1(total.carbsG() / servings),
        round1(total.fatG() / servings));
  }

  private static double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private void requireUsableIngredients(Recipe recipe) {
    if (recipe.ingredients().isEmpty()) {
      throw new ValidationException("Una receta necesita al menos un alimento: " + recipe.id());
    }
    for (RecipeIngredient ingredient : recipe.ingredients()) {
      if (foods.findById(ingredient.foodId()).isEmpty()) {
        throw new ValidationException("No existe el alimento: " + ingredient.foodId());
      }
    }
  }
}
