package dev.diegobarrioh.forma.domain;

import java.util.Optional;

/**
 * Resolves a food id to its nutrition values, for the domain calculations that need one.
 *
 * <p>The domain has to read foods but must not know where they live. Before this, {@link
 * NutritionCalculator} reached for a static in-code catalog, which pinned the only source of
 * nutrition data to a compiled constant: the persisted {@code food_catalog} table could not be it,
 * because a static initializer runs long before any database exists. Taking a lookup as a parameter
 * moves that decision to whoever calls the calculation, so the domain stays framework-free
 * (ADR-001) while the application layer supplies a repository-backed implementation.
 *
 * <p>A functional interface on purpose: a test can pass a map or a lambda without a stub class, and
 * the application can pass a method reference.
 */
@FunctionalInterface
public interface FoodLookup {

  /**
   * The food with this id, or empty when no food has it.
   *
   * <p>Empty is a real answer — an id can be stale, or name a food someone deleted — and callers
   * decide what it means. {@link NutritionCalculator} rejects it rather than skipping the item, so
   * totals are never silently understated.
   */
  Optional<FoodItem> findById(String id);
}
