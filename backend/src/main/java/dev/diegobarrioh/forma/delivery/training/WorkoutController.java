package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.WorkoutTemplateService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.WorkoutType;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workout template REST endpoints (FOR-99) under {@link ApiPaths#V1}{@code /training/workouts}:
 * expose the FOR-25 strength workout templates with per-exercise details resolved from the FOR-24
 * exercise catalog, so the training UI (FOR-53) can render a real exercise list instead of a
 * placeholder.
 *
 * <p>Thin controller (ADR-001, ADR-005): delegates to {@link WorkoutTemplateService} and maps to
 * the delivery read model. No new domain logic. An unknown workout type yields {@code NOT_FOUND}
 * (404) via the FOR-88 {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/training/workouts")
public class WorkoutController {

  private final WorkoutTemplateService service;

  public WorkoutController(WorkoutTemplateService service) {
    this.service = service;
  }

  /** Returns all strength workout templates. */
  @GetMapping
  public List<WorkoutResponse> all() {
    return service.allTemplates().stream().map(WorkoutResponse::from).toList();
  }

  /** Returns one strength workout template by its type (e.g. {@code PUSH}). */
  @GetMapping("/{type}")
  public WorkoutResponse byType(@PathVariable String type) {
    return service
        .findByType(parseType(type))
        .map(WorkoutResponse::from)
        .orElseThrow(
            () -> new NotFoundException("No existe la plantilla de entrenamiento: " + type));
  }

  private static WorkoutType parseType(String type) {
    try {
      return WorkoutType.valueOf(type.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new NotFoundException("Tipo de entrenamiento desconocido: " + type);
    }
  }
}
