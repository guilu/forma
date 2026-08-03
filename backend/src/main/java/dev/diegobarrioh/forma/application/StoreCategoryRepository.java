package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.Optional;

/**
 * Port over one shop's persisted aisles (V46). Owned by the application side; adapters implement it
 * (ADR-001).
 *
 * <p>There is no delete. Products point at these rows through a foreign key, and an aisle the shop
 * stopped publishing is retired rather than removed — a shopping history that loses where something
 * came from is worse than one that says the shelf no longer exists.
 */
public interface StoreCategoryRepository {

  /**
   * One shop's aisles, parents before children.
   *
   * @param includeRetired whether to include the ones the shop no longer publishes. A screen
   *     offering a choice wants them left out; one rendering where an old product came from needs
   *     them
   */
  List<StoreCategory> findByStore(String storeId, boolean includeRetired);

  /** One aisle by our own id; empty when no aisle has it. */
  Optional<StoreCategory> find(String id);

  /** Writes the aisle, inserting it or overwriting the one with the same id. */
  void save(StoreCategory category);

  /** Marks the aisle as no longer published. Does nothing when no aisle has that id. */
  void retire(String id);
}
