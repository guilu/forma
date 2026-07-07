package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Exercise;
import dev.diegobarrioh.forma.domain.ExerciseCatalog;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing the strength exercise catalog (FOR-24).
 *
 * <p>Thin service over the in-code {@link ExerciseCatalog} so later stories (FOR-25 workout
 * templates, and a future read endpoint) can list exercises or resolve one by id. Mirrors the
 * FOR-23 {@code RunningPlanService} pattern.
 */
@Service
public class ExerciseCatalogService {

  /** All catalog exercises. */
  public List<Exercise> allExercises() {
    return ExerciseCatalog.exercises();
  }

  /** Resolves a catalog exercise by its stable id. */
  public Optional<Exercise> findById(String id) {
    return ExerciseCatalog.findById(id);
  }
}
