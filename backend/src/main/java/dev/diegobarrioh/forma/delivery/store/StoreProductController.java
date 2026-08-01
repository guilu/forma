package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.StoreProductService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.Store;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Store catalog REST endpoints (FOR-191) under {@link ApiPaths#V1}{@code /store-products}: the
 * global, admin-curated list of what can be bought and where.
 *
 * <p>Thin controller (ADR-001, ADR-005): delegates to {@link StoreProductService} and maps to the
 * delivery read model. Reads are open to any signed-in account because a shopping list is built
 * from this catalog; only the writes carry {@code @PreAuthorize}. That asymmetry is the whole point
 * — shared reference data, single curator.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/store-products")
public class StoreProductController {

  private final StoreProductService service;

  public StoreProductController(StoreProductService service) {
    this.service = service;
  }

  /**
   * Lists catalog products, optionally narrowed to one chain.
   *
   * <p>An unparseable {@code store} is rejected by Spring's converter as a 400 rather than silently
   * listing everything: a typo that answers with the whole catalog looks like the filter worked.
   */
  @GetMapping
  public List<StoreProductResponse> list(@RequestParam(required = false) Store store) {
    return service.findAll(store).stream().map(StoreProductResponse::from).toList();
  }

  /** Returns one catalog product by its id. */
  @GetMapping("/{id}")
  public StoreProductResponse byId(@PathVariable String id) {
    return StoreProductResponse.from(service.getById(id));
  }

  /** Adds a product to the shared catalog. */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public StoreProductResponse create(@Valid @RequestBody StoreProductRequest request) {
    return StoreProductResponse.from(service.create(request.toCatalogStoreProduct()));
  }

  /** Overwrites the product at {@code id}; the path id wins over the body's. */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public StoreProductResponse update(
      @PathVariable String id, @Valid @RequestBody StoreProductRequest request) {
    return StoreProductResponse.from(service.update(id, request.toCatalogStoreProduct()));
  }

  /** Removes the product at {@code id}. */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
