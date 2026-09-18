package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The explicit direction question (ADR-015 decision 5, amended) maps 1:1 onto the {@link
 * PlanObjective} that carries the arithmetic factor {@link EnergyRequirement#of} consumes. {@link
 * PlanObjective#HEALTHY_EATING} has no {@link PlanDirection} that resolves to it — see the enum's
 * javadoc for why.
 */
class PlanDirectionTest {

  @Test
  void loseFatResolvesToWeightLossWithItsDeficitFactor() {
    assertThat(PlanDirection.LOSE_FAT.toPlanObjective()).isEqualTo(PlanObjective.WEIGHT_LOSS);
    assertThat(PlanDirection.LOSE_FAT.toPlanObjective().factor()).isEqualTo(0.80);
  }

  @Test
  void gainMuscleResolvesToMuscleGainWithItsSurplusFactor() {
    assertThat(PlanDirection.GAIN_MUSCLE.toPlanObjective()).isEqualTo(PlanObjective.MUSCLE_GAIN);
    assertThat(PlanDirection.GAIN_MUSCLE.toPlanObjective().factor()).isEqualTo(1.10);
  }

  @Test
  void maintainResolvesToMaintenanceWithNoAdjustment() {
    assertThat(PlanDirection.MAINTAIN.toPlanObjective()).isEqualTo(PlanObjective.MAINTENANCE);
    assertThat(PlanDirection.MAINTAIN.toPlanObjective().factor()).isEqualTo(1.0);
  }

  @Test
  void everyDirectionResolvesToADistinctPlanObjective() {
    long distinct =
        java.util.Arrays.stream(PlanDirection.values())
            .map(PlanDirection::toPlanObjective)
            .distinct()
            .count();

    assertThat(distinct).isEqualTo(PlanDirection.values().length);
  }
}
