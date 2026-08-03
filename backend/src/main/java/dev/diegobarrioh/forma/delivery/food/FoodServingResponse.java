package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.FoodServing;
import java.math.BigDecimal;

/**
 * Response body for one portion of a food (V49).
 *
 * <p>Delivery read model, distinct from the application type (ADR-005). {@code isDefault} travels
 * as a plain boolean rather than as the sentinel the database keeps: how the one-per-food rule is
 * enforced portably is nobody else's problem.
 */
public record FoodServingResponse(
    String id, String foodId, String name, BigDecimal grams, boolean isDefault, int sortOrder) {

  public static FoodServingResponse from(FoodServing serving) {
    return new FoodServingResponse(
        serving.id(),
        serving.foodId(),
        serving.name(),
        serving.grams(),
        serving.isDefault(),
        serving.sortOrder());
  }
}
