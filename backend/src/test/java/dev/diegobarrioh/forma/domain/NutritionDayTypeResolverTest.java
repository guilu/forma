package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.RunningPlanService;
import dev.diegobarrioh.forma.application.TrainingSessionStatusRepository;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule;
import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService;
import dev.diegobarrioh.forma.application.WorkoutTemplateService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NutritionDayTypeResolver} (FOR-128 spec/tests.md "Resolver Tests"): a
 * calendar date resolves to the {@link NutritionDayType} of its {@link DayOfWeek} per the shared
 * {@link WeeklyTrainingDayPolicy}.
 */
class NutritionDayTypeResolverTest {

  // Fixture dates (FOR-151: Diego's real plan): a Wednesday (RUNNING), a Tuesday (STRENGTH), a
  // Friday (REST).
  private static final LocalDate A_WEDNESDAY = LocalDate.of(2026, 7, 15);
  private static final LocalDate A_TUESDAY = LocalDate.of(2026, 7, 14);
  private static final LocalDate A_THURSDAY = LocalDate.of(2026, 7, 16);
  private static final LocalDate A_SATURDAY = LocalDate.of(2026, 7, 18);
  private static final LocalDate A_MONDAY = LocalDate.of(2026, 7, 13);
  private static final LocalDate A_FRIDAY = LocalDate.of(2026, 7, 17);
  private static final LocalDate A_SUNDAY = LocalDate.of(2026, 7, 19);

  @Test
  void resolvesMondayWednesdayAndSaturdayToRunning() {
    assertThat(NutritionDayTypeResolver.resolve(A_MONDAY)).isEqualTo(NutritionDayType.RUNNING);
    assertThat(NutritionDayTypeResolver.resolve(A_WEDNESDAY)).isEqualTo(NutritionDayType.RUNNING);
    assertThat(NutritionDayTypeResolver.resolve(A_SATURDAY)).isEqualTo(NutritionDayType.RUNNING);
  }

  @Test
  void resolvesTuesdayThursdayAndSundayToStrength() {
    assertThat(NutritionDayTypeResolver.resolve(A_TUESDAY)).isEqualTo(NutritionDayType.STRENGTH);
    assertThat(NutritionDayTypeResolver.resolve(A_THURSDAY)).isEqualTo(NutritionDayType.STRENGTH);
    assertThat(NutritionDayTypeResolver.resolve(A_SUNDAY)).isEqualTo(NutritionDayType.STRENGTH);
  }

  @Test
  void resolvesFridayToRest() {
    assertThat(NutritionDayTypeResolver.resolve(A_FRIDAY)).isEqualTo(NutritionDayType.REST);
  }

  @Test
  void agreesWithWeeklyTrainingScheduleServiceOnEveryDayOfTheWeek() {
    // Same day-kind source (FOR-128): a policy change updates both, so they can never drift.
    WeeklyTrainingScheduleService scheduleService =
        new WeeklyTrainingScheduleService(
            new RunningPlanService(), new WorkoutTemplateService(), new FakeStatusRepository());
    Map<DayOfWeek, WeeklyTrainingSchedule.TrainingDay> byDay =
        scheduleService.currentWeek().days().stream()
            .collect(
                Collectors.toMap(
                    WeeklyTrainingSchedule.TrainingDay::dayOfWeek, Function.identity()));

    for (DayOfWeek day : DayOfWeek.values()) {
      NutritionDayType resolved = WeeklyTrainingDayPolicy.classify(day);
      WeeklyTrainingSchedule.TrainingDay trainingDay = byDay.get(day);
      if (resolved == NutritionDayType.REST) {
        assertThat(trainingDay.isRest()).as("%s should be a rest day", day).isTrue();
      } else {
        assertThat(trainingDay.isRest()).as("%s should not be a rest day", day).isFalse();
        String expectedKind = resolved == NutritionDayType.RUNNING ? "RUNNING" : "STRENGTH";
        assertThat(trainingDay.entries())
            .as("%s entries should be of kind %s", day, expectedKind)
            .allSatisfy(entry -> assertThat(entry.kind()).isEqualTo(expectedKind));
      }
    }
  }

  /** Same MVP mapping documented in spec FOR-128 edge cases: running/strength never overlap. */
  @Test
  void runningAndStrengthDaysNeverOverlapUnderTheMvpPolicy() {
    for (DayOfWeek day : WeeklyTrainingDayPolicy.runningDays()) {
      assertThat(WeeklyTrainingDayPolicy.strengthDays()).doesNotContainKey(day);
    }
  }

  /**
   * In-memory {@link TrainingSessionStatusRepository}, matching {@code
   * WeeklyTrainingScheduleServiceTest}.
   */
  private static final class FakeStatusRepository implements TrainingSessionStatusRepository {
    private final Map<String, dev.diegobarrioh.forma.application.StoredSessionStatus> stored =
        new HashMap<>();

    @Override
    public Map<String, dev.diegobarrioh.forma.application.StoredSessionStatus> findAll() {
      return stored;
    }

    @Override
    public void upsert(String sessionId, SessionStatus status, String notes) {
      stored.put(
          sessionId,
          new dev.diegobarrioh.forma.application.StoredSessionStatus(sessionId, status, notes));
    }
  }
}
