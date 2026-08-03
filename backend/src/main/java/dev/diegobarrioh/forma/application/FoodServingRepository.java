package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.Optional;

/**
 * Port over a food's portions (V49). Owned by the application side; adapters implement it
 * (ADR-001).
 */
public interface FoodServingRepository {

  /** Every portion of a food, default first and then in its own order. */
  List<FoodServing> findByFood(String foodId);

  /** One portion by id; empty when nobody wrote it. */
  Optional<FoodServing> find(String id);

  /**
   * Takes the default marker off whichever portion of this food holds it, if any.
   *
   * <p>Separate from saving the new default because the order matters: the unique index permits one
   * marked portion per food, so setting before clearing trips it.
   */
  void clearDefault(String foodId);

  /**
   * Removes one portion.
   *
   * @return {@code true} when a row was removed
   */
  boolean delete(String id);

  /** The portion meant by "one serving", or empty when nobody has decided one. */
  Optional<FoodServing> findDefault(String foodId);

  /** Writes the portion, inserting it or overwriting the one with the same id. */
  void save(FoodServing serving);

  /**
   * Removes every portion of a food.
   *
   * <p>For when the food itself goes: a portion is part of a food rather than a thing that refers
   * to one, so it has nothing to mean once the food is gone. Deleting it here rather than through
   * ON DELETE CASCADE keeps the consequence where somebody reading the service can see it.
   */
  void deleteByFood(String foodId);

  /**
   * Removes a food's default portion, leaving its named ones alone.
   *
   * @return {@code true} when a row was removed
   */
  boolean deleteDefault(String foodId);
}
