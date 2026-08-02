package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.LinkPreview;
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
  private final LinkPreview linkPreview;

  public StoreProductController(StoreProductService service, LinkPreview linkPreview) {
    this.service = service;
    this.linkPreview = linkPreview;
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

  /**
   * The product photo the page at {@code url} advertises (FOR-200), for an admin typing a product
   * by hand.
   *
   * <p>Admin-only, like the import: this makes the server fetch a URL somebody chose, and that is
   * not something to leave open. A page that publishes no image answers 200 with a null field — it
   * was read, it just says nothing — while a URL that is not a public http(s) address is a 400.
   */
  @GetMapping("/link-image")
  @PreAuthorize("hasRole('ADMIN')")
  public LinkImageResponse linkImage(@RequestParam String url) {
    return new LinkImageResponse(linkPreview.imageFor(url).orElse(null));
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

  /**
   * Re-reads the product at {@code id} from the shop it came from and stores what the shop owns
   * (FOR-195).
   *
   * <p>A POST rather than a PUT: the body is empty and the new values come from somewhere else
   * entirely, so this is "do the thing", not "here is the new state".
   */
  @PostMapping("/{id}/refresh")
  @PreAuthorize("hasRole('ADMIN')")
  public StoreProductResponse refresh(@PathVariable String id) {
    return StoreProductResponse.from(service.refresh(id));
  }

  /** Removes the product at {@code id}. */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
