package dev.diegobarrioh.forma.delivery.shopping;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.StoredShoppingProduct;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response body for the shopping products API (FOR-36). Delivery read model, distinct from the
 * domain type and persistence row (ADR-005). Null fields are omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShoppingProductResponse(
    String id,
    String name,
    String url,
    String packageSize,
    BigDecimal estimatedPriceEur,
    BigDecimal pricePerUnitEur,
    String linkedFoodItemId,
    Instant lastCheckedAt,
    String notes) {

  /** Maps a stored product to its API read model. */
  public static ShoppingProductResponse from(StoredShoppingProduct stored) {
    ShoppingProduct product = stored.product();
    return new ShoppingProductResponse(
        stored.id(),
        product.name(),
        product.url(),
        product.packageSize(),
        product.estimatedPriceEur(),
        product.pricePerUnitEur(),
        product.linkedFoodItemId(),
        product.lastCheckedAt(),
        product.notes());
  }
}
