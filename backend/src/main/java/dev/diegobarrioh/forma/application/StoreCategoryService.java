package dev.diegobarrioh.forma.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Keeping a shop's aisles in step with the shop (V46).
 *
 * <p>These rows are a copy of somebody else's data, so the only thing that may write them is a
 * crawl. Nothing here lets an admin rename an aisle: that would be editing what Mercadona calls its
 * own shelf, and the next sync would undo it. Our own six aisles are the ones a person chooses, and
 * they live on the product.
 */
@Service
public class StoreCategoryService {

  private final StoreCategoryRepository repository;
  private final List<StoreCatalogSource> sources;
  private final StoreRepository stores;

  public StoreCategoryService(
      StoreCategoryRepository repository,
      List<StoreCatalogSource> sources,
      StoreRepository stores) {
    this.repository = repository;
    this.sources = sources;
    this.stores = stores;
  }

  /** One shop's currently published aisles, parents before children. */
  public List<StoreCategory> findByStore(String storeId) {
    requireKnownStore(storeId);
    return repository.findByStore(storeId, false);
  }

  /**
   * Re-reads a shop's aisles and writes what changed.
   *
   * @return how many aisles the shop published
   * @throws ValidationException when the id names no shop of ours
   * @throws NotFoundException when the shop has no catalogue behind it — OTRAS never will, and a
   *     chain we have not written an adapter for does not yet
   */
  public int sync(String storeId) {
    requireKnownStore(storeId);
    StoreCatalogSource source =
        sources.stream()
            .filter(candidate -> candidate.store().equals(storeId))
            .findFirst()
            .orElseThrow(
                () -> new NotFoundException("No hay catálogo disponible para: " + storeId));

    List<StoreCategory> published = StoreCategoryTree.flatten(storeId, source.categories());
    if (published.isEmpty()) {
      // "No aisles I can see" is not "the shop closed". A thin crawl — a bad response, an API that
      // moved — would otherwise retire the whole tree in one go, and every product's provenance
      // with it.
      return 0;
    }

    // Parents before children, which flatten guarantees: the other order trips the foreign key.
    published.forEach(repository::save);

    Set<String> stillPublished = new HashSet<>();
    published.forEach(category -> stillPublished.add(category.id()));
    repository.findByStore(storeId, true).stream()
        .filter(stored -> !stillPublished.contains(stored.id()))
        .forEach(stored -> repository.retire(stored.id()));

    return published.size();
  }

  private void requireKnownStore(String storeId) {
    if (stores.find(storeId).isEmpty()) {
      throw new ValidationException("No existe la tienda: " + storeId);
    }
  }
}
