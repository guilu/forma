package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.ShoppingProduct;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use cases for shopping products (FOR-36).
 *
 * <p>Lists, creates and updates products via the {@link ShoppingProductRepository} port. Stamps
 * {@code lastCheckedAt} to now on create/update (the price was just entered/verified), so callers
 * don't supply it. An update to an unknown id yields a {@link NotFoundException} → 404. Controllers
 * stay thin (ADR-001); this is where the "when checked" rule lives.
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V32): this "gap table" service had ZERO
 * owner-scoping before this slice. Every use case now resolves the caller's account id via {@link
 * CurrentUserProvider} and passes it to the repository on every call.
 */
@Service
public class ShoppingProductService {

  private final ShoppingProductRepository repository;
  private final CurrentUserProvider currentUserProvider;

  public ShoppingProductService(
      ShoppingProductRepository repository, CurrentUserProvider currentUserProvider) {
    this.repository = repository;
    this.currentUserProvider = currentUserProvider;
  }

  /** Lists all of the caller's products. */
  public List<StoredShoppingProduct> list() {
    return repository.findAllByOwner(currentUserProvider.currentUserId());
  }

  /** Creates a product for the caller, stamping {@code lastCheckedAt} to now. */
  public StoredShoppingProduct create(ShoppingProduct product) {
    return repository.create(currentUserProvider.currentUserId(), withCheckedNow(product));
  }

  /**
   * Updates one of the caller's existing products, stamping {@code lastCheckedAt} to now.
   *
   * @throws NotFoundException if no product has the given id for the caller
   */
  public StoredShoppingProduct update(String id, ShoppingProduct product) {
    return repository
        .update(currentUserProvider.currentUserId(), id, withCheckedNow(product))
        .orElseThrow(() -> new NotFoundException("No existe el producto: " + id));
  }

  private static ShoppingProduct withCheckedNow(ShoppingProduct product) {
    return new ShoppingProduct(
        product.name(),
        product.url(),
        product.packageSize(),
        product.estimatedPriceEur(),
        product.pricePerUnitEur(),
        product.linkedFoodItemId(),
        Instant.now(),
        product.notes(),
        product.category());
  }
}
