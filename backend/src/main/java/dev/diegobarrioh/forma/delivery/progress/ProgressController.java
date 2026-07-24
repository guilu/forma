package dev.diegobarrioh.forma.delivery.progress;

import dev.diegobarrioh.forma.application.AchievementService;
import dev.diegobarrioh.forma.application.AdherenceService;
import dev.diegobarrioh.forma.application.StreakService;
import dev.diegobarrioh.forma.application.WeeklyHistoryService;
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
 * {@code GET /api/v1/progress/streak} and {@code GET /api/v1/progress/weekly-history} (FOR-139,
 * streak-&-weekly-history slice of FOR-104) — consistency streak and per-week bars, both derived on
 * demand from real per-date nutrition history.
 *
 * <p>Thin controller (ADR-001, ADR-005): delegates all counting/derivation/evaluation to {@link
 * AdherenceService}/{@link AchievementService}/{@link StreakService}/{@link WeeklyHistoryService};
 * only maps request parameters and the resulting view to the response DTO, mirroring {@code
 * NutritionController}'s one-controller-per-URL-prefix, multiple-services shape. A non-numeric
 * {@code days}/{@code weeks} is rejected by Spring's binder before reaching the service; an
 * out-of-range value is rejected by the service's {@code compute} method. Both map to {@code
 * VALIDATION_ERROR} (400) via the FOR-88 {@code GlobalExceptionHandler}. For the legacy placeholder
 * owner, {@code achievements}, {@code streak} and {@code weekly-history} never 404 — an owner with
 * no history yet still gets a zeroed/empty read model (spec FOR-135/FOR-139 api.md).
 *
 * <p><b>Interim security guard (mandatory review of 145b-1, HIGH cross-account disclosure):</b>
 * {@link AdherenceService}, {@link AchievementService}, {@link StreakService} and {@link
 * WeeklyHistoryService} still read only the legacy placeholder owner's data (their per-user wiring
 * is deferred to 145b-2/145c). Until then, all four reject any authenticated caller other than the
 * placeholder account with a 404 ({@code NotFoundException}), so a real self-registered user can
 * never read the legacy owner's private health data through these endpoints.
 *
 * <p>Single-user MVP (ADR-002): mirrors {@code GoalController}'s documented limitation — no
 * account/owner path segment or auth header is accepted yet.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/progress")
public class ProgressController {

  private final AdherenceService service;
  private final AchievementService achievementService;
  private final StreakService streakService;
  private final WeeklyHistoryService weeklyHistoryService;

  public ProgressController(
      AdherenceService service,
      AchievementService achievementService,
      StreakService streakService,
      WeeklyHistoryService weeklyHistoryService) {
    this.service = service;
    this.achievementService = achievementService;
    this.streakService = streakService;
    this.weeklyHistoryService = weeklyHistoryService;
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

  /**
   * Current + longest consistency streak (FOR-139) over a {@code days}-long lookback ending today
   * (default 90, bounded {@code [1, 365]} — see {@link StreakService}).
   */
  @GetMapping("/streak")
  public StreakResponse streak(@RequestParam(name = "days", defaultValue = "90") int days) {
    return StreakResponse.from(streakService.compute(days));
  }

  /**
   * Per-week planned-vs-completed series (FOR-139) over the last {@code weeks} weeks, ending with
   * the current week (default 8, bounded {@code [1, 52]} — see {@link WeeklyHistoryService}).
   */
  @GetMapping("/weekly-history")
  public WeeklyHistoryResponse weeklyHistory(
      @RequestParam(name = "weeks", defaultValue = "8") int weeks) {
    return WeeklyHistoryResponse.from(weeklyHistoryService.compute(weeks));
  }
}
