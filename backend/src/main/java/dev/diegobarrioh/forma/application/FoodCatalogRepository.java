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
}
