package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting {@link ShoppingProduct}s (FOR-36). Owned by the application side; adapters
 * implement it (ADR-001).
 *
 * <p>{@code userId} is a real account id (FOR-145c "gap table" closure, migration V32) — {@code
 * shopping_products.user_id UUID}, FK-referencing {@code users(id)}. Before this slice the table
 * had NO owner-scoping at all.
 */
public interface ShoppingProductRepository {

  /** All of {@code userId}'s products. */
  List<StoredShoppingProduct> findAllByOwner(UUID userId);

  /** Persists a new product for {@code userId}, generating and returning its id. */
  StoredShoppingProduct create(UUID userId, ShoppingProduct product);

  /**
   * Updates one of {@code userId}'s existing products; empty if no product has the given id for
   * that owner.
   */
  Optional<StoredShoppingProduct> update(UUID userId, String id, ShoppingProduct product);

  /**
   * Gives {@code userId} an entry for each of {@code storeProductIds} it does not already have
   * (FOR-192, V37). Entries created here are pure references: every field is null, so the product
   * reads through to the catalog until the account overrides something.
   *
   * <p>Existing entries are left exactly as they are — this never overwrites an account's own price
   * or notes with the catalog's.
   *
   * @return how many entries were created
   */
  int addMissingCatalogReferences(UUID userId, List<String> storeProductIds);
}
