package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.ImportableProduct;
import dev.diegobarrioh.forma.application.StoreProductImportService;
import dev.diegobarrioh.forma.application.ValidationException;
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
 * <p>Two ways in: by a food in our catalog, or by free text. The first fills the link between a
 * product and a food by construction; the second exists for what our catalog cannot name — the
 * seeded rows V40 could not match, and anything an admin wants that no food describes.
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
      @RequestParam(required = false) String foodId,
      @RequestParam(required = false) String q,
      @RequestParam Store store) {
    // One or the other, never both: starting from a food fills the link between
    // product and food by construction, and starting from text is for the
    // products our own catalog cannot name.
    if (foodId == null && q == null) {
      throw new ValidationException("Indica un alimento (foodId) o un texto de búsqueda (q)");
    }
    List<ImportableProduct> found =
        foodId != null ? service.suggestionsFor(foodId, store) : service.searchFor(q, store);
    return found.stream().map(StoreSuggestionResponse::from).toList();
  }
}
