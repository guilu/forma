package dev.diegobarrioh.forma.delivery.goals;

import dev.diegobarrioh.forma.application.GoalView;
import java.time.LocalDate;
import java.util.List;

/** Delivery read model for a goal with its derived progress and milestones (FOR-125 api.md). */
public record GoalResponse(
    String id,
    String title,
    String metric,
    double target,
    LocalDate dueDate,
    String status,
    GoalProgressResponse progress,
    List<MilestoneResponse> milestones) {

  public static GoalResponse from(GoalView view) {
    return new GoalResponse(
        view.id(),
        view.goal().title(),
        view.goal().metric().name(),
        view.goal().target(),
        view.goal().dueDate(),
        view.goal().status().name(),
        GoalProgressResponse.from(view.progress()),
        view.goal().milestones().stream().map(MilestoneResponse::from).toList());
  }
}
