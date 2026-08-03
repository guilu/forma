package dev.diegobarrioh.forma.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Curating a food's portions (V49).
 *
 * <p>A food may have several — a banana is small, medium or large — and at most one of them is what
 * "one serving" means. That last rule is enforced by the database with a nullable sentinel, which
 * makes the ORDER of the two writes that swap a default part of the contract rather than a detail:
 * marking the new one before unmarking the old trips the unique index.
 */
@Service
public class FoodServingService {

  private final FoodServingRepository repository;
  private final FoodCatalogService foods;

  public FoodServingService(FoodServingRepository repository, FoodCatalogService foods) {
    this.repository = repository;
    this.foods = foods;
  }

  /** Every portion of a food, default first. */
  public List<FoodServing> findByFood(String foodId) {
    requireFood(foodId);
    return repository.findByFood(foodId);
  }

  /**
   * Adds a portion (admin only).
   *
   * @throws ValidationException when the food is unknown
   * @throws ConflictException when that food already has a portion under the same name. Two
   *     portions of one food called "Grande" is somebody having written it twice; the same name
   *     under a different food is a different portion and is free
   */
  @Transactional
  public FoodServing create(FoodServing serving) {
    requireFood(serving.foodId());
    requireNameIsFree(serving.foodId(), serving.name(), null);
    return write(serving.withId(UUID.randomUUID().toString()));
  }

  /**
   * Replaces a portion (admin only).
   *
   * @throws NotFoundException when nobody wrote that portion
   * @throws ValidationException when the change would move it to a different food, which would
   *     reassign it silently rather than fail
   */
  @Transactional
  public FoodServing update(String id, FoodServing serving) {
    FoodServing current =
        repository.find(id).orElseThrow(() -> new NotFoundException("No existe la ración: " + id));
    if (!current.foodId().equals(serving.foodId())) {
      throw new ValidationException("Una ración no puede cambiar de alimento: " + current.foodId());
    }
    requireNameIsFree(serving.foodId(), serving.name(), id);
    return write(serving.withId(id));
  }

  /**
   * Removes a portion (admin only).
   *
   * <p>Including the default one. A food then has no portion meant by "one serving", which is not a
   * broken state — it is where every food nobody has portioned already sits.
   *
   * @throws NotFoundException when nobody wrote that portion
   */
  public void delete(String id) {
    if (!repository.delete(id)) {
      throw new NotFoundException("No existe la ración: " + id);
    }
  }

  /**
   * Writes the portion, making room first when it claims the default.
   *
   * <p>Transactional because it is two statements that must not half-happen: a crash between them
   * would leave the food with no default at all, silently losing a choice somebody made. It is the
   * only place in this codebase that needs one, which is why it says so.
   */
  private FoodServing write(FoodServing serving) {
    if (serving.isDefault()) {
      // Clearing the one that holds the place, which may well be this same row being saved again —
      // promoting the current default must not end up demoting it to nothing.
      repository.clearDefault(serving.foodId());
    }
    repository.save(serving);
    return serving;
  }

  private void requireNameIsFree(String foodId, String name, String exceptId) {
    if (name == null) {
      return;
    }
    boolean taken =
        repository.findByFood(foodId).stream()
            .filter(existing -> !existing.id().equals(exceptId))
            .anyMatch(existing -> name.equalsIgnoreCase(existing.name()));
    if (taken) {
      throw new ConflictException("Ese alimento ya tiene una ración llamada: " + name);
    }
  }

  private void requireFood(String foodId) {
    if (foods.findById(foodId).isEmpty()) {
      throw new ValidationException("No existe el alimento: " + foodId);
    }
  }
}
