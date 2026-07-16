package dev.diegobarrioh.forma.delivery.progress;

import dev.diegobarrioh.forma.application.AdherenceService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Progress REST endpoint (FOR-129, second implementable slice of FOR-104): {@code GET
 * /api/v1/progress/adherence?days=} — planned vs completed per category over a rolling window
 * ending today.
 *
 * <p>Thin controller (ADR-001, ADR-005): delegates all counting/derivation to {@link
 * AdherenceService}; only maps the {@code days} query parameter and the resulting view to the
 * response DTO. A non-numeric {@code days} is rejected by Spring's binder before reaching the
 * service; an out-of-range {@code days} is rejected by {@link AdherenceService#compute}. Both map
 * to {@code VALIDATION_ERROR} (400) via the FOR-88 {@code GlobalExceptionHandler}.
 *
 * <p>Single-user MVP (ADR-002): mirrors {@code GoalController}'s documented limitation — no
 * account/owner path segment or auth header is accepted yet.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/progress")
public class ProgressController {

  private final AdherenceService service;

  public ProgressController(AdherenceService service) {
    this.service = service;
  }

  /** Adherence read model for a {@code days}-long rolling window ending today (default 30). */
  @GetMapping("/adherence")
  public AdherenceResponse adherence(@RequestParam(name = "days", defaultValue = "30") int days) {
    return AdherenceResponse.from(service.compute(days));
  }
}
