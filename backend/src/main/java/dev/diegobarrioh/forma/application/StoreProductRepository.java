package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.Optional;

/**
 * Port for the persisted store product catalog (FOR-191). Owned by the application side; adapters
 * implement it (ADR-001).
 */
public interface StoreProductRepository {

  /** Catalog products, narrowed to one chain when {@code store} is given; all of them when null. */
  List<CatalogStoreProduct> findAll(String store);

  /** A single product by id; empty when no product has that id. */
  Optional<CatalogStoreProduct> findById(String id);

  /** Stores a new product. Callers check the id is free first — this does not. */
  void insert(CatalogStoreProduct product);

  /** Overwrites the product with {@code product.id()}. Callers check it exists first. */
  void update(CatalogStoreProduct product);

  /**
   * Removes the product with {@code id}.
   *
   * @return {@code true} when a row was removed, {@code false} when none matched
   */
  boolean delete(String id);
}
