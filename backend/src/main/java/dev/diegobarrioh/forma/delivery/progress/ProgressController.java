package dev.diegobarrioh.forma.delivery.progress;

import dev.diegobarrioh.forma.application.AchievementService;
import dev.diegobarrioh.forma.application.AdherenceService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Progress REST endpoint: {@code GET /api/v1/progress/adherence?days=} (FOR-129, second
 * implementable slice of FOR-104) — planned vs completed per category over a rolling window ending
 * today. {@code GET /api/v1/progress/achievements} (FOR-135, achievements slice of FOR-104) —
 * earned + available achievements, evaluating and persisting any newly-earned one on every call.
 *
 * <p>Thin controller (ADR-001, ADR-005): delegates all counting/derivation/evaluation to {@link
 * AdherenceService}/{@link AchievementService}; only maps request parameters and the resulting view
 * to the response DTO, mirroring {@code NutritionController}'s one-controller-per-URL-prefix,
 * multiple-services shape. A non-numeric {@code days} is rejected by Spring's binder before
 * reaching the service; an out-of-range {@code days} is rejected by {@link
 * AdherenceService#compute}. Both map to {@code VALIDATION_ERROR} (400) via the FOR-88 {@code
 * GlobalExceptionHandler}. {@code achievements} takes no parameters and never 404s (spec FOR-135
 * api.md).
 *
 * <p>Single-user MVP (ADR-002): mirrors {@code GoalController}'s documented limitation — no
 * account/owner path segment or auth header is accepted yet.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/progress")
public class ProgressController {

  private final AdherenceService service;
  private final AchievementService achievementService;

  public ProgressController(AdherenceService service, AchievementService achievementService) {
    this.service = service;
    this.achievementService = achievementService;
  }

  /** Adherence read model for a {@code days}-long rolling window ending today (default 30). */
  @GetMapping("/adherence")
  public AdherenceResponse adherence(@RequestParam(name = "days", defaultValue = "30") int days) {
    return AdherenceResponse.from(service.compute(days));
  }

  /**
   * Earned + available achievements (FOR-135). Evaluation runs on every GET: any catalog rule newly
   * met by the owner's current data is persisted (idempotently) before the response is built. Never
   * 404s — an owner with nothing earned yet still gets the full available catalog.
   */
  @GetMapping("/achievements")
  public AchievementsResponse achievements() {
    return AchievementsResponse.from(achievementService.evaluate());
  }
}
