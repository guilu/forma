package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.StoreRepository;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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

  public StoreController(StoreRepository stores) {
    this.stores = stores;
  }

  /** Every chain, in the order they should be shown. */
  @GetMapping
  public List<StoreResponse> list() {
    return stores.findAll().stream().map(StoreResponse::from).toList();
  }
}
