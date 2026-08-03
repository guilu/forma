package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.ImportableProduct;
import java.math.BigDecimal;

/**
 * A candidate from a store's own catalogue (FOR-194), as the admin screen reads it.
 *
 * <p>Deliberately NOT shaped like {@link StoreProductResponse}: this is what the shop says, not a
 * row of ours, and the difference has to stay visible on screen. `storeCategory` is the shop's own
 * aisle name, offered as a hint for the admin to file the product under one of our six — never
 * mapped automatically, since their 151 shelves and our 6 aisles are different vocabularies.
 */
public record StoreSuggestionResponse(
    String externalId,
    String name,
    String packaging,
    BigDecimal priceEur,
    String url,
    String ean,
    String storeCategory,
    String storeCategoryExternalId,
    String imageUrl) {

  public static StoreSuggestionResponse from(ImportableProduct product) {
    return new StoreSuggestionResponse(
        product.externalId(),
        product.name(),
        product.packaging(),
        product.priceEur(),
        product.url(),
        product.ean(),
        product.storeCategory(),
        product.storeCategoryExternalId(),
        product.imageUrl());
  }
}
