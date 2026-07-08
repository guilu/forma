package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.NutritionDayCatalogService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nutrition REST endpoint (FOR-34): {@code GET /api/v1/nutrition/days/{type}} returns a seeded
 * nutrition day (targets + ordered meals) for the given day type.
 *
 * <p>Thin controller (ADR-001, ADR-005): maps the {@link NutritionDayCatalogService} result to the
 * delivery read model. An unknown day type yields {@code NOT_FOUND} (404) via the FOR-27 {@code
 * GlobalExceptionHandler}. Mounted under {@link ApiPaths#V1}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/nutrition")
public class NutritionController {

  private final NutritionDayCatalogService service;

  public NutritionController(NutritionDayCatalogService service) {
    this.service = service;
  }

  /** Returns the seeded nutrition day for the given type (e.g. {@code running}). */
  @GetMapping("/days/{type}")
  public NutritionDayResponse day(@PathVariable String type) {
    NutritionDayType dayType = parseType(type);
    return service
        .findByType(dayType)
        .map(NutritionDayResponse::from)
        .orElseThrow(() -> new NotFoundException("No existe el día de nutrición: " + type));
  }

  private static NutritionDayType parseType(String type) {
    try {
      return NutritionDayType.valueOf(type.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new NotFoundException("Tipo de día de nutrición desconocido: " + type);
    }
  }
}
