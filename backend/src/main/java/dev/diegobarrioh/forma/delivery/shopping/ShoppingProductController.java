package dev.diegobarrioh.forma.delivery.shopping;

import dev.diegobarrioh.forma.application.ShoppingProductService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shopping products CRUD endpoints (FOR-36): list, create and update products under {@link
 * ApiPaths#V1}{@code /shopping/products}.
 *
 * <p>Thin controller (ADR-001, ADR-005): validates the request DTO, maps to/from delivery read
 * models, and delegates to {@link ShoppingProductService}. Validation and not-found failures become
 * the standard {@code ApiError} shapes via the FOR-88/FOR-27 {@code GlobalExceptionHandler}. The
 * raw Jira path {@code /api/shopping/products} is mounted under the established {@code /api/v1}
 * convention (documented in specs/FOR-36/spec.md).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/shopping/products")
public class ShoppingProductController {

  private final ShoppingProductService service;

  public ShoppingProductController(ShoppingProductService service) {
    this.service = service;
  }

  /** Lists all products (ordered by name). */
  @GetMapping
  public List<ShoppingProductResponse> list() {
    return service.list().stream().map(ShoppingProductResponse::from).toList();
  }

  /** Creates a product. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ShoppingProductResponse create(@Valid @RequestBody ShoppingProductRequest request) {
    return ShoppingProductResponse.from(service.create(request.toDomain()));
  }

  /** Updates an existing product; 404 if the id does not exist. */
  @PutMapping("/{id}")
  public ShoppingProductResponse update(
      @PathVariable String id, @Valid @RequestBody ShoppingProductRequest request) {
    return ShoppingProductResponse.from(service.update(id, request.toDomain()));
  }
}
