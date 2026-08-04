package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.EquivalentPortion;
import dev.diegobarrioh.forma.domain.FoodItem;
import dev.diegobarrioh.forma.domain.Preparation;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Curating which food may stand in for which (V47).
 *
 * <p>Reads answer with the grams worked out from today's catalog; nothing here stores them. A
 * substitution written a year ago against a food whose macros were corrected last week reports the
 * corrected answer, which is the whole reason the arithmetic is not a column.
 */
@Service
public class FoodEquivalenceService {

  private final FoodEquivalenceRepository repository;
  private final FoodCatalogService foods;

  public FoodEquivalenceService(FoodEquivalenceRepository repository, FoodCatalogService foods) {
    this.repository = repository;
    this.foods = foods;
  }

  /**
   * What may replace this food, each with the grams it works out to.
   *
   * <p>One direction only. That a food may be replaced by another says nothing about the reverse,
   * and inventing the inverse here would put advice in front of somebody that no curator wrote.
   */
  public List<ResolvedEquivalence> findBySource(String sourceFoodId) {
    FoodItem source = requireFood(sourceFoodId);
    return repository.findBySource(sourceFoodId).stream()
        .map(equivalence -> resolve(source, equivalence))
        .toList();
  }

  /**
   * Records that one food may stand in for another (admin only).
   *
   * <p>The substitution is worked out before it is written, and that is the whole validation: a
   * swap whose grams cannot be computed is a swap that must not be stored, so the refusal from
   * {@link EquivalentPortion#of} is simply let through as a bad request. Keeping a second copy of
   * the rule here is how the two would eventually disagree.
   *
   * @throws ValidationException when either food is unknown, or when the substitution cannot be
   *     worked out — which covers a food standing in for itself and either side carrying none of
   *     the nutrient being matched
   * @throws ConflictException when that pair already has advice on those grounds. The opposite
   *     direction, and the same pair on different grounds, are different statements and are free
   */
  public FoodEquivalence create(FoodEquivalence equivalence) {
    FoodItem source = requireFood(equivalence.sourceFoodId());
    FoodItem target = requireFood(equivalence.targetFoodId());
    if (repository
        .find(equivalence.sourceFoodId(), equivalence.targetFoodId(), equivalence.basis())
        .isPresent()) {
      throw new ConflictException(
          "Ya existe una equivalencia de "
              + equivalence.sourceFoodId()
              + " a "
              + equivalence.targetFoodId()
              + " por "
              + equivalence.basis());
    }
    try {
      EquivalentPortion.of(
          source, target, equivalence.basis(), equivalence.sourceReferenceG().doubleValue());
    } catch (IllegalArgumentException ex) {
      throw new ValidationException(ex.getMessage());
    }
    FoodEquivalence stored = equivalence.identifiedBy(UUID.randomUUID());
    repository.insert(stored);
    return stored;
  }

  /**
   * Removes a substitution (admin only).
   *
   * @throws NotFoundException when no substitution has that id
   */
  public void delete(UUID id) {
    if (!repository.delete(id)) {
      throw new NotFoundException("No existe la equivalencia: " + id);
    }
  }

  private ResolvedEquivalence resolve(FoodItem source, FoodEquivalence equivalence) {
    FoodItem target = requireFood(equivalence.targetFoodId());
    return new ResolvedEquivalence(
        equivalence,
        EquivalentPortion.of(
            source, target, equivalence.basis(), equivalence.sourceReferenceG().doubleValue()),
        target.name(),
        // Reported rather than refused: the grams are right whichever states these are in, and what
        // is off is what they mean. This is the hole the "100 g arroz = 250 g patata" discrepancy
        // pointed into, so a screen showing that swap can now say the two are not comparable.
        Preparation.comparable(preparationOf(source), preparationOf(target)));
  }

  /**
   * The state a food's numbers describe, or null while nobody has said.
   *
   * <p>Read from the catalog rather than carried on {@link FoodItem}: the nutrition domain computes
   * with macros and has no business knowing about kitchens, and this is the one caller that does.
   */
  private Preparation preparationOf(FoodItem food) {
    return foods.preparationOf(food.id());
  }

  private FoodItem requireFood(String foodId) {
    return foods
        .findById(foodId)
        .orElseThrow(() -> new ValidationException("No existe el alimento: " + foodId));
  }
}
