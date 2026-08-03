package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.Optional;

/**
 * Port over the persisted recipes (V52). Owned by the application side; adapters implement it
 * (ADR-001).
 *
 * <p>A recipe and its ingredients travel together. Nothing ever wants one without the other — a
 * dish with no list of foods is a name — so there is no separate ingredient port to keep in step.
 */
public interface RecipeRepository {

  /** Every recipe, ingredients included. */
  List<Recipe> findAll();

  /** One recipe by id; empty when nobody wrote it. */
  Optional<Recipe> find(String id);

  /** Writes the recipe and replaces its ingredients with exactly the ones it carries. */
  void save(Recipe recipe);

  /**
   * Removes a recipe and its ingredients.
   *
   * @return {@code true} when a row was removed
   */
  boolean delete(String id);
}
