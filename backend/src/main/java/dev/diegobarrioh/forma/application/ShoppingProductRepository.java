package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingProduct;
import java.util.List;
import java.util.Optional;

/**
 * Port for persisting {@link ShoppingProduct}s (FOR-36). Owned by the application side; adapters
 * implement it (ADR-001).
 */
public interface ShoppingProductRepository {

  /** All products. */
  List<StoredShoppingProduct> findAll();

  /** Persists a new product, generating and returning its id. */
  StoredShoppingProduct create(ShoppingProduct product);

  /** Updates an existing product; empty if no product has the given id. */
  Optional<StoredShoppingProduct> update(String id, ShoppingProduct product);
}
