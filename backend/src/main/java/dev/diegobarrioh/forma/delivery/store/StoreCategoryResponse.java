package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.StoreCategory;

/**
 * Response body for a shop's own aisle (V46).
 *
 * <p>Delivery read model, distinct from the application type (ADR-005). Flat, with {@code parentId}
 * and {@code level} carried across rather than nested: a client that wants a tree can build one in
 * a pass, and one that only wants to indent a list already has what it needs.
 *
 * <p>Not to be confused with a {@code category} on a product, which is one of OUR six aisles. This
 * is what the shop calls its shelf.
 */
public record StoreCategoryResponse(
    String id,
    String storeId,
    String parentId,
    String externalId,
    String name,
    String slug,
    int level,
    int sortOrder) {

  public static StoreCategoryResponse from(StoreCategory category) {
    return new StoreCategoryResponse(
        category.id(),
        category.storeId(),
        category.parentId(),
        category.externalId(),
        category.name(),
        category.slug(),
        category.level(),
        category.sortOrder());
  }
}
