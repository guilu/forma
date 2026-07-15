package dev.diegobarrioh.forma.delivery.goals;

import dev.diegobarrioh.forma.domain.GoalMetric;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for {@code PATCH /api/v1/goals/{id}} (FOR-125 api.md).
 *
 * <p>Every field is optional and partial — a {@code null}/omitted field leaves the stored value
 * unchanged, mirroring {@code UpdateProfileFieldsRequest} (FOR-107): the merge happens in {@link
 * dev.diegobarrioh.forma.application.GoalService}, not here. {@link GoalMetric} is never patchable
 * in this slice (reclassifying a goal's tracked dimension is out of scope).
 *
 * @param title optional new title
 * @param target optional new target
 * @param dueDate optional new due date
 * @param status optional; one of {@code ACTIVE}, {@code ACHIEVED}, {@code ARCHIVED}
 * @param milestones optional list of milestone completion-state changes
 */
public record PatchGoalRequest(
    String title,
    Double target,
    LocalDate dueDate,
    @Pattern(
            regexp = "ACTIVE|ACHIEVED|ARCHIVED",
            message = "must be one of ACTIVE, ACHIEVED, ARCHIVED")
        String status,
    List<@Valid PatchMilestoneRequest> milestones) {}
