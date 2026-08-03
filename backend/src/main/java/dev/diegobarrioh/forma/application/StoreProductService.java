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
  private final List<StoreCatalogSource> sources;

  public StoreProductService(StoreProductRepository repository, List<StoreCatalogSource> sources) {
    this.repository = repository;
    this.sources = sources;
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
   * <p>The shop reference wins from the stored row for the same reason: {@code externalId} is where
   * the product came from, not something the form offers, and it is what {@link #refresh} is
   * offered on. A body that omits it is a form that never asked, so an edit leaves provenance
   * alone. The image is the opposite — it is on the form, so the body owns it, blank included.
   *
   * @throws NotFoundException when no product has that id
   */
  public CatalogStoreProduct update(String id, CatalogStoreProduct product) {
    CatalogStoreProduct current = getById(id);
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
            product.notes(),
            current.externalId(),
            product.imageUrl());
    repository.update(stored);
    return stored;
  }

  /**
   * Re-reads a product from the shop it was imported from and stores what the shop owns (FOR-195).
   *
   * <p>Name, package, price, link and photo are taken; the food link, the aisle and the notes stay
   * as the admin left them — see {@link CatalogStoreProduct#refreshedWith}. Prices move weekly and
   * curation does not.
   *
   * @throws NotFoundException when no product has that id, when no source speaks for its chain, or
   *     when the shop no longer lists it — in that last case the stored row is left untouched, so a
   *     discontinued product keeps the figures it had instead of being blanked
   * @throws ConflictException when the product was never imported: a row typed by hand has no shop
   *     behind it, and refreshing it against nothing is not a thing to do quietly
   * @throws StoreCatalogUnavailableException when the shop cannot be reached
   */
  public CatalogStoreProduct refresh(String id) {
    CatalogStoreProduct stored = getById(id);
    if (stored.externalId() == null) {
      throw new ConflictException("Este producto no se importó de ninguna tienda: " + id);
    }
    StoreCatalogSource source =
        sources.stream()
            .filter(candidate -> candidate.store() == stored.store())
            .findFirst()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "No hay catálogo disponible para: " + stored.store().name()));
    ImportableProduct fresh =
        source
            .findByExternalId(stored.externalId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "La tienda ya no lista este producto: " + stored.externalId()));
    CatalogStoreProduct refreshed = stored.refreshedWith(fresh);
    repository.update(refreshed);
    return refreshed;
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
