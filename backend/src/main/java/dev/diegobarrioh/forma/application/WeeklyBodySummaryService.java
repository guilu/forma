package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyBodySummary;
import org.springframework.stereotype.Service;

/**
 * Application use case that produces the weekly body summary (FOR-21).
 *
 * <p>Reads measurements through the FOR-16 {@link BodyMeasurementRepository} port (newest-first)
 * and delegates the rule-based calculation to the {@link WeeklyBodySummary} domain value. Computed
 * on demand — no persisted summary entity (spec FOR-21). No HTTP endpoint is exposed by this story;
 * the result is available for later dashboard/API use.
 */
@Service
public class WeeklyBodySummaryService {

  private final BodyMeasurementRepository repository;

  public WeeklyBodySummaryService(BodyMeasurementRepository repository) {
    this.repository = repository;
  }

  /** Computes the current weekly summary from stored measurements. */
  public WeeklyBodySummary currentSummary() {
    return WeeklyBodySummary.from(repository.list());
  }
}
