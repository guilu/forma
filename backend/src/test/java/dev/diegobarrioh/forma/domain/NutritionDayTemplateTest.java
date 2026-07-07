package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link NutritionDayTemplate} (FOR-29). Plain JUnit 5 + AssertJ (ADR-007).
 * Covers creation, the constrained day type, macro-target storage, and construction validation.
 */
class NutritionDayTemplateTest {

  @Test
  @DisplayName("creates a valid day template with macro targets")
  void createsValidTemplate() {
    NutritionDayTemplate template =
        new NutritionDayTemplate(NutritionDayType.RUNNING, 2600, 160, 320, 70, "Día de carrera");

    assertThat(template.type()).isEqualTo(NutritionDayType.RUNNING);
    assertThat(template.targetCalories()).isEqualTo(2600);
    assertThat(template.targetProteinG()).isEqualTo(160);
    assertThat(template.targetCarbsG()).isEqualTo(320);
    assertThat(template.targetFatG()).isEqualTo(70);
  }

  @Test
  @DisplayName("day type is constrained to the known values")
  void dayTypeIsConstrained() {
    assertThat(NutritionDayType.values())
        .containsExactlyInAnyOrder(
            NutritionDayType.RUNNING, NutritionDayType.STRENGTH, NutritionDayType.REST);
  }

  @Test
  @DisplayName("distinct day types coexist with different carbohydrate targets")
  void restDayHasFewerCarbsThanRunningDay() {
    NutritionDayTemplate running =
        new NutritionDayTemplate(NutritionDayType.RUNNING, 2600, 160, 320, 70, null);
    NutritionDayTemplate rest =
        new NutritionDayTemplate(NutritionDayType.REST, 2200, 160, 200, 70, null);

    assertThat(rest.targetCarbsG()).isLessThan(running.targetCarbsG());
  }

  @Test
  @DisplayName("requires a non-null type")
  void requiresType() {
    assertThatThrownBy(() -> new NutritionDayTemplate(null, 2600, 160, 320, 70, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("type");
  }

  @Test
  @DisplayName("rejects non-positive macro targets")
  void rejectsNonPositiveTargets() {
    assertThatThrownBy(() -> new NutritionDayTemplate(NutritionDayType.REST, 0, 160, 200, 70, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("targetCalories");
    assertThatThrownBy(
            () -> new NutritionDayTemplate(NutritionDayType.REST, 2200, -1, 200, 70, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("targetProteinG");
  }
}
