package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.Optional;

/**
 * Port over the persisted supermarket chains (V45). Owned by the application side; adapters
 * implement it (ADR-001).
 *
 * <p>Read-only for now. A chain is an insert away from existing, but nothing offers that yet: it
 * needs a name and a place in the list, and the screen that would ask for them does not exist. The
 * foreign key already refuses to delete one that products point at.
 */
public interface StoreRepository {

  /** Every chain, in its own {@code sortOrder}. */
  List<Store> findAll();

  /** One chain; empty when no chain has that id. */
  Optional<Store> find(String id);
}
