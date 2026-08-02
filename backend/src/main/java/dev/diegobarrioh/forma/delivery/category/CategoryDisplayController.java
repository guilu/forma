package dev.diegobarrioh.forma.delivery.category;

import dev.diegobarrioh.forma.application.CategoryDisplayService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.CategoryScope;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Category labels and icons (FOR-197) under {@link ApiPaths#V1}{@code /categories}.
 *
 * <p>Reads are open to any signed-in account — every screen that shows a category needs its name —
 * and only the edit is admin-gated. Same asymmetry as the catalogs: shared reference data, single
 * curator.
 *
 * <p>There is no POST and no DELETE. The set of categories is closed in the domain enums and in the
 * database's CHECK constraints; this endpoint changes how one reads, never which ones exist.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/categories")
public class CategoryDisplayController {

  private final CategoryDisplayService service;

  public CategoryDisplayController(CategoryDisplayService service) {
    this.service = service;
  }

  /** Every category, or one vocabulary's when {@code scope} is given. */
  @GetMapping
  public List<CategoryDisplayResponse> list(@RequestParam(required = false) CategoryScope scope) {
    return service.findAll(scope).stream().map(CategoryDisplayResponse::from).toList();
  }

  /** Renames a category and/or changes its icon. */
  @PutMapping("/{scope}/{code}")
  @PreAuthorize("hasRole('ADMIN')")
  public CategoryDisplayResponse update(
      @PathVariable CategoryScope scope,
      @PathVariable String code,
      @Valid @RequestBody CategoryDisplayRequest request) {
    return CategoryDisplayResponse.from(
        service.update(scope, code, request.label(), request.icon()));
  }
}
