package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.RecoveryWarningRules;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case that produces the current recovery warnings (FOR-44).
 *
 * <p>Combines the FOR-40 {@link WeeklyCheckInService} (session counts) with the FOR-21 {@link
 * WeeklyBodySummaryService} (body-fat delta) and delegates to the pure {@link RecoveryWarningRules}
 * domain evaluator; stamps any warning with {@link Instant#now(Clock)} from the injected clock so
 * the result is deterministic under test. Computed on demand — no persistence and no HTTP endpoint
 * (FOR-45 exposes it), mirroring the other Insights services. Returns an empty list on a healthy or
 * dataless week (fail-safe).
 */
@Service
public class RecoveryWarningRecommendationService {

  private final WeeklyCheckInService checkInService;
  private final WeeklyBodySummaryService bodySummaryService;
  private final Clock clock;

  public RecoveryWarningRecommendationService(
      WeeklyCheckInService checkInService,
      WeeklyBodySummaryService bodySummaryService,
      Clock clock) {
    this.checkInService = checkInService;
    this.bodySummaryService = bodySummaryService;
    this.clock = clock;
  }

  /** Evaluates recovery signals for the current week into at most one warning. */
  public List<Recommendation> currentRecommendations() {
    return RecoveryWarningRules.evaluate(
        checkInService.currentCheckIn(), bodySummaryService.currentSummary(), Instant.now(clock));
  }
}
