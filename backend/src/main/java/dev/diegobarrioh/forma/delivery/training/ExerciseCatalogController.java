package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.CatalogExerciseService;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.Modality;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exercise catalog REST endpoints (FOR-172) under {@link ApiPaths#V1}{@code /training/exercises}:
 * exposes the persisted, multi-modality {@code exercise_catalog} (ADR-011), read-only.
 *
 * <p>Thin controller (ADR-001, ADR-005): delegates to {@link CatalogExerciseService} and maps to
 * the delivery read model. An unrecognized {@code ?modality=} value yields {@code VALIDATION_ERROR}
 * (400, mirrors {@code IntegrationController}'s provider-parsing precedent, FOR-126); an unknown
 * {@code id} yields {@code NOT_FOUND} (404) — both via the FOR-88 {@code GlobalExceptionHandler}.
 * COEXISTS with the static {@code ExerciseCatalog}/{@code WorkoutController} — no consumer repoint
 * in this change.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/training/exercises")
public class ExerciseCatalogController {

  private final CatalogExerciseService service;

  public ExerciseCatalogController(CatalogExerciseService service) {
    this.service = service;
  }

  /** Lists all catalog exercises, or only those of {@code modality} when provided. */
  @GetMapping
  public List<ExerciseCatalogResponse> list(@RequestParam(required = false) String modality) {
    return service.list(parseModality(modality)).stream()
        .map(ExerciseCatalogResponse::from)
        .toList();
  }

  /** Returns one catalog exercise by its id. */
  @GetMapping("/{id}")
  public ExerciseCatalogResponse byId(@PathVariable String id) {
    return ExerciseCatalogResponse.from(service.getById(id));
  }

  private static Optional<Modality> parseModality(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(Modality.valueOf(raw.toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException ex) {
      throw new ValidationException("Modalidad desconocida: " + raw);
    }
  }
}
