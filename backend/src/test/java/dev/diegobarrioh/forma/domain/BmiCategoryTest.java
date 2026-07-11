package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for the {@link BmiCategory} classifier (FOR-101).
 *
 * <p>Plain JUnit 5 + AssertJ, no Spring context (ADR-007). Covers one known value per band, every
 * documented boundary and the null-safety edge case from the FOR-101 test plan.
 */
class BmiCategoryTest {

  @Nested
  @DisplayName("known values per band")
  class KnownValues {

    @Test
    @DisplayName("classifies a clearly bajo peso value")
    void classifiesBajoPeso() {
      assertThat(BmiCategory.classify(16.0)).isEqualTo(BmiCategory.BAJO_PESO);
    }

    @Test
    @DisplayName("classifies a clearly saludable value")
    void classifiesSaludable() {
      assertThat(BmiCategory.classify(22.7)).isEqualTo(BmiCategory.SALUDABLE);
    }

    @Test
    @DisplayName("classifies a clearly sobrepeso value")
    void classifiesSobrepeso() {
      assertThat(BmiCategory.classify(27.5)).isEqualTo(BmiCategory.SOBREPESO);
    }

    @Test
    @DisplayName("classifies a clearly obesidad value")
    void classifiesObesidad() {
      assertThat(BmiCategory.classify(33.0)).isEqualTo(BmiCategory.OBESIDAD);
    }
  }

  @Nested
  @DisplayName("boundary values land in exactly one band")
  class Boundaries {

    @Test
    @DisplayName("18.5 is saludable (lower bound inclusive), not bajo peso")
    void lowerBoundOfSaludableIsInclusive() {
      assertThat(BmiCategory.classify(18.5)).isEqualTo(BmiCategory.SALUDABLE);
    }

    @Test
    @DisplayName("just below 18.5 is bajo peso (upper bound exclusive)")
    void justBelowSaludableIsBajoPeso() {
      assertThat(BmiCategory.classify(18.49)).isEqualTo(BmiCategory.BAJO_PESO);
    }

    @Test
    @DisplayName("25.0 is sobrepeso (lower bound inclusive), not saludable")
    void lowerBoundOfSobrepesoIsInclusive() {
      assertThat(BmiCategory.classify(25.0)).isEqualTo(BmiCategory.SOBREPESO);
    }

    @Test
    @DisplayName("just below 25.0 is saludable (upper bound exclusive)")
    void justBelowSobrepesoIsSaludable() {
      assertThat(BmiCategory.classify(24.99)).isEqualTo(BmiCategory.SALUDABLE);
    }

    @Test
    @DisplayName("30.0 is obesidad (lower bound inclusive), not sobrepeso")
    void lowerBoundOfObesidadIsInclusive() {
      assertThat(BmiCategory.classify(30.0)).isEqualTo(BmiCategory.OBESIDAD);
    }

    @Test
    @DisplayName("just below 30.0 is sobrepeso (upper bound exclusive)")
    void justBelowObesidadIsSobrepeso() {
      assertThat(BmiCategory.classify(29.99)).isEqualTo(BmiCategory.SOBREPESO);
    }
  }

  @Test
  @DisplayName("null bmi yields null category, never a fabricated one")
  void nullBmiYieldsNullCategory() {
    assertThat(BmiCategory.classify(null)).isNull();
  }
}
