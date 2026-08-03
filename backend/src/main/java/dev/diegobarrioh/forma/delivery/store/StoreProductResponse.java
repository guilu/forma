package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.CatalogStoreProduct;
import java.math.BigDecimal;

/**
 * Response body for the store catalog endpoints (FOR-191).
 *
 * <p>Delivery read model, distinct from the application {@link CatalogStoreProduct} (ADR-005 —
 * controllers never return application types directly). Enums travel as their names, and every
 * optional field is serialized as an explicit JSON {@code null} rather than omitted: a product with
 * no price yet is a real state and the client has to be able to tell it from a price of zero.
 */
public record StoreProductResponse(
    String id,
    String store,
    String name,
    String foodId,
    String packageSize,
    BigDecimal priceEur,
    String url,
    String category,
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

  /** Maps a persisted catalog product to its API read model. */
  public static StoreProductResponse from(CatalogStoreProduct product) {
    return new StoreProductResponse(
        product.id(),
        product.store(),
        product.name(),
        product.foodId(),
        product.packageSize(),
        product.priceEur(),
        product.url(),
        product.category().name(),
        product.notes(),
        product.externalId(),
        product.imageUrl(),
        product.storeCategoryId(),
        product.ean(),
        product.packageAmount(),
        product.packageUnit(),
        product.available(),
        product.lastSyncedAt(),
        product.brand());
  }
}
