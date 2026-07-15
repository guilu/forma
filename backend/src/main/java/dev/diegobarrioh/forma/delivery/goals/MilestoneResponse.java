package dev.diegobarrioh.forma.delivery.goals;

import dev.diegobarrioh.forma.domain.Milestone;

/** Delivery read model for a {@link Milestone} (FOR-125 api.md). */
public record MilestoneResponse(String id, String title, double target, boolean completed) {

  public static MilestoneResponse from(Milestone milestone) {
    return new MilestoneResponse(
        milestone.id(), milestone.title(), milestone.target(), milestone.completed());
  }
}
