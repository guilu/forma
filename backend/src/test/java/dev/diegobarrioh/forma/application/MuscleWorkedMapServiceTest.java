package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.application.MuscleWorkedMap.MuscleWorked;
import dev.diegobarrioh.forma.domain.MuscleLoad;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MuscleWorkedMapService} (FOR-136): resolves a session id to its strength
 * template via the real FOR-26 schedule + FOR-25 template + FOR-24 catalog services (no duplicated
 * resolution logic, no Spring — ADR-007).
 */
class MuscleWorkedMapServiceTest {

  private final WeeklyTrainingScheduleService scheduleService =
      new WeeklyTrainingScheduleService(
          new RunningPlanService(),
          new WorkoutTemplateService(),
          new FakeTrainingSessionStatusRepository(),
          () -> WeeklyTrainingScheduleServiceTest.USER_ID,
          java.time.Clock.systemUTC());
  private final MuscleWorkedMapService service =
      new MuscleWorkedMapService(
          scheduleService, new WorkoutTemplateService(), new ExerciseCatalogService());

  @Test
  void aggregatesThePushTemplateWithEscalatedLoadsForSharedMuscles() {
    // PUSH (FOR-154 real template): dumbbell-bench-press(pecho, tríceps, hombro anterior),
    // dumbbell-shoulder-press(hombro, tríceps), push-up(pecho, tríceps, hombro anterior),
    // lateral-raise(hombro lateral), plank(core, abdomen)
    // -> tríceps x3, pecho x2, hombro anterior x2 (HIGH); hombro, hombro lateral, core, abdomen x1
    // (MEDIUM).
    MuscleWorkedMap result = service.resolve("STRENGTH:PUSH");

    assertThat(result.sessionId()).isEqualTo("STRENGTH:PUSH");
    assertThat(result.muscles())
        .contains(
            new MuscleWorked("tríceps", MuscleLoad.HIGH),
            new MuscleWorked("pecho", MuscleLoad.HIGH),
            new MuscleWorked("hombro anterior", MuscleLoad.HIGH),
            new MuscleWorked("hombro", MuscleLoad.MEDIUM),
            new MuscleWorked("hombro lateral", MuscleLoad.MEDIUM));
  }

  @Test
  void aNonStrengthSessionReturnsAnEmptyMapNotAnError() {
    MuscleWorkedMap result = service.resolve("RUNNING:LONG_RUN");

    assertThat(result.sessionId()).isEqualTo("RUNNING:LONG_RUN");
    assertThat(result.muscles()).isEmpty();
  }

  @Test
  void anUnknownSessionIdIsRejected() {
    // FULL_BODY has no template on any day of the week, and the pre-V60 day-keyed ids are gone.
    assertThatThrownBy(() -> service.resolve("STRENGTH:FULL_BODY"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("STRENGTH:FULL_BODY");
    assertThatThrownBy(() -> service.resolve("MONDAY:STRENGTH"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("MONDAY:STRENGTH");
  }

  @Test
  void neverFabricatesAMuscleNotPresentInTheRealCatalogData() {
    MuscleWorkedMap result = service.resolve("STRENGTH:PULL");

    // PULL (FOR-154 real template): pull-up(dorsal, bíceps), dumbbell-row(dorsal, romboides,
    // bíceps), band-face-pull(deltoides posterior, trapecio), biceps-curl(bíceps),
    // rear-delt-fly(deltoides posterior) -> no new muscle names, just higher frequency.
    assertThat(result.muscles())
        .extracting(MuscleWorked::muscle)
        .containsExactlyInAnyOrder(
            "dorsal", "bíceps", "romboides", "deltoides posterior", "trapecio");
  }
}
