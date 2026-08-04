package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.PlanStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for nutrition plans (V53), owned by the application layer (ADR-001).
 *
 * <p>A plan is loaded and saved WHOLE — days, meals and items together — rather than through a port
 * per level. The four tables are one aggregate: a meal without its day is not a thing anybody asks
 * for, and letting callers write half of one is how a day ends up with meals that contradict its
 * targets.
 */
public interface NutritionPlanRepository {

  /** Every plan the user owns, newest first, with all their days, meals and items. */
  List<NutritionPlan> findAllByUser(UUID userId);

  /** One plan of that user, or empty when it does not exist or belongs to somebody else. */
  Optional<NutritionPlan> findById(UUID userId, UUID planId);

  /** The plan the user is currently following, if any. At most one can exist (V53). */
  Optional<NutritionPlan> findActive(UUID userId);

  /**
   * Writes a plan whole: the header, then its days, meals and items, replacing whatever was there.
   *
   * @return the plan as stored, with every generated id filled in
   */
  NutritionPlan save(NutritionPlan plan);

  /** Moves a plan to a status, maintaining the one-active-plan invariant. */
  void changeStatus(UUID userId, UUID planId, PlanStatus status);

  /** Removes a plan and everything under it. */
  void delete(UUID userId, UUID planId);
}
