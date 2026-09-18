package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

/**
 * Every label {@code TrainingAvailabilityStep.tsx} actually offers, resolved digit for digit — and
 * an unknown label rejected rather than silently dropped.
 */
class SpanishWeekdayLabelsTest {

  @Test
  void resolvesAllSevenWizardLabels() {
    assertThat(SpanishWeekdayLabels.fromLabel("Lunes")).isEqualTo(DayOfWeek.MONDAY);
    assertThat(SpanishWeekdayLabels.fromLabel("Martes")).isEqualTo(DayOfWeek.TUESDAY);
    assertThat(SpanishWeekdayLabels.fromLabel("Miércoles")).isEqualTo(DayOfWeek.WEDNESDAY);
    assertThat(SpanishWeekdayLabels.fromLabel("Jueves")).isEqualTo(DayOfWeek.THURSDAY);
    assertThat(SpanishWeekdayLabels.fromLabel("Viernes")).isEqualTo(DayOfWeek.FRIDAY);
    assertThat(SpanishWeekdayLabels.fromLabel("Sábado")).isEqualTo(DayOfWeek.SATURDAY);
    assertThat(SpanishWeekdayLabels.fromLabel("Domingo")).isEqualTo(DayOfWeek.SUNDAY);
  }

  @Test
  void rejectsAnUnknownLabelRatherThanDroppingIt() {
    assertThatThrownBy(() -> SpanishWeekdayLabels.fromLabel("lunes"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("lunes");
  }
}
