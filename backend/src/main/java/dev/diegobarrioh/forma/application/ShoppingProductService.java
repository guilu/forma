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
 */
@Service
public class ShoppingProductService {

  private final ShoppingProductRepository repository;

  public ShoppingProductService(ShoppingProductRepository repository) {
    this.repository = repository;
  }

  /** Lists all products. */
  public List<StoredShoppingProduct> list() {
    return repository.findAll();
  }

  /** Creates a product, stamping {@code lastCheckedAt} to now. */
  public StoredShoppingProduct create(ShoppingProduct product) {
    return repository.create(withCheckedNow(product));
  }

  /**
   * Updates an existing product, stamping {@code lastCheckedAt} to now.
   *
   * @throws NotFoundException if no product has the given id
   */
  public StoredShoppingProduct update(String id, ShoppingProduct product) {
    return repository
        .update(id, withCheckedNow(product))
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
