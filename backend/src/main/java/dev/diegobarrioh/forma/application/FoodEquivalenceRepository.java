package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.EquivalenceBasis;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port over the persisted substitutions (V47). Owned by the application side; adapters implement it
 * (ADR-001).
 *
 * <p>No update. A substitution is a short statement — these two foods, this nutrient, this portion
 * — and changing any of it makes it a different statement; there is nothing to amend that is not
 * simply a new one. Retiring uses {@code enabled}, which is a fact about the advice rather than an
 * edit to it.
 */
public interface FoodEquivalenceRepository {

  /** The substitutions offered for a food, in one direction only and excluding retired ones. */
  List<FoodEquivalence> findBySource(String sourceFoodId);

  /** One substitution by its natural key; empty when nobody has stated it. */
  Optional<FoodEquivalence> find(String sourceFoodId, String targetFoodId, EquivalenceBasis basis);

  /** Stores a new substitution. Callers check the pair is free first — this does not. */
  void insert(FoodEquivalence equivalence);

  /**
   * Removes a substitution.
   *
   * @return {@code true} when a row was removed, {@code false} when none matched
   */
  boolean delete(UUID id);
}
