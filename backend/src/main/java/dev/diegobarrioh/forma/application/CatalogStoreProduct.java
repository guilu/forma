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
 * @param ean the barcode (V48), or {@code null}. The only key here that means the same thing in
 *     every shop on earth, which is why it is kept years before anything reads it
 * @param packageAmount how much the package holds, as a number (V48); {@code null} together with
 *     {@code packageUnit} when no size is stated in a usable form
 * @param packageUnit the unit that amount is in — "l", "kg", "g", "ud" — as the shop stated it. Not
 *     converted to grams: five litres is not five of anything weighable without a density
 * @param available whether the shop still lists it (V48). True for a product typed by hand, which
 *     no shop ever stopped selling
 * @param lastSyncedAt when the shop was last asked about it (V48), or {@code null} for a row nobody
 *     imported. Distinct from creation: a product added in January and refreshed last week has two
 *     dates and only one says whether the price is worth trusting
 * @param brand who makes it (V48), or {@code null}. Never filled by an import — no shop we read
 *     publishes it apart from the name — so it is the curator's to write, like {@code notes}
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
    String storeCategoryId,
    String ean,
    java.math.BigDecimal packageAmount,
    String packageUnit,
    boolean available,
    java.time.Instant lastSyncedAt,
    String brand) {

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
        storeCategoryId,
        fresh.ean(),
        fresh.packageAmount(),
        fresh.packageUnit(),
        fresh.available(),
        // Stamped by the act of asking, not by what came back: a refresh that finds the product
        // unchanged still means the price was checked today.
        java.time.Instant.now(),
        // The curator's, not the shop's. No shop we read publishes a brand of its own, so a refresh
        // that overwrote this would replace something somebody typed with nothing.
        brand);
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
        resolvedStoreCategoryId,
        ean,
        packageAmount,
        packageUnit,
        available,
        lastSyncedAt,
        brand);
  }
}
