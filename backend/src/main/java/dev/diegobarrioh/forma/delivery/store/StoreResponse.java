package dev.diegobarrioh.forma.delivery.store;

import dev.diegobarrioh.forma.application.Store;

/**
 * Response body for the store endpoint (V45).
 *
 * <p>Delivery read model, distinct from the application type (ADR-005). It carries no hint of
 * whether a catalogue can be imported from the chain: that depends on a bean existing, not on the
 * row, and answering it here would tie a listing to what happens to be deployed.
 */
public record StoreResponse(
    String id, String name, String logoUrl, String website, int sortOrder, boolean enabled) {

  public static StoreResponse from(Store store) {
    return new StoreResponse(
        store.id(),
        store.name(),
        store.logoUrl(),
        store.website(),
        store.sortOrder(),
        store.enabled());
  }
}
