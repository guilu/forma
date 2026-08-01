package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Store;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use cases for the global store product catalog (FOR-191).
 *
 * <p>Same shape as {@link CatalogFoodService}: a thin service over its repository, with the writes
 * gated to admins at the delivery edge. The catalog is shared by every account — nothing here takes
 * a user id, and nothing here is scoped by one.
 */
@Service
public class StoreProductService {

  private final StoreProductRepository repository;

  public StoreProductService(StoreProductRepository repository) {
    this.repository = repository;
  }

  /** Catalog products, narrowed to one chain when {@code store} is given; all of them when null. */
  public List<CatalogStoreProduct> findAll(Store store) {
    return repository.findAll(store);
  }

  /** A single product by id; throws {@link NotFoundException} when no product has that id. */
  public CatalogStoreProduct getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("No existe el producto: " + id));
  }

  /**
   * Adds a product to the shared catalog (admin only).
   *
   * @throws ConflictException when the id is already taken — it is the catalog's stable handle, so
   *     it is never reassigned to a different product
   */
  public CatalogStoreProduct create(CatalogStoreProduct product) {
    if (repository.findById(product.id()).isPresent()) {
      throw new ConflictException("Ya existe un producto con el id: " + product.id());
    }
    repository.insert(product);
    return product;
  }

  /**
   * Overwrites the product at {@code id} (admin only).
   *
   * <p>The path id wins over whatever the body carries: a rename through an edit would leave every
   * reference to the old id pointing at nothing.
   *
   * @throws NotFoundException when no product has that id
   */
  public CatalogStoreProduct update(String id, CatalogStoreProduct product) {
    getById(id);
    CatalogStoreProduct stored =
        new CatalogStoreProduct(
            id,
            product.store(),
            product.name(),
            product.foodId(),
            product.packageSize(),
            product.priceEur(),
            product.url(),
            product.category(),
            product.notes());
    repository.update(stored);
    return stored;
  }

  /**
   * Removes the product at {@code id} (admin only).
   *
   * @throws NotFoundException when no product has that id, so a repeated delete says so instead of
   *     reporting success for a row that was never there
   */
  public void delete(String id) {
    if (!repository.delete(id)) {
      throw new NotFoundException("No existe el producto: " + id);
    }
  }
}
