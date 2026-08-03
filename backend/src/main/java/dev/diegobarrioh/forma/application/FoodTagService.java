package dev.diegobarrioh.forma.application;

import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Labelling foods (V50).
 *
 * <p>Labels are replaced whole rather than added and removed one at a time. A form showing twelve
 * checkboxes knows the complete answer when somebody submits it, and treating that as a diff would
 * let two people editing at once each keep half of what the other did.
 */
@Service
public class FoodTagService {

  private final FoodTagRepository repository;
  private final FoodCatalogService foods;

  public FoodTagService(FoodTagRepository repository, FoodCatalogService foods) {
    this.repository = repository;
    this.foods = foods;
  }

  /** Every label there is, in the order a list of checkboxes should show them. */
  public List<Tag> allTags() {
    return repository.findAll();
  }

  /** The labels a food carries. */
  public List<Tag> tagsOf(String foodId) {
    requireFood(foodId);
    return repository.findByFood(foodId);
  }

  /**
   * Sets a food's labels to exactly these (admin only).
   *
   * @throws ValidationException when the food is unknown, or when any label is. A label nobody
   *     defined is a typo rather than a request to invent one — letting it through would grow the
   *     vocabulary by accident and leave "Vegano" and "vegano" side by side. The whole request is
   *     refused rather than the valid half applied, so a caller never has to guess what landed
   */
  public List<Tag> setTagsOf(String foodId, List<String> tagIds) {
    requireFood(foodId);
    // Every id checked before anything is written: a partial application would leave the food in a
    // state nobody asked for and no response could honestly describe.
    for (String tagId : tagIds) {
      if (repository.find(tagId).isEmpty()) {
        throw new ValidationException("No existe la etiqueta: " + tagId);
      }
    }
    // Saying the same thing twice in one request is saying it once.
    repository.replaceTagsOf(foodId, List.copyOf(new LinkedHashSet<>(tagIds)));
    return repository.findByFood(foodId);
  }

  private void requireFood(String foodId) {
    if (foods.findById(foodId).isEmpty()) {
      throw new ValidationException("No existe el alimento: " + foodId);
    }
  }
}
