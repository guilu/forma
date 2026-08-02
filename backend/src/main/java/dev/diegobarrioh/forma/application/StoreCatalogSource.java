package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Store;
import java.util.List;

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

  /** The chain this source speaks for. */
  Store store();

  /**
   * Everything the store currently lists.
   *
   * @throws StoreCatalogUnavailableException when the store cannot be reached or answers with
   *     something unusable — never an empty list, which would read as "the shop sells nothing"
   */
  List<ImportableProduct> products();
}
