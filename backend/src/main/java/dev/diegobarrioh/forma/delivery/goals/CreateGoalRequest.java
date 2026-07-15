package dev.diegobarrioh.forma.delivery.goals;

import dev.diegobarrioh.forma.domain.Goal;
import dev.diegobarrioh.forma.domain.GoalMetric;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

/**
 * Request body for {@code POST /api/v1/goals} (FOR-125 api.md).
 *
 * <p>Delivery DTO, distinct from the {@link Goal} domain type (ADR-005). {@code metric} is
 * validated as a {@code String} against the known {@link GoalMetric} names here (not the enum type
 * itself) so an unknown value yields {@code VALIDATION_ERROR} instead of a Jackson enum-parse
 * failure surfacing as 500 — mirroring {@code UpdateProfileFieldsRequest} (FOR-107) / {@code
 * ShoppingProductRequest.category} (FOR-106).
 *
 * @param title required, non-blank
 * @param metric one of {@code BODY_FAT_PCT}, {@code WEIGHT_KG}, {@code LEAN_MASS_KG}
 * @param target required
 * @param dueDate optional, ISO-8601
 * @param milestones optional; defaults to no milestones when omitted
 */
public record CreateGoalRequest(
    @NotBlank String title,
    @NotBlank
        @Pattern(
            regexp = "BODY_FAT_PCT|WEIGHT_KG|LEAN_MASS_KG",
            message = "must be one of BODY_FAT_PCT, WEIGHT_KG, LEAN_MASS_KG")
        String metric,
    @NotNull Double target,
    LocalDate dueDate,
    List<@Valid CreateMilestoneRequest> milestones) {

  public Goal toDomain() {
    List<dev.diegobarrioh.forma.domain.Milestone> domainMilestones =
        milestones == null
            ? List.of()
            : milestones.stream().map(CreateMilestoneRequest::toDomain).toList();
    return new Goal(title, GoalMetric.valueOf(metric), target, dueDate, null, domainMilestones);
  }
}
