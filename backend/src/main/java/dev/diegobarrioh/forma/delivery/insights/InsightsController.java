package dev.diegobarrioh.forma.delivery.insights;

import dev.diegobarrioh.forma.application.InsightHistoryService;
import dev.diegobarrioh.forma.application.WeeklyInsights;
import dev.diegobarrioh.forma.application.WeeklyInsightsService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Insights REST endpoints under {@link ApiPaths#V1}{@code /insights}: the current weekly check-in
 * summary plus prioritized recommendations (FOR-45), and the FOR-110 persisted history.
 *
 * <p>Thin controller (ADR-001, ADR-005): it delegates to {@link WeeklyInsightsService} (generation,
 * which also persists — FOR-110) and {@link InsightHistoryService} (history listing +
 * week-over-week deltas), then maps the result to the delivery DTO. {@code /weekly} always returns
 * {@code 200} — empty underlying data still yields a valid response with an insufficient-data main
 * recommendation, not an error.
 *
 * <p>Jira writes {@code /api/insights/weekly}; this applies the established {@code /api/v1} prefix
 * (documented adaptation, as in FOR-17). {@code /history} naming is indicative (spec FOR-110
 * ui.md).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/insights")
public class InsightsController {

  private final WeeklyInsightsService insightsService;
  private final InsightHistoryService historyService;

  public InsightsController(
      WeeklyInsightsService insightsService, InsightHistoryService historyService) {
    this.insightsService = insightsService;
    this.historyService = historyService;
  }

  /**
   * Returns the current week's insights: check-in summary + main/secondary recommendations + its
   * week-over-week deltas vs. the immediately prior persisted period (FOR-110; null/absent fields
   * when there is no prior period).
   */
  @GetMapping("/weekly")
  public WeeklyInsightsResponse weekly() {
    WeeklyInsights insights = insightsService.currentInsights();
    return WeeklyInsightsResponse.from(insights, historyService.deltasFor(insights.checkIn()));
  }

  /**
   * Returns every persisted period's insights, most recent first, each with its own week-over-week
   * deltas (FOR-110). Empty list when nothing has been generated yet — not an error.
   */
  @GetMapping("/history")
  public List<WeeklyInsightsResponse> history() {
    return historyService.history().stream()
        .map(
            insights ->
                WeeklyInsightsResponse.from(insights, historyService.deltasFor(insights.checkIn())))
        .toList();
  }
}
