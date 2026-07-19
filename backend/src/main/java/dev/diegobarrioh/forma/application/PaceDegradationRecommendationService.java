package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.PaceDegradationRules;
import dev.diegobarrioh.forma.domain.Recommendation;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case that produces the running pace degradation recommendation (FOR-150 rule 4).
 *
 * <p>Reads the FOR-155 {@link WeeklyTrackingRecordService} weekly history and delegates to the pure
 * {@link PaceDegradationRules} domain evaluator (reframed to a pace-only signal — see that class's
 * javadoc for why heart rate was not added); stamps any recommendation with {@link
 * Instant#now(Clock)} from the injected clock so the result is deterministic under test. Computed
 * on demand — no persistence, mirroring the FOR-42/43/44 recommendation services.
 */
@Service
public class PaceDegradationRecommendationService {

  private final WeeklyTrackingRecordService trackingRecordService;
  private final Clock clock;

  public PaceDegradationRecommendationService(
      WeeklyTrackingRecordService trackingRecordService, Clock clock) {
    this.trackingRecordService = trackingRecordService;
    this.clock = clock;
  }

  /** Evaluates the weekly-tracking history for a week-over-week pace degradation. */
  public List<Recommendation> currentRecommendations() {
    return PaceDegradationRules.evaluate(trackingRecordService.list(), Instant.now(clock));
  }
}
