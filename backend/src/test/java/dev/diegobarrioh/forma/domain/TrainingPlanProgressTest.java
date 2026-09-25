package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TrainingPlanProgress}: derives which week of the 16-week plan is showing
 * from the Monday the account accepted its plan and the Monday of "today", with no persisted
 * counter (design D1).
 */
class TrainingPlanProgressTest {

  private static final int TOTAL_WEEKS = 16;

  @Test
  void isWeekOneTheMondayOfAcceptance() {
    LocalDate acceptedMonday = LocalDate.of(2026, 8, 17);

    TrainingPlanProgress progress =
        TrainingPlanProgress.since(acceptedMonday, acceptedMonday, TOTAL_WEEKS);

    assertThat(progress).isEqualTo(new TrainingPlanProgress.Active(1));
  }

  @Test
  void isWeekFourThreeMondaysLater() {
    LocalDate acceptedMonday = LocalDate.of(2026, 8, 17);
    LocalDate currentMonday = acceptedMonday.plusWeeks(3);

    TrainingPlanProgress progress =
        TrainingPlanProgress.since(acceptedMonday, currentMonday, TOTAL_WEEKS);

    assertThat(progress).isEqualTo(new TrainingPlanProgress.Active(4));
  }

  @Test
  void isWeekThirteenTwelveMondaysLater() {
    LocalDate acceptedMonday = LocalDate.of(2026, 8, 17);
    LocalDate currentMonday = acceptedMonday.plusWeeks(12);

    TrainingPlanProgress progress =
        TrainingPlanProgress.since(acceptedMonday, currentMonday, TOTAL_WEEKS);

    assertThat(progress).isEqualTo(new TrainingPlanProgress.Active(13));
  }

  @Test
  void isWeekSixteenTheLastOfThePlan() {
    LocalDate acceptedMonday = LocalDate.of(2026, 8, 17);
    LocalDate currentMonday = acceptedMonday.plusWeeks(15);

    TrainingPlanProgress progress =
        TrainingPlanProgress.since(acceptedMonday, currentMonday, TOTAL_WEEKS);

    assertThat(progress).isEqualTo(new TrainingPlanProgress.Active(16));
  }

  /** Accepted Sunday inside week 1; the very next Monday is already week 2 (Monday boundary). */
  @Test
  void crossesIntoWeekTwoAtTheFollowingMondayNotAtOneWeekLater168Hours() {
    LocalDate acceptedWeekMonday = LocalDate.of(2026, 8, 10);
    LocalDate nextMonday = LocalDate.of(2026, 8, 17);

    TrainingPlanProgress progress =
        TrainingPlanProgress.since(acceptedWeekMonday, nextMonday, TOTAL_WEEKS);

    assertThat(progress).isEqualTo(new TrainingPlanProgress.Active(2));
  }

  @Test
  void isCompletedRightAfterWeekSixteen() {
    LocalDate acceptedMonday = LocalDate.of(2026, 8, 17);
    LocalDate currentMonday = acceptedMonday.plusWeeks(16);

    TrainingPlanProgress progress =
        TrainingPlanProgress.since(acceptedMonday, currentMonday, TOTAL_WEEKS);

    assertThat(progress).isEqualTo(new TrainingPlanProgress.Completed());
  }

  /** 30 weeks past acceptance stays completed rather than wrapping back to a week number. */
  @Test
  void staysCompletedThirtyWeeksLaterRatherThanWrappingAround() {
    LocalDate acceptedMonday = LocalDate.of(2026, 8, 17);
    LocalDate currentMonday = acceptedMonday.plusWeeks(30);

    TrainingPlanProgress progress =
        TrainingPlanProgress.since(acceptedMonday, currentMonday, TOTAL_WEEKS);

    assertThat(progress).isEqualTo(new TrainingPlanProgress.Completed());
  }
}
