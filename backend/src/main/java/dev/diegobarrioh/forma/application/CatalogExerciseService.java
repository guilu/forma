package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Modality;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing the persisted exercise catalog (FOR-172), read-only. Thin service
 * over {@link ExerciseCatalogRepository}, mirroring {@link WorkoutTemplateService}'s pattern.
 */
@Service
public class CatalogExerciseService {

  private final ExerciseCatalogRepository repository;

  public CatalogExerciseService(ExerciseCatalogRepository repository) {
    this.repository = repository;
  }

  /** All exercises, optionally filtered by modality. */
  public List<CatalogExercise> list(Optional<Modality> modality) {
    return modality.map(repository::findByModality).orElseGet(repository::findAll);
  }

  /** A single exercise by id; throws {@link NotFoundException} when no exercise has that id. */
  public CatalogExercise getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("No existe el ejercicio: " + id));
  }
}
