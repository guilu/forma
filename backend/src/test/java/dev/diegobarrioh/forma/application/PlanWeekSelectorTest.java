package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.domain.MacroTargets;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanWeekSelectorTest {

  private static PlanDay day(int week, int day) {
    return new PlanDay(
        null, week, day, NutritionDayType.REST, MacroTargets.none(), null, List.of());
  }

  private static List<PlanDay> twoWeeks() {
    return List.of(
        day(1, 1), day(1, 2), day(1, 3), day(1, 4), day(1, 5), day(1, 6), day(1, 7), day(2, 1),
        day(2, 2), day(2, 3), day(2, 4), day(2, 5), day(2, 6), day(2, 7));
  }

  @Test
  void selectsOnlyThePlanWeekContainingToday() {
    List<PlanDay> selected =
        PlanWeekSelector.currentWeek(
            twoWeeks(), LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 12));

    assertThat(selected).extracting(PlanDay::weekNumber).containsOnly(2);
    assertThat(selected).extracting(PlanDay::dayNumber).containsExactly(1, 2, 3, 4, 5, 6, 7);
  }

  @Test
  void mondayAlreadyBelongsToTheNewPlanWeek() {
    List<PlanDay> selected =
        PlanWeekSelector.currentWeek(
            twoWeeks(), LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 10));

    assertThat(selected).extracting(PlanDay::weekNumber).containsOnly(2);
  }

  @Test
  void returnsNoDaysOutsideThePlanDateRange() {
    assertThat(
            PlanWeekSelector.currentWeek(
                twoWeeks(), LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 17)))
        .isEmpty();
  }

  @Test
  void aTemplateWithoutAStartDateHasNoCurrentCalendarWeek() {
    assertThat(PlanWeekSelector.currentWeek(twoWeeks(), null, LocalDate.of(2026, 8, 12))).isEmpty();
  }
}
