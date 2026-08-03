package dev.diegobarrioh.forma.application;

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
  private final StoreRepository stores;
  private final StoreCategoryRepository aisles;

  public StoreProductService(
      StoreProductRepository repository,
      List<StoreCatalogSource> sources,
      StoreRepository stores,
      StoreCategoryRepository aisles) {
    this.repository = repository;
    this.sources = sources;
    this.stores = stores;
    this.aisles = aisles;
  }

  /** Catalog products, narrowed to one chain when {@code store} is given; all of them when null. */
  public List<CatalogStoreProduct> findAll(String store) {
    if (store != null) {
      requireKnownStore(store);
    }
    return repository.findAll(store);
  }

  /**
   * Refuses a chain that is not one of ours.
   *
   * <p>{@code store} used to be an enum, so Spring's converter answered a typo with a 400 before
   * anything else ran. It is a {@code store} row id since V45, so a typo would otherwise reach the
   * database: as a filter it would quietly return nothing, and as a write it would trip the foreign
   * key and surface as a 500. Both look like a bug in the wrong place. Checking here keeps the 400
   * and says which value was wrong.
   */
  /**
   * The shop's id for an aisle, as one of our {@code store_category} rows — or {@code null}.
   *
   * <p>Null in every case where the answer is not certain, and none of them is an error. Nobody has
   * to sync a shop's aisles before importing from it, so the row usually does not exist yet;
   * refusing the request over that would block a perfectly good import, and building the id anyway
   * would trip the foreign key and surface as a server error.
   *
   * <p>The store is checked too, not just the id. Two shops number their aisles independently, so
   * Mercadona's shelf 112 and Carrefour's shelf 112 are different shelves, and a product must never
   * borrow the wrong one because the numbers happened to match.
   */
  private String resolveAisle(String storeId, String aisleExternalId) {
    if (aisleExternalId == null || aisleExternalId.isBlank()) {
      return null;
    }
    String id = StoreCategoryTree.idFor(storeId, aisleExternalId);
    return aisles
        .find(id)
        .filter(aisle -> aisle.storeId().equals(storeId))
        .map(StoreCategory::id)
        .orElse(null);
  }

  private void requireKnownStore(String store) {
    if (stores.find(store).isEmpty()) {
      throw new ValidationException("No existe la tienda: " + store);
    }
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
    return create(product, null);
  }

  /**
   * Adds a product to the shared catalog, filing it on the shop's own shelf when we know that shelf
   * (V46, admin only).
   *
   * @param aisleExternalId the SHOP's id for the aisle, as the suggestion reported it, or {@code
   *     null} for a product nobody imported
   */
  public CatalogStoreProduct create(CatalogStoreProduct product, String aisleExternalId) {
    if (repository.findById(product.id()).isPresent()) {
      throw new ConflictException("Ya existe un producto con el id: " + product.id());
    }
    requireKnownStore(product.store());
    CatalogStoreProduct stored = product.onShelf(resolveAisle(product.store(), aisleExternalId));
    repository.insert(stored);
    return stored;
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
    requireKnownStore(product.store());
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
            product.imageUrl(),
            // Owned by the shop, like the name and the price: an edit does not move a product to a
            // different shelf of Mercadona's, so the stored answer stands until a refresh.
            current.storeCategoryId(),
            // Everything the shop owns stays as the last sync left it: the admin form asks for none
            // of it, so a body that omits it is a form that never offered it, not an erasure.
            current.ean(),
            current.packageAmount(),
            current.packageUnit(),
            current.available(),
            current.lastSyncedAt(),
            // The brand is the one of these a person does type, so the body owns it, blank
            // included.
            product.brand());
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
            .filter(candidate -> candidate.store().equals(stored.store()))
            .findFirst()
            .orElseThrow(
                () -> new NotFoundException("No hay catálogo disponible para: " + stored.store()));
    ImportableProduct fresh =
        source
            .findByExternalId(stored.externalId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "La tienda ya no lista este producto: " + stored.externalId()));
    CatalogStoreProduct refreshed =
        stored
            .refreshedWith(fresh)
            .onShelf(resolveAisle(stored.store(), fresh.storeCategoryExternalId()));
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
