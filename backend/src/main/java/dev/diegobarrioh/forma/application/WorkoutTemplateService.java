package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.StrengthWorkoutTemplate;
import dev.diegobarrioh.forma.domain.WorkoutTemplateCatalog;
import dev.diegobarrioh.forma.domain.WorkoutType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Application use case exposing the strength workout templates (FOR-25).
 *
 * <p>Thin service over the in-code {@link WorkoutTemplateCatalog} so later stories (FOR-26
 * calendar, a future read endpoint) can list templates or resolve one by type. Mirrors the
 * FOR-23/FOR-24 service pattern.
 */
@Service
public class WorkoutTemplateService {

  /** All workout templates. */
  public List<StrengthWorkoutTemplate> allTemplates() {
    return WorkoutTemplateCatalog.templates();
  }

  /** Resolves a template by its workout type. */
  public Optional<StrengthWorkoutTemplate> findByType(WorkoutType type) {
    return WorkoutTemplateCatalog.findByType(type);
  }
}
