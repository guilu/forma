package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.StoreProductImportService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.Store;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Suggestions from a supermarket's own catalogue for a food in ours (FOR-194).
 *
 * <p>Admin only, and read only. It creates nothing: the admin picks a suggestion and the existing
 * {@code POST /store-products} stores it, with the food link and the aisle they chose. Keeping the
 * write on the endpoint that already exists means an imported row is indistinguishable from a
 * hand-typed one — because it should be.
 *
 * <p>Reaching a third-party shop is admin-gated even though it only reads: an unauthenticated
 * endpoint here would let anyone make this server crawl somebody else's site.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/store-products/suggestions")
public class StoreImportController {

  private final StoreProductImportService service;

  public StoreImportController(StoreProductImportService service) {
    this.service = service;
  }

  /**
   * Products from {@code store} that look like the food at {@code foodId}, best first.
   *
   * <p>An unknown food or a chain with no source behind it is a 404; the shop being unreachable is
   * a 502 (see {@code GlobalExceptionHandler}). The screen needs to tell those apart to say
   * anything useful.
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<StoreSuggestionResponse> suggestions(
      @RequestParam String foodId, @RequestParam Store store) {
    return service.suggestionsFor(foodId, store).stream()
        .map(StoreSuggestionResponse::from)
        .toList();
  }
}
