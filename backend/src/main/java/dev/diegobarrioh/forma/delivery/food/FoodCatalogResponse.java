package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.CatalogFood;
import java.math.BigDecimal;

/**
 * Response body for {@code GET /api/v1/foods} and {@code GET /api/v1/foods/{id}} (FOR-173).
 *
 * <p>Delivery read model, distinct from the application {@link CatalogFood} (ADR-005 — controllers
 * never return application/domain types directly). Unlike {@code ExerciseCatalogResponse} (whose
 * modality-inapplicable fields are omitted via {@code @JsonInclude(NON_NULL)}), key nutrients here
 * are serialized as an EXPLICIT JSON {@code null} when unknown — never omitted, never fabricated as
 * {@code 0} (FOR-134, design decision id 354). This gives nutrition consumers explicit "unknown"
 * semantics for {@code fiberG}/{@code sugarsG}/{@code sodiumMg}/{@code saturatedFatG}.
 */
public record FoodCatalogResponse(
    String id,
    String name,
    BigDecimal servingSizeG,
    int kcal,
    BigDecimal proteinG,
    BigDecimal carbsG,
    BigDecimal fatG,
    BigDecimal fiberG,
    BigDecimal sugarsG,
    BigDecimal sodiumMg,
    BigDecimal saturatedFatG) {

  /** Maps a persisted catalog food to its API read model. */
  public static FoodCatalogResponse from(CatalogFood food) {
    return new FoodCatalogResponse(
        food.id(),
        food.name(),
        food.servingSizeG(),
        food.kcal(),
        food.proteinG(),
        food.carbsG(),
        food.fatG(),
        food.fiberG(),
        food.sugarsG(),
        food.sodiumMg(),
        food.saturatedFatG());
  }
}
