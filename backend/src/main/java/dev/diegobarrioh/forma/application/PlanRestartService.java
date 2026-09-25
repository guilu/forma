package dev.diegobarrioh.forma.application;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Starts a new 16-week cycle for an account that already has one (design D5 of
 * training-progression-and-logging).
 *
 * <p>Deliberately not {@link PlanActivationService#accept()}: that method is insert-if-absent by
 * design (accepting twice keeps the first instant, ADR the class itself documents), which is
 * exactly the behavior a restart must NOT have. This service writes through {@link
 * PlanAcceptanceRepository#restartCycle}, a separate column ({@code cycle_started_at}, V66) that
 * leaves the original {@code accepted_at} untouched.
 */
@Service
public class PlanRestartService {

  private final PlanAcceptanceRepository acceptances;
  private final CurrentUserProvider currentUserProvider;
  private final Clock clock;

  public PlanRestartService(
      PlanAcceptanceRepository acceptances, CurrentUserProvider currentUserProvider, Clock clock) {
    this.acceptances = acceptances;
    this.currentUserProvider = currentUserProvider;
    this.clock = clock;
  }

  /**
   * Reanchors this account's plan cycle to now, so {@link WeeklyTrainingScheduleService} derives
   * week 1 again on the next read. Uses the injected {@link Clock} rather than {@link
   * Instant#now()} — the same discipline {@link WeeklyTrainingScheduleService} follows — so tests
   * can pin "now" and production stays consistent with the rest of the plan-week derivation.
   */
  public void restart() {
    acceptances.restartCycle(currentUserProvider.currentUserId(), Instant.now(clock));
  }
}
