package dev.diegobarrioh.forma.delivery.shopping;

import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request body for creating/updating a shopping product (FOR-36).
 *
 * <p>Delivery DTO, distinct from the FOR-35 domain type and the persistence row (ADR-005). {@code
 * lastCheckedAt} is not client-supplied — the service stamps it. Bounds match the FOR-35 domain so
 * validation fails at the boundary with {@code VALIDATION_ERROR} rather than a domain exception.
 * {@code category} (FOR-106) is validated as one of the known {@link ShoppingCategory} names here
 * (a {@code String}, not the enum type) so an unknown value yields {@code VALIDATION_ERROR} instead
 * of a Jackson enum-parse failure surfacing as 500, mirroring {@code UpdateSessionStatusRequest}.
 *
 * @param name required, non-blank
 * @param url optional product URL
 * @param packageSize optional package-size label
 * @param estimatedPriceEur required, strictly positive
 * @param pricePerUnitEur optional, strictly positive when present
 * @param linkedFoodItemId optional soft link to a FOR-30 food id
 * @param notes optional note
 * @param category optional; one of the {@link ShoppingCategory} names; defaults to {@code OTROS}
 *     when omitted
 */
public record ShoppingProductRequest(
    @NotBlank String name,
    String url,
    String packageSize,
    @NotNull @Positive BigDecimal estimatedPriceEur,
    @Positive BigDecimal pricePerUnitEur,
    String linkedFoodItemId,
    String notes,
    @Pattern(
            regexp =
                "FRUTAS_Y_VERDURAS|PROTEINAS|LACTEOS_Y_HUEVOS|CEREALES_Y_LEGUMBRES"
                    + "|GRASAS_Y_ACEITES|OTROS",
            message =
                "must be one of FRUTAS_Y_VERDURAS, PROTEINAS, LACTEOS_Y_HUEVOS,"
                    + " CEREALES_Y_LEGUMBRES, GRASAS_Y_ACEITES, OTROS")
        String category) {

  /** Builds the domain product (without {@code lastCheckedAt}; the service stamps it). */
  public ShoppingProduct toDomain() {
    return new ShoppingProduct(
        name,
        url,
        packageSize,
        estimatedPriceEur,
        pricePerUnitEur,
        linkedFoodItemId,
        null,
        notes,
        category == null ? null : ShoppingCategory.valueOf(category));
  }
}
