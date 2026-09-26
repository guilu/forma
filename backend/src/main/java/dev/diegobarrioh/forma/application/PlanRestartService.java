package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.TrainingPlanProgress;
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
 *
 * <p>Restarting is only valid for a plan that already reached its terminal state ({@link
 * TrainingPlanProgress.Completed}). This is enforced here, not just by the frontend hiding the
 * restart CTA outside that state (post-review fix): a client that called the endpoint directly
 * while the plan was still active would otherwise silently reset a visible week's progress. The
 * precondition is read through {@link WeeklyTrainingScheduleService#currentProgress()} rather than
 * re-deriving it from {@link PlanAcceptanceRepository} a second way, keeping that service the
 * single portero (design D2).
 */
@Service
public class PlanRestartService {

  private final PlanAcceptanceRepository acceptances;
  private final WeeklyTrainingScheduleService scheduleService;
  private final CurrentUserProvider currentUserProvider;
  private final Clock clock;

  public PlanRestartService(
      PlanAcceptanceRepository acceptances,
      WeeklyTrainingScheduleService scheduleService,
      CurrentUserProvider currentUserProvider,
      Clock clock) {
    this.acceptances = acceptances;
    this.scheduleService = scheduleService;
    this.currentUserProvider = currentUserProvider;
    this.clock = clock;
  }

  /**
   * Reanchors this account's plan cycle to now, so {@link WeeklyTrainingScheduleService} derives
   * week 1 again on the next read. Uses the injected {@link Clock} rather than {@link
   * Instant#now()} — the same discipline {@link WeeklyTrainingScheduleService} follows — so tests
   * can pin "now" and production stays consistent with the rest of the plan-week derivation.
   *
   * @throws ConflictException if the account's plan has not reached {@link
   *     TrainingPlanProgress.Completed} yet — restarting is a use case for closing a finished
   *     cycle, not a way to jump an active one back to week 1.
   */
  public void restart() {
    if (!(scheduleService.currentProgress() instanceof TrainingPlanProgress.Completed)) {
      throw new ConflictException("El plan debe estar completado para poder reiniciar el ciclo.");
    }
    acceptances.restartCycle(currentUserProvider.currentUserId(), Instant.now(clock));
  }
}
