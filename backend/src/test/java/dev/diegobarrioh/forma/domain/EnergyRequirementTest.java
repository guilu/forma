package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Mifflin-St Jeor, the activity factor and the objective adjustment.
 *
 * <p>The first case is the mockup's own worked example, digit for digit. A calculator whose numbers
 * do not match the screen that promises them is worse than no calculator.
 */
class EnergyRequirementTest {

  /** Hombre, 45 años, 75 kg, 182 cm, moderado — el ejemplo del mockup: GEB 1668, GET 2585. */
  @Test
  void matchesTheWorkedExampleOnTheScreen() {
    EnergyRequirement requirement =
        EnergyRequirement.of(
            Sex.MALE, 45, 75, 182, ActivityLevel.MODERATE, PlanObjective.MAINTENANCE);

    assertThat(requirement.basalKcal()).isEqualTo(1668);
    assertThat(requirement.activityFactor()).isEqualTo(1.55);
    assertThat(requirement.dailyKcal()).isEqualTo(2585);
    assertThat(requirement.planKcal()).isEqualTo(2585);
  }

  /** The same body, as a woman: the formula's two constants differ by 166 kcal. */
  @Test
  void appliesTheFemaleConstant() {
    EnergyRequirement requirement =
        EnergyRequirement.of(
            Sex.FEMALE, 45, 75, 182, ActivityLevel.MODERATE, PlanObjective.MAINTENANCE);

    assertThat(requirement.basalKcal()).isEqualTo(1502); // 1667,5 − 166
  }

  /**
   * Rounded once, at the end.
   *
   * <p>1667,5 × 1,55 is 2584,6 and rounds to 2585. Rounding the basal figure first gives 1668 ×
   * 1,55 = 2585,4, which happens to land on the same number here and would not elsewhere.
   */
  @Test
  void multipliesBeforeRounding() {
    EnergyRequirement requirement =
        EnergyRequirement.of(
            Sex.MALE, 45, 75, 182, ActivityLevel.MODERATE, PlanObjective.MAINTENANCE);

    assertThat(requirement.dailyKcal()).isEqualTo(2585);
  }

  /** The objective is the third step, and it moves the plan without moving what the body spends. */
  @Test
  void adjustsForTheObjectiveWithoutTouchingTheDailyFigure() {
    EnergyRequirement losing =
        EnergyRequirement.of(
            Sex.MALE, 45, 75, 182, ActivityLevel.MODERATE, PlanObjective.WEIGHT_LOSS);

    assertThat(losing.dailyKcal()).isEqualTo(2585);
    assertThat(losing.planKcal()).isEqualTo(2068); // 2584,6 × 0,80
  }

  @Test
  void addsASurplusForMuscleGain() {
    EnergyRequirement gaining =
        EnergyRequirement.of(
            Sex.MALE, 45, 75, 182, ActivityLevel.MODERATE, PlanObjective.MUSCLE_GAIN);

    assertThat(gaining.planKcal()).isGreaterThan(gaining.dailyKcal());
  }

  /** Eating well is not eating differently in amount; only the food changes. */
  @Test
  void leavesTheRequirementAloneForHealthyEating() {
    EnergyRequirement healthy =
        EnergyRequirement.of(
            Sex.MALE, 45, 75, 182, ActivityLevel.MODERATE, PlanObjective.HEALTHY_EATING);

    assertThat(healthy.planKcal()).isEqualTo(healthy.dailyKcal());
  }

  /** The five factors the screen shows, on the enum rather than in the screen. */
  @Test
  void carriesTheFiveActivityFactors() {
    assertThat(ActivityLevel.SEDENTARY.factor()).isEqualTo(1.2);
    assertThat(ActivityLevel.LIGHT.factor()).isEqualTo(1.375);
    assertThat(ActivityLevel.MODERATE.factor()).isEqualTo(1.55);
    assertThat(ActivityLevel.ACTIVE.factor()).isEqualTo(1.725);
    assertThat(ActivityLevel.VERY_ACTIVE.factor()).isEqualTo(1.9);
  }

  /**
   * A sex the formula does not define takes the midpoint, and it is between the other two.
   *
   * <p>Asserted rather than assumed: it is a decision, and a decision nobody can see is a decision
   * nobody can argue with.
   */
  @Test
  void putsAnUndefinedSexBetweenTheTwoTheFormulaKnows() {
    int male =
        EnergyRequirement.of(
                Sex.MALE, 45, 75, 182, ActivityLevel.MODERATE, PlanObjective.MAINTENANCE)
            .basalKcal();
    int female =
        EnergyRequirement.of(
                Sex.FEMALE, 45, 75, 182, ActivityLevel.MODERATE, PlanObjective.MAINTENANCE)
            .basalKcal();
    int other =
        EnergyRequirement.of(
                Sex.OTHER, 45, 75, 182, ActivityLevel.MODERATE, PlanObjective.MAINTENANCE)
            .basalKcal();

    assertThat(other).isBetween(female, male);
  }

  @Test
  void refusesFiguresThatCannotDescribeAPerson() {
    assertThatThrownBy(
            () ->
                EnergyRequirement.of(
                    Sex.MALE, 0, 75, 182, ActivityLevel.MODERATE, PlanObjective.MAINTENANCE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ageYears");
    assertThatThrownBy(
            () ->
                EnergyRequirement.of(
                    Sex.MALE, 45, 0, 182, ActivityLevel.MODERATE, PlanObjective.MAINTENANCE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("weightKg");
  }
}
