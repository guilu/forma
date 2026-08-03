package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.StoreCategoryService;
import dev.diegobarrioh.forma.application.StoreRepository;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The supermarket chains (V45) under {@link ApiPaths#V1}{@code /stores}.
 *
 * <p>Read-only and open to any signed-in account, like the catalogs: every screen that lets someone
 * say where a product was bought needs the list, and the list stopped being something a client
 * could hardcode the moment it became a table.
 *
 * <p>No POST and no DELETE. Adding a chain is an insert the database already accepts, but it needs
 * a name and a place in the order, and the screen that would ask for those does not exist yet —
 * offering a half-filled row would be worse than not offering one.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/stores")
public class StoreController {

  private final StoreRepository stores;
  private final StoreCategoryService categories;

  public StoreController(StoreRepository stores, StoreCategoryService categories) {
    this.stores = stores;
    this.categories = categories;
  }

  /** Every chain, in the order they should be shown. */
  @GetMapping
  public List<StoreResponse> list() {
    return stores.findAll().stream().map(StoreResponse::from).toList();
  }

  /**
   * One shop's own aisles (V46), parents before children.
   *
   * <p>Open to any signed-in account, like the rest of the reference data. Empty until somebody
   * syncs the shop, and permanently empty for a chain with no catalogue behind it — {@code OTRAS}
   * has none by definition.
   */
  @GetMapping("/{id}/categories")
  public List<StoreCategoryResponse> categories(@PathVariable String id) {
    return categories.findByStore(id).stream().map(StoreCategoryResponse::from).toList();
  }

  /**
   * Re-reads a shop's aisles from the shop itself and writes what changed.
   *
   * <p>Admin only, and a POST rather than a PUT because it is an action with a side effect on
   * somebody else's server, not a value being set. It is the only thing that may write these rows:
   * they are a copy of the shop's own words, so an edit here would be overwritten by the next sync.
   */
  @PostMapping("/{id}/categories/sync")
  @PreAuthorize("hasRole('ADMIN')")
  public List<StoreCategoryResponse> syncCategories(@PathVariable String id) {
    categories.sync(id);
    return categories.findByStore(id).stream().map(StoreCategoryResponse::from).toList();
  }
}
