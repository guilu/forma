package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Sustained body-fat trend rule (FOR-150 rule 2, epic FOR-148 "Personalizar FORMA a Diego", sheet
 * *Reglas*): turns the FOR-155 {@link WeeklyTrackingRecord} history into at most one {@code BODY}
 * {@link Recommendation} when body fat has risen for **2 consecutive weeks in a row** ("2–3 semanas
 * seguidas" per the Excel). Pure, framework-free domain logic (ADR-001); deterministic and
 * fail-safe — insufficient or gapped history never produces a false trigger.
 *
 * <p><b>Why {@link WeeklyTrackingRecord} and not {@link WeeklyBodySummary}</b>: the existing FOR-42
 * {@link BodyTrendRules} only ever compares the two most recent {@link
 * dev.diegobarrioh.forma.domain.BodyMeasurement} entries (a single delta) — it has no notion of a
 * multi-week streak. The FOR-150 spec (Data Model Notes) explicitly resolves this by reading the
 * multi-week history that the FOR-155 *Seguimiento* weekly records provide, one {@code
 * bodyFatPercentage} value per week.
 *
 * <h2>Rule</h2>
 *
 * <ul>
 *   <li>Take the three most recent weeks (by {@link WeeklyTrackingRecord#week()}) present in the
 *       supplied history.
 *   <li>They must be <b>calendar-consecutive</b> (week N, N-1, N-2 with no gap) — a gap means the
 *       trend was not observed "seguidas" (in a row), so it does not count (spec Edge Cases:
 *       "insufficient history → no recommendation, not a false trigger").
 *   <li>All three must carry a {@code bodyFatPercentage} value; a missing value anywhere in the
 *       window breaks the chain.
 *   <li>Body fat must strictly increase on both transitions (N-2 &lt; N-1 &lt; N) — two consecutive
 *       rises, matching the Excel's "2 semanas seguidas" minimum.
 * </ul>
 *
 * <p>Fewer than three records, or any of the above conditions unmet, yields an empty list — no
 * fabricated recommendation (existing convention, mirrors {@link RecoveryWarningRules}).
 */
public final class BodyFatTrendRules {

  private BodyFatTrendRules() {}

  /** Evaluates the sustained body-fat trend, stamping any recommendation with {@code createdAt}. */
  public static List<Recommendation> evaluate(
      List<WeeklyTrackingRecord> records, Instant createdAt) {
    if (records == null || records.size() < 3) {
      return List.of();
    }

    List<WeeklyTrackingRecord> ascending =
        records.stream().sorted(Comparator.comparingInt(WeeklyTrackingRecord::week)).toList();

    int n = ascending.size();
    WeeklyTrackingRecord latest = ascending.get(n - 1);
    WeeklyTrackingRecord middle = ascending.get(n - 2);
    WeeklyTrackingRecord earliest = ascending.get(n - 3);

    boolean consecutiveWeeks =
        latest.week() == middle.week() + 1 && middle.week() == earliest.week() + 1;
    if (!consecutiveWeeks) {
      return List.of();
    }

    Double bfEarliest = earliest.bodyFatPercentage();
    Double bfMiddle = middle.bodyFatPercentage();
    Double bfLatest = latest.bodyFatPercentage();
    if (bfEarliest == null || bfMiddle == null || bfLatest == null) {
      return List.of();
    }

    boolean sustainedRise = bfEarliest < bfMiddle && bfMiddle < bfLatest;
    if (!sustainedRise) {
      return List.of();
    }

    return List.of(sustainedRise(earliest, middle, latest, createdAt));
  }

  private static Recommendation sustainedRise(
      WeeklyTrackingRecord earliest,
      WeeklyTrackingRecord middle,
      WeeklyTrackingRecord latest,
      Instant createdAt) {
    String reason =
        String.format(
            Locale.ROOT,
            "La grasa corporal sube de forma sostenida durante 2 semanas seguidas (semana %d: %.1f%% "
                + "→ semana %d: %.1f%% → semana %d: %.1f%%).",
            earliest.week(),
            earliest.bodyFatPercentage(),
            middle.week(),
            middle.bodyFatPercentage(),
            latest.week(),
            latest.bodyFatPercentage());
    return new Recommendation(
        createdAt,
        RecommendationCategory.BODY,
        RecommendationSeverity.ACTION,
        "La grasa corporal sube varias semanas seguidas; recorta unas 100 kcal al día como ajuste mínimo.",
        reason,
        "bodyFatPercentage");
  }
}
