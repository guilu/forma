package dev.diegobarrioh.forma.delivery.insights;

import dev.diegobarrioh.forma.application.WeeklyInsightsService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Insights REST endpoint (FOR-45) under {@link ApiPaths#V1}{@code /insights}: the weekly check-in
 * summary plus prioritized recommendations for the dashboard.
 *
 * <p>Thin controller (ADR-001, ADR-005): it delegates to {@link WeeklyInsightsService} and maps the
 * result to the delivery DTO. Always returns {@code 200} — empty underlying data still yields a
 * valid response with an insufficient-data main recommendation, not an error.
 *
 * <p>Jira writes {@code /api/insights/weekly}; this applies the established {@code /api/v1} prefix
 * (documented adaptation, as in FOR-17).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/insights")
public class InsightsController {

  private final WeeklyInsightsService insightsService;

  public InsightsController(WeeklyInsightsService insightsService) {
    this.insightsService = insightsService;
  }

  /** Returns the current week's insights: check-in summary + main/secondary recommendations. */
  @GetMapping("/weekly")
  public WeeklyInsightsResponse weekly() {
    return WeeklyInsightsResponse.from(insightsService.currentInsights());
  }
}
