package dev.diegobarrioh.forma.domain;

import java.time.DayOfWeek;
import java.util.Map;

/**
 * Maps the onboarding wizard's Spanish weekday checkboxes to {@link DayOfWeek} (ADR-015 decision 6,
 * implementation plan slice 1).
 *
 * <p>{@code frontend/src/pages/onboarding/steps/TrainingAvailabilityStep.tsx} collects training
 * availability as the literal strings {@code 'Lunes'}, {@code 'Martes'}, … — a label, not a
 * vocabulary. The Spanish label never crosses the boundary (ADR-015 decision 6, applying ADR-004's
 * rule to FORMA's own frontend rather than to a provider); this is where it resolves to the
 * machine-readable side, at the delivery boundary, the day the wizard is redesigned to submit
 * through it (ADR-015 implementation plan slice 3). Framework-free by design: it is plain string
 * matching, not persistence.
 */
public final class SpanishWeekdayLabels {

  private static final Map<String, DayOfWeek> BY_LABEL =
      Map.of(
          "Lunes", DayOfWeek.MONDAY,
          "Martes", DayOfWeek.TUESDAY,
          "Miércoles", DayOfWeek.WEDNESDAY,
          "Jueves", DayOfWeek.THURSDAY,
          "Viernes", DayOfWeek.FRIDAY,
          "Sábado", DayOfWeek.SATURDAY,
          "Domingo", DayOfWeek.SUNDAY);

  private SpanishWeekdayLabels() {}

  /**
   * Resolves one of the wizard's seven Spanish day labels to its {@link DayOfWeek}.
   *
   * @throws IllegalArgumentException if the label is not one of the seven the wizard offers — never
   *     silently ignored, because a label the wizard did not send is a bug worth surfacing rather
   *     than a day quietly dropped from someone's training week
   */
  public static DayOfWeek fromLabel(String label) {
    DayOfWeek day = BY_LABEL.get(label);
    if (day == null) {
      throw new IllegalArgumentException("Unknown weekday label: " + label);
    }
    return day;
  }
}
