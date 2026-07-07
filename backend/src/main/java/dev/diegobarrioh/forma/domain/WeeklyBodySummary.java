package dev.diegobarrioh.forma.domain;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * A simple, rule-based weekly body-composition summary over {@link BodyMeasurement} history
 * (FOR-21). Framework-free domain value: latest weight/body fat/lean mass plus the change since the
 * previous measurement, and a short factual status message.
 *
 * <p>Honesty rules (spec FOR-21, docs/ui-guidelines.md):
 *
 * <ul>
 *   <li>Change fields are {@code null} (not {@code 0}) when there is no prior measurement to
 *       compare.
 *   <li>The comparison is against the <em>immediately previous</em> measurement, and {@link
 *       #comparisonDays} plus the message always state the actual number of days between them — so
 *       a gap longer than a week is never presented as if it were exactly a week.
 *   <li>The message is descriptive, never prescriptive or "gamified" (no recommendations — those
 *       are the separate Insights concern).
 * </ul>
 *
 * <p>Computed on demand from the measurement list; this story introduces no persisted entity. All
 * numeric fields are {@code null} when the data does not support them.
 *
 * @param latestWeightKg most recent weight, or null if there are no measurements
 * @param latestBodyFatPercentage most recent body fat %, or null
 * @param latestLeanMassKg most recent lean mass, or null
 * @param weeklyWeightChangeKg weight delta vs the previous measurement, or null if none
 * @param weeklyBodyFatChange body fat delta vs the previous measurement, or null if none
 * @param comparisonDays days between the two most recent measurements, or null if fewer than two
 * @param message short, factual human-readable status
 */
public record WeeklyBodySummary(
    Double latestWeightKg,
    Double latestBodyFatPercentage,
    Double latestLeanMassKg,
    Double weeklyWeightChangeKg,
    Double weeklyBodyFatChange,
    Integer comparisonDays,
    String message) {

  /**
   * Builds a summary from measurements ordered newest-first (the FOR-16 repository order). When
   * multiple measurements share the same instant, the first in the list counts as the latest.
   */
  public static WeeklyBodySummary from(List<BodyMeasurement> newestFirst) {
    if (newestFirst.isEmpty()) {
      return new WeeklyBodySummary(
          null, null, null, null, null, null, "Aún no hay mediciones para resumir.");
    }

    BodyMeasurement latest = newestFirst.get(0);
    Double latestWeight = latest.weightKg();
    Double latestBodyFat = latest.bodyFatPercentage();
    Double latestLean = latest.leanMassKg().orElse(null);

    if (newestFirst.size() == 1) {
      return new WeeklyBodySummary(
          latestWeight,
          latestBodyFat,
          latestLean,
          null,
          null,
          null,
          latestOnlyMessage(latestWeight, latestBodyFat, latestLean));
    }

    BodyMeasurement previous = newestFirst.get(1);
    int days = (int) ChronoUnit.DAYS.between(previous.measuredAt(), latest.measuredAt());
    Double weightChange = latest.weightKg() - previous.weightKg();
    Double bodyFatChange =
        (latestBodyFat != null && previous.bodyFatPercentage() != null)
            ? latestBodyFat - previous.bodyFatPercentage()
            : null;

    return new WeeklyBodySummary(
        latestWeight,
        latestBodyFat,
        latestLean,
        weightChange,
        bodyFatChange,
        days,
        changeMessage(latestWeight, latestBodyFat, latestLean, weightChange, bodyFatChange, days));
  }

  private static String latestOnlyMessage(Double weight, Double bodyFat, Double lean) {
    StringBuilder message = new StringBuilder("Última medición — ");
    message.append(latestValues(weight, bodyFat, lean));
    message.append(". Registra otra medición para ver el cambio.");
    return message.toString();
  }

  private static String changeMessage(
      Double weight,
      Double bodyFat,
      Double lean,
      Double weightChange,
      Double bodyFatChange,
      int days) {
    StringBuilder message = new StringBuilder();
    message.append(String.format(Locale.ROOT, "Peso %.1f kg", weight));
    if (weightChange != null) {
      message.append(String.format(Locale.ROOT, " (%+.1f kg en %d días)", weightChange, days));
    }
    if (bodyFat != null) {
      message.append(String.format(Locale.ROOT, ". Grasa corporal %.1f%%", bodyFat));
      if (bodyFatChange != null) {
        message.append(String.format(Locale.ROOT, " (%+.1f%%)", bodyFatChange));
      }
    }
    if (lean != null) {
      message.append(String.format(Locale.ROOT, ". Masa magra %.1f kg", lean));
    }
    message.append('.');
    return message.toString();
  }

  private static String latestValues(Double weight, Double bodyFat, Double lean) {
    StringBuilder values = new StringBuilder(String.format(Locale.ROOT, "Peso %.1f kg", weight));
    if (bodyFat != null) {
      values.append(String.format(Locale.ROOT, ", grasa %.1f%%", bodyFat));
    }
    if (lean != null) {
      values.append(String.format(Locale.ROOT, ", masa magra %.1f kg", lean));
    }
    return values.toString();
  }
}
