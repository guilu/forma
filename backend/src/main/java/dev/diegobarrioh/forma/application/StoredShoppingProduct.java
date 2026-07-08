package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingProduct;

/**
 * A persisted {@link ShoppingProduct} together with its generated id (FOR-36).
 *
 * <p>The domain type carries no identity (FOR-35); persistence assigns the id, so the CRUD layer
 * works with this pair.
 *
 * @param id the stored product's id
 * @param product the product data
 */
public record StoredShoppingProduct(String id, ShoppingProduct product) {}
