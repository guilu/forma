package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.diegobarrioh.forma.application.MuscleWorkedMap.MuscleWorked;
import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleServiceTest.FakeStatusRepository;
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
          new RunningPlanService(), new WorkoutTemplateService(), new FakeStatusRepository());
  private final MuscleWorkedMapService service =
      new MuscleWorkedMapService(
          scheduleService, new WorkoutTemplateService(), new ExerciseCatalogService());

  @Test
  void aggregatesTheTuesdayPushTemplateWithEscalatedLoadsForSharedMuscles() {
    // PUSH (FOR-154 real template): dumbbell-bench-press(pecho, tríceps, hombro anterior),
    // dumbbell-shoulder-press(hombro, tríceps), push-up(pecho, tríceps, hombro anterior),
    // lateral-raise(hombro lateral), plank(core, abdomen)
    // -> tríceps x3, pecho x2, hombro anterior x2 (HIGH); hombro, hombro lateral, core, abdomen x1
    // (MEDIUM).
    MuscleWorkedMap result = service.resolve("TUESDAY:STRENGTH");

    assertThat(result.sessionId()).isEqualTo("TUESDAY:STRENGTH");
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
    MuscleWorkedMap result = service.resolve("SATURDAY:RUNNING");

    assertThat(result.sessionId()).isEqualTo("SATURDAY:RUNNING");
    assertThat(result.muscles()).isEmpty();
  }

  @Test
  void anUnknownSessionIdIsRejected() {
    // Monday is now a RUNNING day (FOR-151), so "MONDAY:STRENGTH" no longer resolves.
    assertThatThrownBy(() -> service.resolve("MONDAY:STRENGTH"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("MONDAY:STRENGTH");
  }

  @Test
  void neverFabricatesAMuscleNotPresentInTheRealCatalogData() {
    MuscleWorkedMap result = service.resolve("THURSDAY:STRENGTH");

    // PULL (FOR-154 real template): pull-up(dorsal, bíceps), dumbbell-row(dorsal, romboides,
    // bíceps), band-face-pull(deltoides posterior, trapecio), biceps-curl(bíceps),
    // rear-delt-fly(deltoides posterior) -> no new muscle names, just higher frequency.
    assertThat(result.muscles())
        .extracting(MuscleWorked::muscle)
        .containsExactlyInAnyOrder(
            "dorsal", "bíceps", "romboides", "deltoides posterior", "trapecio");
  }
}
