package dev.diegobarrioh.forma.delivery.plan;

import dev.diegobarrioh.forma.application.FoodServing;
import dev.diegobarrioh.forma.domain.FoodItem;
import java.util.List;

/**
 * The vocabulary an import file may use: every food, its macros and its named portions.
 *
 * <p>Exists because a model asked to write a plan and given no list of foods will invent ids, and
 * every one of them will be rejected. Handing it this is the difference between a format it can
 * write and a format it can guess at.
 *
 * <p>Read from the database rather than kept as a document beside the code, so it cannot fall
 * behind a food somebody adds from the admin screen — which is the same reason nothing else in this
 * model stores what it can compute.
 *
 * @param foods every food the catalog holds
 */
public record ImportCatalogResponse(List<Food> foods) {

  /**
   * @param id what an import file writes in {@code foodId}
   * @param name what a person calls it
   * @param per100g its macros, which the plan never states and always computes from
   * @param preparation whether those macros describe it raw, cooked, or as sold (V51); null when
   *     nobody has said, which is a different thing from the question not applying
   * @param servings its named portions, each usable as {@code servingId}
   */
  public record Food(
      String id, String name, Macros per100g, String preparation, List<Serving> servings) {}

  public record Macros(int kcal, double proteinG, double carbsG, double fatG) {}

  /**
   * @param id what an import file writes in {@code servingId}
   * @param name what to call it — "Mediano", "Cucharada" — or null for the plain one
   * @param grams what one of them weighs
   * @param isDefault whether this is the one meant by "one serving"
   */
  public record Serving(String id, String name, double grams, boolean isDefault) {}

  static Food food(FoodItem item, String preparation, List<FoodServing> servings) {
    return new Food(
        item.id(),
        item.name(),
        new Macros(
            item.kcalPer100g(), item.proteinPer100g(), item.carbsPer100g(), item.fatPer100g()),
        preparation,
        servings.stream()
            .map(
                serving ->
                    new Serving(
                        serving.id(),
                        serving.name(),
                        serving.grams().doubleValue(),
                        serving.isDefault()))
            .toList());
  }
}
