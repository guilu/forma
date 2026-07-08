package dev.diegobarrioh.forma.delivery.shopping;

import dev.diegobarrioh.forma.domain.ShoppingProduct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request body for creating/updating a shopping product (FOR-36).
 *
 * <p>Delivery DTO, distinct from the FOR-35 domain type and the persistence row (ADR-005). {@code
 * lastCheckedAt} is not client-supplied — the service stamps it. Bounds match the FOR-35 domain so
 * validation fails at the boundary with {@code VALIDATION_ERROR} rather than a domain exception.
 *
 * @param name required, non-blank
 * @param url optional product URL
 * @param packageSize optional package-size label
 * @param estimatedPriceEur required, strictly positive
 * @param pricePerUnitEur optional, strictly positive when present
 * @param linkedFoodItemId optional soft link to a FOR-30 food id
 * @param notes optional note
 */
public record ShoppingProductRequest(
    @NotBlank String name,
    String url,
    String packageSize,
    @NotNull @Positive BigDecimal estimatedPriceEur,
    @Positive BigDecimal pricePerUnitEur,
    String linkedFoodItemId,
    String notes) {

  /** Builds the domain product (without {@code lastCheckedAt}; the service stamps it). */
  public ShoppingProduct toDomain() {
    return new ShoppingProduct(
        name, url, packageSize, estimatedPriceEur, pricePerUnitEur, linkedFoodItemId, null, notes);
  }
}
