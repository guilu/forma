package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Running pace degradation rule (FOR-150 rule 4, epic FOR-148 "Personalizar FORMA a Diego", sheet
 * *Reglas*): turns the FOR-155 {@link WeeklyTrackingRecord} history into at most one {@code
 * RECOVERY} {@link Recommendation} when the recorded 4 km pace gets slower from one recorded week
 * to the next. Pure, framework-free domain logic (ADR-001); deterministic and fail-safe.
 *
 * <h2>Reframed from the original Excel rule (resolved design decision)</h2>
 *
 * The Excel rule reads "FC running alta: mismo ritmo, más ppm" (same pace, higher heart rate) — a
 * fatigue signal. FORMA deliberately does <b>not</b> capture heart rate: the *Seguimiento* sheet
 * (FOR-155) only has "Ritmo 4 km" (pace), not FC/ppm, and {@link WeeklyTrackingRecord}'s javadoc
 * ("Heart-rate field") explicitly defers adding one as an unrequested, speculative field. This rule
 * is therefore reframed to use the pace signal alone: running the same distance <b>slower</b> at
 * the same (implicit) effort, week over week, is read as the same underlying fatigue/recovery
 * signal the Excel rule intended ("dormir más / bajar intensidad").
 *
 * <h2>Rule</h2>
 *
 * <ul>
 *   <li>Take the two most recent weekly records that carry a non-null {@code pace4kmMinPerKm}
 *       (parsed as {@code mm:ss}). Unlike {@link BodyFatTrendRules} (rule 2), these two records
 *       need not be calendar-consecutive weeks — the rule only needs the last two pace
 *       observations, however far apart, mirroring the Excel's simple "same pace, more effort"
 *       comparison rather than a strict weekly cadence (spec FOR-150 tests.md: "needs ≥2 weekly
 *       records with pace").
 *   <li>If the later pace is slower (more seconds per km) than the earlier one, fire a {@code
 *       WARNING}.
 *   <li>Equal or faster pace, or fewer than two pace-bearing records, produces no recommendation.
 * </ul>
 */
public final class PaceDegradationRules {

  private PaceDegradationRules() {}

  /** Evaluates the pace trend, stamping any recommendation with {@code createdAt}. */
  public static List<Recommendation> evaluate(
      List<WeeklyTrackingRecord> records, Instant createdAt) {
    if (records == null) {
      return List.of();
    }

    List<WeeklyTrackingRecord> withPace =
        records.stream()
            .filter(record -> record.pace4kmMinPerKm() != null)
            .sorted(Comparator.comparingInt(WeeklyTrackingRecord::week))
            .toList();

    if (withPace.size() < 2) {
      return List.of();
    }

    WeeklyTrackingRecord previous = withPace.get(withPace.size() - 2);
    WeeklyTrackingRecord latest = withPace.get(withPace.size() - 1);

    int previousSeconds = paceToSeconds(previous.pace4kmMinPerKm());
    int latestSeconds = paceToSeconds(latest.pace4kmMinPerKm());

    if (latestSeconds <= previousSeconds) {
      return List.of();
    }

    return List.of(degrading(previous, latest, createdAt));
  }

  /** {@code mm:ss} to total seconds; format already validated by {@link WeeklyTrackingRecord}. */
  private static int paceToSeconds(String pace) {
    String[] parts = pace.split(":", 2);
    return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
  }

  private static Recommendation degrading(
      WeeklyTrackingRecord previous, WeeklyTrackingRecord latest, Instant createdAt) {
    String reason =
        String.format(
            Locale.ROOT,
            "El ritmo de 4 km pasa de %s a %s min/km entre la semana %d y la semana %d.",
            previous.pace4kmMinPerKm(),
            latest.pace4kmMinPerKm(),
            previous.week(),
            latest.week());
    return new Recommendation(
        createdAt,
        RecommendationCategory.RECOVERY,
        RecommendationSeverity.WARNING,
        "El ritmo de carrera empeora; prueba a dormir más o bajar la intensidad esta semana.",
        reason,
        "pace4kmMinPerKm");
  }
}
