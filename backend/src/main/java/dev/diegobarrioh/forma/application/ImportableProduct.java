package dev.diegobarrioh.forma.application;

import java.math.BigDecimal;

/**
 * A product as a store's own catalogue describes it (FOR-194), before anybody decides it belongs in
 * ours.
 *
 * <p>Deliberately not a {@link CatalogStoreProduct}: this is what the shop says, and the two fields
 * that make a row of ours useful — which food it is, and which of our six aisles it belongs to —
 * are exactly the ones the shop cannot tell us. An admin supplies those on import, which is why
 * suggestions are a read model and not a write.
 *
 * @param externalId the store's own id, stable enough to build ours from
 * @param name the shelf name, brand and all
 * @param packaging free-text format ("Garrafa", "500 g"); optional
 * @param priceEur price of that package; optional, since an unpriced listing is a real state
 * @param url the product page
 * @param ean barcode, when the store publishes one; the only truly global key here
 * @param storeCategory the store's own aisle name, for the admin to judge by — never mapped
 *     automatically onto our own closed set
 * @param storeCategoryExternalId the store's own id for that aisle (V46), which is what a {@code
 *     store_category} row is keyed on. The name is for a person to read; this is what links the
 *     product to the shelf it came off
 * @param imageUrl the shop's product photo, at whatever size their CDN handed us; callers resize it
 */
public record ImportableProduct(
    String externalId,
    String name,
    String packaging,
    BigDecimal priceEur,
    String url,
    String ean,
    String storeCategory,
    String storeCategoryExternalId,
    String imageUrl) {}
