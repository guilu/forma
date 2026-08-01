package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.Optional;

/**
 * Read-only port for the persisted food catalog (FOR-173). Owned by the application side; adapters
 * implement it (ADR-001).
 */
public interface FoodCatalogRepository {

  /** All catalog foods. */
  List<CatalogFood> findAll();

  /** A single catalog food by id; empty if no food has that id. */
  Optional<CatalogFood> findById(String id);

  /** Stores a new food. Callers check the id is free first — this does not. */
  void insert(CatalogFood food);

  /** Overwrites the food with {@code food.id()}. Callers check it exists first. */
  void update(CatalogFood food);

  /**
   * Removes the food with {@code id}.
   *
   * @return {@code true} when a row was removed, {@code false} when none matched
   */
  boolean delete(String id);
}
