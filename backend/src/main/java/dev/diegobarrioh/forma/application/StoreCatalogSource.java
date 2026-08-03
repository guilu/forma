package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.Optional;

/**
 * Port over one supermarket's own catalogue (FOR-194). Owned by the application side; adapters
 * implement it (ADR-001).
 *
 * <p>One implementation per chain, chosen by {@link #store()} — the same shape that makes {@code
 * store_product} a single table with a {@code store} column. Adding Carrefour is another adapter
 * and nothing else.
 *
 * <p>Implementations are expected to hold a snapshot rather than fetch per call: the only source
 * that exists today has no search endpoint, so answering a question means having the whole shelf
 * already.
 */
public interface StoreCatalogSource {

  /**
   * The id of the chain this source speaks for — a {@code store} row id (V45).
   *
   * <p>A string rather than a constant because the chains are data and the sources are not: this
   * says which row a source answers for, and a source whose id matches no row can import nothing.
   * {@code StoreCatalogSourcesMatchAStoreTest} refuses that mismatch at build time.
   */
  String store();

  /**
   * Everything the store currently lists.
   *
   * @throws StoreCatalogUnavailableException when the store cannot be reached or answers with
   *     something unusable — never an empty list, which would read as "the shop sells nothing"
   */
  List<ImportableProduct> products();

  /**
   * One product by the store's own id (FOR-195), read straight from the shop rather than from the
   * held snapshot — a refresh exists precisely to get past a snapshot that may be a day old.
   *
   * @return empty when the shop no longer lists it, which is a real answer: a product can be
   *     discontinued, and saying so beats reporting a failure
   * @throws StoreCatalogUnavailableException when the store cannot be reached
   */
  Optional<ImportableProduct> findByExternalId(String externalId);
}
