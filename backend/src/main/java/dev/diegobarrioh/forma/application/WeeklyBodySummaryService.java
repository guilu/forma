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
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V30): resolves the caller's account id via
 * {@link CurrentUserProvider} instead of reading every account's measurements (this "gap table" had
 * ZERO owner-scoping before this slice).
 */
@Service
public class WeeklyBodySummaryService {

  private final BodyMeasurementRepository repository;
  private final CurrentUserProvider currentUserProvider;

  public WeeklyBodySummaryService(
      BodyMeasurementRepository repository, CurrentUserProvider currentUserProvider) {
    this.repository = repository;
    this.currentUserProvider = currentUserProvider;
  }

  /** Computes the caller's current weekly summary from their stored measurements. */
  public WeeklyBodySummary currentSummary() {
    return WeeklyBodySummary.from(repository.list(currentUserProvider.currentUserId()));
  }
}
