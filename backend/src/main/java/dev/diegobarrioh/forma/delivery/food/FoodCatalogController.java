package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.CatalogFoodService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Food catalog REST endpoints (FOR-173) under {@link ApiPaths#V1}{@code /foods}: exposes the
 * persisted {@code food_catalog} (ADR-011), read-only.
 *
 * <p>Thin controller (ADR-001, ADR-005): delegates to {@link CatalogFoodService} and maps to the
 * delivery read model. An unknown {@code id} yields {@code NOT_FOUND} (404) via the FOR-88 {@code
 * GlobalExceptionHandler}. No {@code category} filter — no food category concept exists. COEXISTS
 * with the static {@code FoodCatalog}/{@code FoodCatalogService} — no consumer repoint in this
 * change.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/foods")
public class FoodCatalogController {

  private final CatalogFoodService service;

  public FoodCatalogController(CatalogFoodService service) {
    this.service = service;
  }

  /** Lists all catalog foods. */
  @GetMapping
  public List<FoodCatalogResponse> list() {
    return service.listAll().stream().map(FoodCatalogResponse::from).toList();
  }

  /** Returns one catalog food by its id. */
  @GetMapping("/{id}")
  public FoodCatalogResponse byId(@PathVariable String id) {
    return FoodCatalogResponse.from(service.getById(id));
  }
}
