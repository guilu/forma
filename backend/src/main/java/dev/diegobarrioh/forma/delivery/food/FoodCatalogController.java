package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.CatalogFoodService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

  /**
   * Adds a food to the shared catalog (FOR-190).
   *
   * <p>Admin only, like the two below. Reading stays open to every authenticated account: the
   * catalog is reference data the whole app runs on, and only its maintenance is restricted.
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public FoodCatalogResponse create(@Valid @RequestBody FoodCatalogRequest request) {
    return FoodCatalogResponse.from(service.create(request.toCatalogFood()));
  }

  /** Replaces a food. The id in the path wins over any id in the body — see the service. */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public FoodCatalogResponse update(
      @PathVariable String id, @Valid @RequestBody FoodCatalogRequest request) {
    return FoodCatalogResponse.from(service.update(id, request.toCatalogFood()));
  }

  /**
   * Removes a food. 409 rather than a cascade when a shopping product still references it: the
   * foreign key refuses, and deleting someone's linked product as a side effect of tidying the
   * catalog would be a surprise nobody asked for.
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
