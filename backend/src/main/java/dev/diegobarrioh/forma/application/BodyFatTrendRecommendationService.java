package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyFatTrendRules;
import dev.diegobarrioh.forma.domain.Recommendation;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case that produces the sustained body-fat trend recommendation (FOR-150 rule 2).
 *
 * <p>Reads the FOR-155 {@link WeeklyTrackingRecordService} weekly history and delegates to the pure
 * {@link BodyFatTrendRules} domain evaluator; stamps any recommendation with {@link
 * Instant#now(Clock)} from the injected clock so the result is deterministic under test. Computed
 * on demand — no persistence, mirroring the FOR-42/43/44 recommendation services.
 */
@Service
public class BodyFatTrendRecommendationService {

  private final WeeklyTrackingRecordService trackingRecordService;
  private final Clock clock;

  public BodyFatTrendRecommendationService(
      WeeklyTrackingRecordService trackingRecordService, Clock clock) {
    this.trackingRecordService = trackingRecordService;
    this.clock = clock;
  }

  /** Evaluates the weekly-tracking history for a sustained body-fat rise. */
  public List<Recommendation> currentRecommendations() {
    return BodyFatTrendRules.evaluate(trackingRecordService.list(), Instant.now(clock));
  }
}
