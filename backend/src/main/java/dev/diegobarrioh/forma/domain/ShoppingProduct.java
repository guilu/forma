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
 * @param storeProductId optional link to the global store catalog (FOR-192, V37). When set, this is
 *     the account's entry FOR that catalog product and any {@code null} field means "whatever the
 *     catalog says"; {@link #resolveWith} fills those in. When absent, the product stands alone and
 *     must carry its own name and price, as every product did before the catalog existed.
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
    ShoppingCategory category,
    String storeProductId) {

  public ShoppingProduct {
    boolean referencesCatalog = storeProductId != null;
    // A standalone product still has to name and price itself. A reference may
    // leave both out, because the catalog carries them — the difference between
    // "the user set this" and "inherit it" is exactly what the nulls encode.
    if (!referencesCatalog && (name == null || name.isBlank())) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (name != null && name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    requirePositivePrice(estimatedPriceEur, "estimatedPriceEur", !referencesCatalog);
    requirePositivePrice(pricePerUnitEur, "pricePerUnitEur", false);
    if (category == null) {
      category = ShoppingCategory.OTROS;
    }
  }

  /**
   * A product that stands alone, with no catalog behind it — the shape every product had before
   * FOR-192, kept so the callers that create one do not have to say "and no catalog" every time.
   */
  public ShoppingProduct(
      String name,
      String url,
      String packageSize,
      BigDecimal estimatedPriceEur,
      BigDecimal pricePerUnitEur,
      String linkedFoodItemId,
      Instant lastCheckedAt,
      String notes,
      ShoppingCategory category) {
    this(
        name,
        url,
        packageSize,
        estimatedPriceEur,
        pricePerUnitEur,
        linkedFoodItemId,
        lastCheckedAt,
        notes,
        category,
        null);
  }

  /**
   * This product with every unset field taken from the catalog row it references (FOR-192).
   *
   * <p>Field by field, not all-or-nothing: an account that corrected the price of one product still
   * sees the catalog's name, package and link for it. {@code category} is special — the record
   * defaults it to {@link ShoppingCategory#OTROS} rather than leaving it null, so an entry still
   * sitting on that default takes the catalog's aisle instead; a product deliberately filed under
   * "Otros" in the catalog is unaffected, since both agree.
   *
   * @param catalog the referenced product's values; {@code null} leaves this product untouched,
   *     which is what a dangling or absent reference must do — never blank the row
   */
  public ShoppingProduct resolveWith(StoreProductValues catalog) {
    if (catalog == null) {
      return this;
    }
    return new ShoppingProduct(
        name != null ? name : catalog.name(),
        url != null ? url : catalog.url(),
        packageSize != null ? packageSize : catalog.packageSize(),
        estimatedPriceEur != null ? estimatedPriceEur : catalog.priceEur(),
        pricePerUnitEur,
        linkedFoodItemId != null ? linkedFoodItemId : catalog.foodId(),
        lastCheckedAt,
        notes != null ? notes : catalog.notes(),
        category == ShoppingCategory.OTROS ? catalog.category() : category,
        storeProductId);
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
