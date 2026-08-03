package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.Optional;

/**
 * Port over the persisted food groups (V43). Owned by the application side; adapters implement it
 * (ADR-001).
 *
 * <p>No insert and no delete yet. The ten seeded groups cover what the catalog files foods under,
 * and a delete would have to answer what happens to the foods pointing at the group — the foreign
 * key refuses it outright, which is the right answer but not one an endpoint should discover by
 * catching a constraint violation.
 */
public interface FoodGroupRepository {

  /** Every group, in its own {@code sortOrder}. */
  List<FoodGroup> findAll();

  /** One group; empty when no group has that id. */
  Optional<FoodGroup> find(String id);

  /** Overwrites the group with {@code group.id()}. Callers check it exists first. */
  void update(FoodGroup group);
}
