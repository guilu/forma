package dev.diegobarrioh.forma.delivery.food;

import dev.diegobarrioh.forma.application.FoodEquivalenceService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * What may stand in for a food (V47) under {@link ApiPaths#V1}{@code /food-equivalences}.
 *
 * <p>Reads are open to any signed-in account — somebody who has run out of rice needs the answer,
 * and it is shared reference data — while stating a substitution is admin only. Same asymmetry as
 * the catalogs: one curator, everybody reads.
 *
 * <p>Reads are hung off a source food rather than listed whole. "What can I eat instead of this" is
 * the only question anybody asks of this table, and answering it for one food at a time keeps the
 * arithmetic proportional to the question.
 *
 * <p>There is no update. A substitution is a short statement — these two foods, this nutrient, this
 * portion — and changing any part of it makes it a different statement.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/food-equivalences")
public class FoodEquivalenceController {

  private final FoodEquivalenceService service;

  public FoodEquivalenceController(FoodEquivalenceService service) {
    this.service = service;
  }

  /**
   * What may replace {@code foodId}, each with the grams it works out to against today's catalog.
   *
   * <p>One direction only: that rice may be replaced by potato says nothing about the reverse.
   */
  @GetMapping("/{foodId}")
  public List<FoodEquivalenceResponse> forFood(@PathVariable String foodId) {
    return service.findBySource(foodId).stream().map(FoodEquivalenceResponse::from).toList();
  }

  /** States that one food may stand in for another. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public FoodEquivalenceResponse create(@Valid @RequestBody FoodEquivalenceRequest request) {
    var stored = service.create(request.toFoodEquivalence());
    // Read back so the response carries the worked-out grams, which is what the caller came for and
    // what create does not compute a second time.
    return service.findBySource(stored.sourceFoodId()).stream()
        .filter(resolved -> resolved.equivalence().id().equals(stored.id()))
        .findFirst()
        .map(FoodEquivalenceResponse::from)
        .orElseThrow();
  }

  /** Removes a substitution. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
