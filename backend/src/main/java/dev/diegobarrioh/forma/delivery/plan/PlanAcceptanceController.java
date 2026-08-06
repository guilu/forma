package dev.diegobarrioh.forma.delivery.plan;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.PlanAcceptance;
import dev.diegobarrioh.forma.application.PlanActivationService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The first question after logging in: your plan is written, do you want to start it?
 *
 * <p>Its own resource rather than a field on {@code /profile}, because it is not part of the
 * profile: the profile describes a person and this records a decision. Thin controller (ADR-001,
 * ADR-005) — the whole behaviour lives in {@link PlanActivationService}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/plan-acceptance")
public class PlanAcceptanceController {

  private final PlanActivationService service;

  public PlanAcceptanceController(PlanActivationService service) {
    this.service = service;
  }

  /**
   * Whether a plan is waiting to be started. Never 404s: an account with no plan gets {@code
   * pending: false}, which is what the screens' empty state is for.
   */
  @GetMapping
  public PlanAcceptanceResponse pending() {
    return PlanAcceptanceResponse.from(service.pending());
  }

  /** Starts the plan the account was given. 404 when there is nothing waiting. */
  @PostMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void accept() {
    service.accept();
  }

  /**
   * @param planName omitted entirely when there is nothing to offer.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record PlanAcceptanceResponse(boolean pending, String planName) {

    static PlanAcceptanceResponse from(PlanAcceptance acceptance) {
      return new PlanAcceptanceResponse(acceptance.pending(), acceptance.planName());
    }
  }
}
