package dev.diegobarrioh.forma.delivery.body;

import dev.diegobarrioh.forma.application.WeeklyBodySummaryService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Weekly body summary REST endpoint (FOR-97): {@code GET} {@code /api/v1/body/weekly-summary}.
 *
 * <p>Thin controller (ADR-001, ADR-005): it delegates to the FOR-21 {@link
 * WeeklyBodySummaryService} and maps the result to the delivery read model. No business logic and
 * no recomputation happen here — the honesty rules (null deltas, real {@code comparisonDays}) all
 * live in the domain {@code WeeklyBodySummary}.
 *
 * <p>Mounted under {@link ApiPaths#V1} as a sibling of {@link BodyMeasurementController}
 * (docs/api/body-measurements.md).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/body/weekly-summary")
public class WeeklyBodySummaryController {

  private final WeeklyBodySummaryService service;

  public WeeklyBodySummaryController(WeeklyBodySummaryService service) {
    this.service = service;
  }

  /** Returns the current weekly body-composition summary (FOR-21), computed on demand. */
  @GetMapping
  public WeeklyBodySummaryResponse weeklySummary() {
    return WeeklyBodySummaryResponse.from(service.currentSummary());
  }
}
