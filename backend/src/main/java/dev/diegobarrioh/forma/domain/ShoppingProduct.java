package dev.diegobarrioh.forma.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A purchasable shopping product with cost information (FOR-35).
 *
 * <p>The Shopping context's core type (docs/domain-model.md, "MercadonaProduct" — named {@code
 * ShoppingProduct} here since it is not strictly Mercadona-specific). It is framework-free — no
 * Spring, JPA/JDBC or HTTP types (ADR-001) — following the FOR-15 precedent, and carries no
 * identity (persistence generates the id, FOR-36).
 *
 * <p>It holds <em>purchase/cost</em> data only and is kept separate from {@link FoodItem}, which
 * holds nutrition values; the two are linked softly by {@link #linkedFoodItemId} (a FOR-30 food id,
 * optional). Money is {@link BigDecimal} (currency-safe). Prices are editable estimates in the MVP
 * — no external price sync.
 *
 * @param name product name; required, non-blank
 * @param url product URL; optional
 * @param packageSize free-text package size label (e.g. "1 kg"); optional
 * @param estimatedPriceEur estimated price in euros; required, strictly positive
 * @param pricePerUnitEur unit price in euros if stored; optional, strictly positive when present
 * @param linkedFoodItemId optional soft link to a FOR-30 {@link FoodItem} id
 * @param lastCheckedAt when the price was last checked; optional
 * @param notes optional free-text note
 * @param category grocery aisle classification (FOR-106); optional on construction — {@code null}
 *     defaults to {@link ShoppingCategory#OTROS} so old rows/callers stay backward compatible
 */
public record ShoppingProduct(
    String name,
    String url,
    String packageSize,
    BigDecimal estimatedPriceEur,
    BigDecimal pricePerUnitEur,
    String linkedFoodItemId,
    Instant lastCheckedAt,
    String notes,
    ShoppingCategory category) {

  public ShoppingProduct {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    requirePositivePrice(estimatedPriceEur, "estimatedPriceEur", true);
    requirePositivePrice(pricePerUnitEur, "pricePerUnitEur", false);
    if (category == null) {
      category = ShoppingCategory.OTROS;
    }
  }

  private static void requirePositivePrice(BigDecimal value, String field, boolean required) {
    if (value == null) {
      if (required) {
        throw new IllegalArgumentException(field + " must not be null");
      }
      return;
    }
    if (value.signum() <= 0) {
      throw new IllegalArgumentException(field + " must be strictly positive, was: " + value);
    }
  }
}
