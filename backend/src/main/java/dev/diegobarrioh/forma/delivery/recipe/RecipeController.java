package dev.diegobarrioh.forma.delivery.recipe;

import dev.diegobarrioh.forma.application.RecipeService;
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
 * Recipes (V52) under {@link ApiPaths#V1}{@code /recipes}.
 *
 * <p>Reads are open to any signed-in account — a recipe is what somebody is going to cook — and
 * writing is admin only, like the rest of the shared catalog.
 *
 * <p>Every read carries the dish's macros, summed from the catalog at that moment. Nothing here
 * accepts them: they are what the ingredients add up to, and a body offering them would be offering
 * numbers the server has to ignore.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/recipes")
public class RecipeController {

  private final RecipeService service;

  public RecipeController(RecipeService service) {
    this.service = service;
  }

  /** Every recipe, each with what it works out to. */
  @GetMapping
  public List<RecipeResponse> list() {
    return service.findAll().stream().map(RecipeResponse::from).toList();
  }

  /** One recipe with its ingredients and its totals. */
  @GetMapping("/{id}")
  public RecipeResponse get(@PathVariable String id) {
    return RecipeResponse.from(service.findById(id));
  }

  /** Adds a recipe. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public RecipeResponse create(@Valid @RequestBody RecipeRequest request) {
    return RecipeResponse.from(service.create(request.toRecipe()));
  }

  /**
   * Replaces the recipe at {@code id}.
   *
   * <p>The id in the path wins over any in the body: a rename would leave whatever points at the
   * old one pointing at nothing, the same rule the food catalog follows.
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public RecipeResponse update(@PathVariable String id, @Valid @RequestBody RecipeRequest request) {
    return RecipeResponse.from(service.update(id, request.toRecipe()));
  }

  /** Removes a recipe and its ingredients. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
