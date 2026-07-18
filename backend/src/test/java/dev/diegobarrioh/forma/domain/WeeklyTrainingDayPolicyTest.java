package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeeklyTrainingDayPolicy} (FOR-128): the single source of truth for the
 * deterministic MVP weekly training day classification, shared by {@code
 * WeeklyTrainingScheduleService} (FOR-26) and {@link NutritionDayTypeResolver} (FOR-128).
 */
class WeeklyTrainingDayPolicyTest {

  @Test
  void classifiesMondayWednesdayAndSaturdayAsRunning() {
    assertThat(WeeklyTrainingDayPolicy.classify(DayOfWeek.MONDAY))
        .isEqualTo(NutritionDayType.RUNNING);
    assertThat(WeeklyTrainingDayPolicy.classify(DayOfWeek.WEDNESDAY))
        .isEqualTo(NutritionDayType.RUNNING);
    assertThat(WeeklyTrainingDayPolicy.classify(DayOfWeek.SATURDAY))
        .isEqualTo(NutritionDayType.RUNNING);
  }

  @Test
  void classifiesTuesdayThursdayAndSundayAsStrength() {
    assertThat(WeeklyTrainingDayPolicy.classify(DayOfWeek.TUESDAY))
        .isEqualTo(NutritionDayType.STRENGTH);
    assertThat(WeeklyTrainingDayPolicy.classify(DayOfWeek.THURSDAY))
        .isEqualTo(NutritionDayType.STRENGTH);
    assertThat(WeeklyTrainingDayPolicy.classify(DayOfWeek.SUNDAY))
        .isEqualTo(NutritionDayType.STRENGTH);
  }

  @Test
  void classifiesFridayAsRest() {
    assertThat(WeeklyTrainingDayPolicy.classify(DayOfWeek.FRIDAY)).isEqualTo(NutritionDayType.REST);
  }

  @Test
  void strengthDaysMapEachDayToItsWorkoutType() {
    assertThat(WeeklyTrainingDayPolicy.strengthDays())
        .containsEntry(DayOfWeek.TUESDAY, WorkoutType.PUSH)
        .containsEntry(DayOfWeek.THURSDAY, WorkoutType.PULL)
        .containsEntry(DayOfWeek.SUNDAY, WorkoutType.LEGS)
        .hasSize(3);
  }

  @Test
  void runningDaysMatchTheDaysTheRunningPlanGeneratorActuallySchedules() {
    assertThat(WeeklyTrainingDayPolicy.runningDays())
        .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY);
    // Not duplicated: derived from the real generator output (FOR-23), not a hardcoded literal set.
    assertThat(
            RunningPlanGenerator.sixteenWeekPlan().stream()
                .map(RunningPlanSession::dayOfWeek)
                .distinct())
        .containsExactlyInAnyOrderElementsOf(WeeklyTrainingDayPolicy.runningDays());
  }

  @Test
  void everyDayOfWeekIsClassified() {
    for (DayOfWeek day : DayOfWeek.values()) {
      assertThat(WeeklyTrainingDayPolicy.classify(day)).isNotNull();
    }
  }
}
