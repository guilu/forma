package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlanRestartService} (design D5 of training-progression-and-logging, no
 * Spring — ADR-007): reanchoring the cycle is only valid once the account's plan reached its
 * terminal state, enforced server-side rather than trusted to the frontend hiding the restart CTA.
 */
class PlanRestartServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  private final FakePlanAcceptanceRepository acceptanceRepository =
      new FakePlanAcceptanceRepository();

  private PlanRestartService serviceAt(Instant now) {
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    WeeklyTrainingScheduleService scheduleService =
        new WeeklyTrainingScheduleService(
            new RunningPlanService(),
            new WorkoutTemplateService(),
            new FakeTrainingSessionStatusRepository(),
            () -> USER_ID,
            clock,
            acceptanceRepository);
    return new PlanRestartService(acceptanceRepository, scheduleService, () -> USER_ID, clock);
  }

  /**
   * The bug this fix exists for: without the guard, any authenticated request could restart an
   * in-progress plan and silently drop the visible week back to 1.
   */
  @Test
  void rejectsRestartingAPlanThatIsStillActive() {
    Instant acceptedAt = Instant.parse("2026-08-17T09:00:00Z");
    Instant fourWeeksLater = acceptedAt.plus(4 * 7, ChronoUnit.DAYS);
    acceptanceRepository.markAccepted(USER_ID, acceptedAt);
    PlanRestartService service = serviceAt(fourWeeksLater);

    assertThatThrownBy(service::restart).isInstanceOf(ConflictException.class);

    // The rejected call must not have reanchored the cycle.
    assertThat(acceptanceRepository.planStartedAt(USER_ID)).contains(acceptedAt);
  }

  /**
   * The exact boundary the guard exists to protect: 15 full weeks after acceptance is still {@code
   * Active(16)} — the plan's own last active week ({@link
   * dev.diegobarrioh.forma.domain.TrainingPlanProgress#since}'s {@code weekNumber > totalWeeks}
   * check, not {@code >=}). An off-by-one there would let a restart wipe out the final week of real
   * progress while every other test in this class still passed.
   */
  @Test
  void rejectsRestartingAPlanOnItsLastActiveWeek() {
    Instant acceptedAt = Instant.parse("2026-08-17T09:00:00Z");
    Instant fifteenWeeksLater = acceptedAt.plus(15 * 7, ChronoUnit.DAYS);
    acceptanceRepository.markAccepted(USER_ID, acceptedAt);
    PlanRestartService service = serviceAt(fifteenWeeksLater);

    assertThatThrownBy(service::restart).isInstanceOf(ConflictException.class);

    assertThat(acceptanceRepository.planStartedAt(USER_ID)).contains(acceptedAt);
  }

  @Test
  void rejectsRestartingAPlanThatWasNeverAccepted() {
    PlanRestartService service = serviceAt(Instant.parse("2026-08-17T09:00:00Z"));

    assertThatThrownBy(service::restart).isInstanceOf(ConflictException.class);
  }

  @Test
  void restartsACompletedPlanAndReanchorsTheCycleToNow() {
    Instant acceptedAt = Instant.parse("2026-08-17T09:00:00Z");
    Instant sixteenWeeksLater = acceptedAt.plus(16 * 7, ChronoUnit.DAYS);
    acceptanceRepository.markAccepted(USER_ID, acceptedAt);
    PlanRestartService service = serviceAt(sixteenWeeksLater);

    service.restart();

    assertThat(acceptanceRepository.planStartedAt(USER_ID)).contains(sixteenWeeksLater);
  }
}
