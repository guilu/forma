package dev.diegobarrioh.forma.delivery.planrequest;

import dev.diegobarrioh.forma.application.PlanRequestService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Plan-request REST endpoints (ADR-015 slice 2, capture): {@code /api/v1/plan-requests}.
 *
 * <p>Every method is scoped to the authenticated caller through {@link PlanRequestService}, which
 * resolves the caller's id itself (mirroring {@code BodyMeasurementService}'s pattern) — no
 * principal argument here (ADR-005: thin controllers).
 *
 * <p>Thin by design: {@link PlanRequestService} owns the one-open-request rule and the frozen-input
 * arithmetic; nothing here does either.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/plan-requests")
public class PlanRequestController {

  private final PlanRequestService service;

  public PlanRequestController(PlanRequestService service) {
    this.service = service;
  }

  /**
   * Captures a new plan request from the wizard's answers.
   *
   * @throws dev.diegobarrioh.forma.application.ConflictException (409) when the caller already has
   *     an open request (ADR-015 decision 4)
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PlanRequestResponse create(@Valid @RequestBody PlanRequestCreateRequest request) {
    return PlanRequestResponse.from(
        service.request(
            request.direction(),
            request.trainingDaysPerWeek(),
            request.trainingWeekdays(),
            request.equipment(),
            request.mealsPerDay(),
            request.dietPattern(),
            request.cuisineStyle()));
  }

  /**
   * The caller's currently open request, so the UI can show its state while it waits.
   *
   * @throws dev.diegobarrioh.forma.application.NotFoundException (404) when the caller has none —
   *     the same answer this codebase gives for every absent singular resource, never a special
   *     empty shape
   */
  @GetMapping("/current")
  public PlanRequestResponse current() {
    return PlanRequestResponse.from(service.currentOpen());
  }
}
