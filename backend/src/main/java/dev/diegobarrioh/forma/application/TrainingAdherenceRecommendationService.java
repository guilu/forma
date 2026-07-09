package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.Recommendation;
import dev.diegobarrioh.forma.domain.TrainingAdherenceRules;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use case that produces the current training-adherence recommendations (FOR-43).
 *
 * <p>Reads the FOR-40 {@link WeeklyCheckInService} (which assembles the FOR-28 session counts) and
 * delegates to the pure {@link TrainingAdherenceRules} domain evaluator; stamps recommendations
 * with {@link Instant#now(Clock)} from the injected clock so the result is deterministic under
 * test. Computed on demand — no persistence and no HTTP endpoint (FOR-45 exposes it), mirroring the
 * FOR-21/FOR-28/FOR-40/FOR-42 services.
 */
@Service
public class TrainingAdherenceRecommendationService {

  private final WeeklyCheckInService checkInService;
  private final Clock clock;

  public TrainingAdherenceRecommendationService(WeeklyCheckInService checkInService, Clock clock) {
    this.checkInService = checkInService;
    this.clock = clock;
  }

  /** Evaluates the training adherence of the current weekly check-in into recommendations. */
  public List<Recommendation> currentRecommendations() {
    return TrainingAdherenceRules.evaluate(checkInService.currentCheckIn(), Instant.now(clock));
  }
}
