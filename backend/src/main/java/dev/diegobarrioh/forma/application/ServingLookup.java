package dev.diegobarrioh.forma.application;

import java.util.Optional;

/**
 * Finding one named portion by id (V49).
 *
 * <p>A narrow contract over the eight-method {@link FoodServingRepository}, in the same spirit as
 * {@link dev.diegobarrioh.forma.domain.FoodLookup}: logging a meal needs to know what one portion
 * weighs and has no business being able to create, re-order or delete them.
 */
@FunctionalInterface
public interface ServingLookup {

  /** The portion with that id, or empty when nobody wrote it. */
  Optional<FoodServing> find(String servingId);
}
