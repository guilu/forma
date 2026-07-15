package dev.diegobarrioh.forma.delivery.goals;

import dev.diegobarrioh.forma.domain.Milestone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A milestone as supplied on {@code POST /api/v1/goals} (FOR-125): no {@code id} — persistence
 * assigns one — and no {@code completed} (a newly created milestone always starts incomplete).
 *
 * @param title required, non-blank
 * @param target required
 */
public record CreateMilestoneRequest(@NotBlank String title, @NotNull Double target) {

  public Milestone toDomain() {
    return new Milestone(null, title, target, false);
  }
}
