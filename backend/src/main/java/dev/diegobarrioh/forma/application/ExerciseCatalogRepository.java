package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Modality;
import java.util.List;
import java.util.Optional;

/**
 * Read-only port for the persisted exercise catalog (FOR-172). Owned by the application side;
 * adapters implement it (ADR-001).
 */
public interface ExerciseCatalogRepository {

  /** All catalog exercises, any modality. */
  List<CatalogExercise> findAll();

  /** Catalog exercises of a single modality. */
  List<CatalogExercise> findByModality(Modality modality);

  /** A single catalog exercise by id; empty if no exercise has that id. */
  Optional<CatalogExercise> findById(String id);
}
