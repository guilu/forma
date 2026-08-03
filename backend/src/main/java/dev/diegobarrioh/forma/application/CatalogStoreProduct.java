package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingCategory;
import java.math.BigDecimal;

/**
 * A purchasable product in the global store catalog (FOR-191), as the application layer reads and
 * writes it.
 *
 * <p>Sibling of {@link CatalogFood}: that one says what a food is worth nutritionally, this one
 * says where to buy it and what it costs. {@code foodId} links the two and is optional — a product
 * nobody has matched to a food yet is still a real product.
 *
 * <p>Not to be confused with {@link dev.diegobarrioh.forma.domain.ShoppingProduct}, which is one
 * user's own copy of purchase data (per-account since V32). This is reference data shared by every
 * account and editable only by an admin.
 *
 * @param id stable slug, the catalog's handle
 * @param store the chain it is sold by
 * @param name product name as the shelf shows it
 * @param foodId optional link to a {@link CatalogFood} id
 * @param packageSize free-text package label ("500 g", "12 uds"); optional
 * @param priceEur price of that package; optional, since an unpriced product is a real state
 * @param url link to the product page; optional
 * @param category grocery aisle, for grouping a list
 * @param notes free-text caveat ("precio verificar", "sustituible por pavo"); optional
 * @param externalId the shop's own id when this row was imported (FOR-195); absent for anything
 *     transcribed or typed by hand, which is what makes a row refreshable or not
 * @param imageUrl the shop's product photo; absent for the same reason
 */
public record CatalogStoreProduct(
    String id,
    String store,
    String name,
    String foodId,
    String packageSize,
    BigDecimal priceEur,
    String url,
    ShoppingCategory category,
    String notes,
    String externalId,
    String imageUrl) {

  /*
   * Eleven components and no shorter constructor, deliberately: the convenience overload that
   * defaulted externalId and imageUrl to null read as "this product came from nowhere" and was used
   * by the update path, which silently stripped the provenance and the photo off every row an admin
   * edited. Forgetting them now has to be typed out.
   */
  public CatalogStoreProduct {
    if (category == null) {
      category = ShoppingCategory.OTROS;
    }
  }

  /**
   * This product with the fields the shop owns taken from {@code fresh} (FOR-195).
   *
   * <p>Name, package, price, link and photo move; the food link, the aisle and the notes do not.
   * That split is the whole point of a refresh: the shop knows what it sells and for how much, and
   * an admin knows what it is for us. A refresh that overwrote the aisle would undo curation every
   * time a price changed.
   */
  public CatalogStoreProduct refreshedWith(ImportableProduct fresh) {
    return new CatalogStoreProduct(
        id,
        store,
        fresh.name(),
        foodId,
        fresh.packaging(),
        fresh.priceEur(),
        fresh.url(),
        category,
        notes,
        fresh.externalId(),
        fresh.imageUrl());
  }
}
