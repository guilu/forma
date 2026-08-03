package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.FoodServingService;
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
 * A food's portions (V49) under {@link ApiPaths#V1}{@code /foods/{foodId}/servings}.
 *
 * <p>Nested under the food because a portion has no meaning apart from one: "150 g" is not a thing,
 * "a large banana" is. Reads are open to any signed-in account — anybody logging a meal needs to
 * know what sizes exist — and writing is admin only, like the rest of the shared catalog.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/foods/{foodId}/servings")
public class FoodServingController {

  private final FoodServingService service;

  public FoodServingController(FoodServingService service) {
    this.service = service;
  }

  /** Every portion of the food, the default first. */
  @GetMapping
  public List<FoodServingResponse> list(@PathVariable String foodId) {
    return service.findByFood(foodId).stream().map(FoodServingResponse::from).toList();
  }

  /** Adds a portion. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public FoodServingResponse create(
      @PathVariable String foodId, @Valid @RequestBody FoodServingRequest request) {
    return FoodServingResponse.from(service.create(request.toFoodServing(foodId)));
  }

  /**
   * Replaces a portion.
   *
   * <p>The food comes from the path, so a body cannot move a portion to a different food — that
   * would reassign it silently rather than fail.
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public FoodServingResponse update(
      @PathVariable String foodId,
      @PathVariable String id,
      @Valid @RequestBody FoodServingRequest request) {
    return FoodServingResponse.from(service.update(id, request.toFoodServing(foodId)));
  }

  /** Removes a portion, including the default one. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable String foodId, @PathVariable String id) {
    service.delete(id);
  }
}
