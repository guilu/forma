package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.Optional;

/**
 * Port over the labels and which foods carry them (V50). Owned by the application side; adapters
 * implement it (ADR-001).
 *
 * <p>One port for both tables because they are one question. Nothing ever wants the join table
 * without the vocabulary — a label id is not something anybody can read.
 */
public interface FoodTagRepository {

  /** The whole vocabulary, in its own order. */
  List<Tag> findAll();

  /** One label; empty when nobody defined it. */
  Optional<Tag> find(String id);

  /** The labels a food carries, in the vocabulary's order. */
  List<Tag> findByFood(String foodId);

  /**
   * Sets a food's labels to exactly this list.
   *
   * <p>Replaces rather than merges: the screen that calls this shows every label at once, so what
   * it leaves out is what somebody unticked, not what it forgot to mention.
   */
  void replaceTagsOf(String foodId, List<String> tagIds);
}
