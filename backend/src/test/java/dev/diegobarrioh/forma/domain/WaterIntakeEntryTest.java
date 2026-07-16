package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Domain unit tests for {@link WaterIntakeEntry} (FOR-130, hydration slice of FOR-102):
 * construction and its positive-volume invariant. Plain JUnit 5 + AssertJ (ADR-007), mirroring
 * {@code MealLogEntryTest} (FOR-127).
 */
class WaterIntakeEntryTest {

  private static final LocalDate DAY = LocalDate.of(2026, 7, 15);

  @Test
  void storesTheDateAndVolume() {
    WaterIntakeEntry entry = new WaterIntakeEntry(DAY, 500.0);

    assertThat(entry.date()).isEqualTo(DAY);
    assertThat(entry.volumeMl()).isEqualTo(500.0);
  }

  @Test
  void rejectsANullDate() {
    assertThatThrownBy(() -> new WaterIntakeEntry(null, 500.0))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsAZeroVolume() {
    assertThatThrownBy(() -> new WaterIntakeEntry(DAY, 0.0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsANegativeVolume() {
    assertThatThrownBy(() -> new WaterIntakeEntry(DAY, -100.0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
