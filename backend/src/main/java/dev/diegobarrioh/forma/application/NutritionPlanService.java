package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.PlanStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Owning nutrition plans (V53).
 *
 * <p>Every method is scoped to the caller: a plan belongs to one account, and the repository takes
 * the user id on every read as well as every write, so a plan somebody else owns is not "forbidden"
 * — it is not found, which is the same answer an id that never existed gets and tells a prober
 * nothing.
 *
 * <p>What this layer checks that the database cannot: that a food or a recipe an item names still
 * exists (the foreign key would refuse it, but as a 500 rather than a sentence), and that a portion
 * an item counts belongs to the food beside it. The second is not expressible as a constraint —
 * {@code food_serving} has no composite key to point at — so it lives here, which is exactly where
 * V53's own comment says to look for it.
 */
@Service
public class NutritionPlanService {

  private final NutritionPlanRepository repository;
  private final FoodCatalogService foods;
  private final FoodServingRepository servings;
  private final RecipeRepository recipes;

  public NutritionPlanService(
      NutritionPlanRepository repository,
      FoodCatalogService foods,
      FoodServingRepository servings,
      RecipeRepository recipes) {
    this.repository = repository;
    this.foods = foods;
    this.servings = servings;
    this.recipes = recipes;
  }

  /** Every plan this user owns, newest first. */
  public List<NutritionPlan> findAll(UUID userId) {
    return repository.findAllByUser(userId);
  }

  /**
   * One of this user's plans.
   *
   * @throws NotFoundException when it does not exist, or belongs to somebody else
   */
  public NutritionPlan findById(UUID userId, UUID planId) {
    return repository
        .findById(userId, planId)
        .orElseThrow(() -> new NotFoundException("No existe el plan: " + planId));
  }

  /** The plan this user is currently following, if they have one. */
  public Optional<NutritionPlan> findActive(UUID userId) {
    return repository.findActive(userId);
  }

  /**
   * Writes a new plan.
   *
   * <p>A plan is created as it was given, {@link PlanStatus#DRAFT} or otherwise — but if it arrives
   * asking to be {@link PlanStatus#ACTIVE}, it goes in inactive and is then activated through
   * {@link #activate}, so the "one active plan per user" rule is enforced in exactly one place
   * instead of two that could disagree.
   */
  public NutritionPlan create(NutritionPlan plan) {
    requireUsableItems(plan);
    boolean wantsActive = plan.active();
    NutritionPlan stored = repository.save(asDraftIf(wantsActive, plan));
    if (wantsActive) {
      return activate(stored.userId(), stored.id());
    }
    return stored;
  }

  /**
   * Replaces a plan's contents, leaving its status where it was.
   *
   * <p>The days, meals and items are replaced whole rather than merged: the caller states the
   * complete plan, so what it leaves out is what somebody removed.
   *
   * @throws NotFoundException when it does not exist, or belongs to somebody else
   */
  public NutritionPlan update(UUID userId, UUID planId, NutritionPlan plan) {
    NutritionPlan existing = findById(userId, planId);
    requireUsableItems(plan);
    return repository.save(
        new NutritionPlan(
            planId,
            userId,
            plan.name(),
            plan.description(),
            plan.objective(),
            existing.status(),
            plan.startDate(),
            plan.endDate(),
            plan.targets(),
            plan.generation(),
            plan.days()));
  }

  /**
   * Makes this the plan the user is following.
   *
   * <p>Whatever they were following becomes {@link PlanStatus#COMPLETED}. That is the repository's
   * job because it is one statement away from the activation itself, and doing it in two calls from
   * here would leave a window with no active plan at all.
   *
   * @throws NotFoundException when it does not exist, or belongs to somebody else
   */
  public NutritionPlan activate(UUID userId, UUID planId) {
    findById(userId, planId);
    repository.changeStatus(userId, planId, PlanStatus.ACTIVE);
    return findById(userId, planId);
  }

  /**
   * Moves a plan to a status other than {@link PlanStatus#ACTIVE}.
   *
   * @throws ValidationException when asked to activate — that is {@link #activate}, which has more
   *     to do than set a column
   * @throws NotFoundException when it does not exist, or belongs to somebody else
   */
  public NutritionPlan changeStatus(UUID userId, UUID planId, PlanStatus status) {
    if (status == PlanStatus.ACTIVE) {
      throw new ValidationException("Para activar un plan usa la activación, no el estado.");
    }
    findById(userId, planId);
    repository.changeStatus(userId, planId, status);
    return findById(userId, planId);
  }

  /**
   * Removes a plan and everything under it.
   *
   * @throws NotFoundException when it does not exist, or belongs to somebody else
   */
  public void delete(UUID userId, UUID planId) {
    findById(userId, planId);
    repository.delete(userId, planId);
  }

  private static NutritionPlan asDraftIf(boolean condition, NutritionPlan plan) {
    if (!condition) {
      return plan;
    }
    return new NutritionPlan(
        plan.id(),
        plan.userId(),
        plan.name(),
        plan.description(),
        plan.objective(),
        PlanStatus.DRAFT,
        plan.startDate(),
        plan.endDate(),
        plan.targets(),
        plan.generation(),
        plan.days());
  }

  /**
   * Checks every line of every meal against the catalog before anything is written.
   *
   * <p>Up front rather than as it goes: a plan half-written because its fourteenth day named a food
   * that had been deleted is worse than one refused whole.
   */
  private void requireUsableItems(NutritionPlan plan) {
    for (PlanDay day : plan.days()) {
      for (PlanMeal meal : day.meals()) {
        for (PlanItem item : meal.items()) {
          requireUsable(item);
        }
      }
    }
  }

  private void requireUsable(PlanItem item) {
    if (item.isRecipe()) {
      if (recipes.find(item.recipeId()).isEmpty()) {
        throw new ValidationException("No existe la receta: " + item.recipeId());
      }
      return;
    }
    if (foods.findById(item.foodId()).isEmpty()) {
      throw new ValidationException("No existe el alimento: " + item.foodId());
    }
    if (item.servingId() == null) {
      return;
    }
    FoodServing serving =
        servings
            .find(item.servingId())
            .orElseThrow(() -> new ValidationException("No existe la ración: " + item.servingId()));
    // The rule V53 could not express: a portion counts portions of ITS food. "2 rebanadas de aceite
    // de oliva" is arithmetically fine — 2 x 30 g — and means nothing, and the grams it produces
    // would be silently wrong rather than obviously so.
    if (!serving.foodId().equals(item.foodId())) {
      throw new ValidationException(
          "La ración " + item.servingId() + " no es de " + item.foodId() + ".");
    }
  }
}
