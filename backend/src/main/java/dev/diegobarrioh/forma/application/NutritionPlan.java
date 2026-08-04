package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.PlanStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A nutrition plan (V53): what somebody is meant to eat, as rows.
 *
 * <p>Replaces the three constants in {@code domain.NutritionDayCatalog} with something a user owns,
 * can edit, and can have more than one of. At most one plan per user is {@link PlanStatus#ACTIVE},
 * enforced by the database rather than by whoever remembers to (V53's {@code active_marker}).
 *
 * <p>{@link #targets} is what this plan was asked to hit and is allowed to be unset, in which case
 * the user's own profile figures stand (V20). It is never the sum of the plan's days: totals are
 * computed on read and stored nowhere (ADR-011).
 *
 * @param id the plan's id; null before it has been written
 * @param userId whose plan this is; required
 * @param name what to call it; required, non-blank
 * @param description free text
 * @param objective the goal it serves, in the profile's own vocabulary; null when unstated
 * @param status where it sits in its life; required
 * @param startDate the day it begins; null while it is a template
 * @param endDate the day it ends; null when open-ended
 * @param targets what it was asked to hit; never null, possibly unset
 * @param generation how it came to exist; never null
 * @param days its days, in order
 */
public record NutritionPlan(
    UUID id,
    UUID userId,
    String name,
    String description,
    MainGoal objective,
    PlanStatus status,
    LocalDate startDate,
    LocalDate endDate,
    PlanTargets targets,
    PlanGeneration generation,
    List<PlanDay> days) {

  public NutritionPlan {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("endDate must not precede startDate");
    }
    targets = targets == null ? PlanTargets.none() : targets;
    generation = generation == null ? PlanGeneration.byHand() : generation;
    days = days == null ? List.of() : List.copyOf(days);
  }

  /** Whether this is the plan its owner is currently following. */
  public boolean active() {
    return status == PlanStatus.ACTIVE;
  }
}
