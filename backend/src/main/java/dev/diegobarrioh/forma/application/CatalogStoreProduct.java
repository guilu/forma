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
 * @param storeCategoryId which shelf of ITS OWN shop the product sits on (V46), or {@code null}.
 *     Null is the common case and not a gap: a product typed by hand never came off a shelf, a
 *     product filed under OTRAS has no shop with shelves, and an imported one stays null until
 *     somebody syncs that shop's aisles. Distinct from {@code category}, which is one of OUR six
 *     and is a person's decision
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
    String imageUrl,
    String storeCategoryId) {

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
        fresh.imageUrl(),
        storeCategoryId);
  }

  /**
   * This product moved onto the shelf the shop says it is on (V46).
   *
   * <p>Separate from {@link #refreshedWith} because the caller has to do work the domain cannot:
   * the shop hands over its own id for the aisle, and turning that into one of our rows means
   * knowing whether that row exists. It usually does not — nobody has to sync a shop's aisles to
   * import from it — and {@code null} is the answer then.
   */
  public CatalogStoreProduct onShelf(String resolvedStoreCategoryId) {
    return new CatalogStoreProduct(
        id,
        store,
        name,
        foodId,
        packageSize,
        priceEur,
        url,
        category,
        notes,
        externalId,
        imageUrl,
        resolvedStoreCategoryId);
  }
}
