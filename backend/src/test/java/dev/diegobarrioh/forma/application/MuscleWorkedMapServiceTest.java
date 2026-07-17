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
  void aggregatesTheMondayPushTemplateWithEscalatedLoadsForSharedMuscles() {
    // PUSH: push-up(pecho, tríceps, hombro anterior), dumbbell-shoulder-press(hombro, tríceps),
    // bench-dip(tríceps, pecho) -> tríceps x3, pecho x2 (HIGH); hombro anterior, hombro x1
    // (MEDIUM).
    MuscleWorkedMap result = service.resolve("MONDAY:STRENGTH");

    assertThat(result.sessionId()).isEqualTo("MONDAY:STRENGTH");
    assertThat(result.muscles())
        .contains(
            new MuscleWorked("tríceps", MuscleLoad.HIGH),
            new MuscleWorked("pecho", MuscleLoad.HIGH),
            new MuscleWorked("hombro anterior", MuscleLoad.MEDIUM),
            new MuscleWorked("hombro", MuscleLoad.MEDIUM));
  }

  @Test
  void aNonStrengthSessionReturnsAnEmptyMapNotAnError() {
    MuscleWorkedMap result = service.resolve("SATURDAY:RUNNING");

    assertThat(result.sessionId()).isEqualTo("SATURDAY:RUNNING");
    assertThat(result.muscles()).isEmpty();
  }

  @Test
  void anUnknownSessionIdIsRejected() {
    assertThatThrownBy(() -> service.resolve("SUNDAY:STRENGTH"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("SUNDAY:STRENGTH");
  }

  @Test
  void neverFabricatesAMuscleNotPresentInTheRealCatalogData() {
    MuscleWorkedMap result = service.resolve("WEDNESDAY:STRENGTH");

    // PULL: pull-up(dorsal, bíceps), dumbbell-row(dorsal, romboides, bíceps),
    // band-face-pull(deltoides posterior, trapecio).
    assertThat(result.muscles())
        .extracting(MuscleWorked::muscle)
        .containsExactlyInAnyOrder(
            "dorsal", "bíceps", "romboides", "deltoides posterior", "trapecio");
  }
}
