package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.FoodServing;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Body accepted when writing a portion of a food (V49, admin only).
 *
 * <p>Carries no food and no id: both are in the path. The food especially — accepting it in the
 * body would offer a move between foods that the service has to refuse anyway.
 *
 * @param name what to call it, or absent for the plain portion a food starts with. Not every food
 *     needs its portions distinguished, and forcing a name on the only one would mean inventing
 *     "Normal"
 * @param isDefault whether this is what "one serving" means. At most one per food; claiming it
 *     takes it from whichever portion held it
 */
public record FoodServingRequest(
    @Size(max = 64) String name,
    @NotNull @DecimalMin("0.1") BigDecimal grams,
    boolean isDefault,
    int sortOrder) {

  /** Maps the request onto the application's own type; the id is the service's to mint. */
  public FoodServing toFoodServing(String foodId) {
    return new FoodServing(
        null, foodId, name == null || name.isBlank() ? null : name, grams, isDefault, sortOrder);
  }
}
