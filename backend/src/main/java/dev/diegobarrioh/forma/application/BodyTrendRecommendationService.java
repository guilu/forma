package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyTrendRules;
import dev.diegobarrioh.forma.domain.Recommendation;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case that produces the current body-trend recommendations (FOR-42).
 *
 * <p>Reads the FOR-21 {@link WeeklyBodySummaryService} and delegates to the pure {@link
 * BodyTrendRules} domain evaluator; stamps recommendations with {@link Instant#now(Clock)} from the
 * injected clock so the result is deterministic under test. Computed on demand — no persistence and
 * no HTTP endpoint (FOR-45 exposes it), mirroring the FOR-21/FOR-28/FOR-40 services.
 */
@Service
public class BodyTrendRecommendationService {

  private final WeeklyBodySummaryService bodySummaryService;
  private final Clock clock;

  public BodyTrendRecommendationService(WeeklyBodySummaryService bodySummaryService, Clock clock) {
    this.bodySummaryService = bodySummaryService;
    this.clock = clock;
  }

  /** Evaluates the body trend of the current weekly summary into recommendations. */
  public List<Recommendation> currentRecommendations() {
    return BodyTrendRules.evaluate(bodySummaryService.currentSummary(), Instant.now(clock));
  }
}
