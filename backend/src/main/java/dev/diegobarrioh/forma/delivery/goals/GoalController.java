package dev.diegobarrioh.forma.delivery.goals;

import dev.diegobarrioh.forma.application.GoalService;
import dev.diegobarrioh.forma.application.MilestonePatch;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.GoalStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Goals & milestones REST endpoints (FOR-125) under {@link ApiPaths#V1}{@code /goals}: list goals
 * with derived progress, create a goal (optionally with milestones), and partially update a goal's
 * fields and/or milestone completion state.
 *
 * <p>Thin controller (ADR-001, ADR-005): validates request DTOs, converts known-value strings to
 * domain enums, and delegates all merge/derivation behavior to {@link GoalService}. Never accepts
 * or returns domain/persistence types directly.
 *
 * <p>Single-user MVP (ADR-002): every endpoint operates on the one account {@link GoalService}
 * resolves internally; no account/owner path segment or auth header is accepted yet — a known,
 * documented MVP limitation (AGENTS.md), not an oversight.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/goals")
public class GoalController {

  private final GoalService service;

  public GoalController(GoalService service) {
    this.service = service;
  }

  /** Lists the owner's goals with derived progress and milestones; empty, never 404. */
  @GetMapping
  public GoalsListResponse list() {
    return new GoalsListResponse(service.list().stream().map(GoalResponse::from).toList());
  }

  /** Creates a goal, optionally with milestones. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public GoalResponse create(@Valid @RequestBody CreateGoalRequest request) {
    return GoalResponse.from(service.create(request.toDomain()));
  }

  /**
   * Partially updates a goal's fields and/or milestone completion state; omitted fields are
   * unchanged.
   */
  @PatchMapping("/{id}")
  public GoalResponse update(
      @PathVariable String id, @Valid @RequestBody PatchGoalRequest request) {
    GoalStatus status = request.status() == null ? null : GoalStatus.valueOf(request.status());
    List<MilestonePatch> milestonePatches =
        request.milestones() == null
            ? List.of()
            : request.milestones().stream()
                .map(m -> new MilestonePatch(m.id(), m.completed()))
                .toList();
    return GoalResponse.from(
        service.update(
            id, request.title(), request.target(), request.dueDate(), status, milestonePatches));
  }
}
