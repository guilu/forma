package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Conservative, explainable body-trend rules (FOR-42): turn a {@link WeeklyBodySummary} (FOR-21)
 * into at most one {@code BODY} {@link Recommendation}. Pure, framework-free domain logic
 * (ADR-001); deterministic — the same trend always yields the same recommendation.
 *
 * <p>Reads the FOR-21 weekly deltas ({@code weeklyWeightChangeKg}, {@code weeklyBodyFatChange}) and
 * {@code comparisonDays}. The FOR-40 {@link WeeklyCheckIn} deliberately does not carry these
 * deltas, so the rules read the summary directly (see FOR-40/FOR-41 decisions).
 *
 * <h2>Thresholds (conservative; documented per spec FOR-42 Open Questions)</h2>
 *
 * <ul>
 *   <li><b>Excessive weight drop</b>: losing more than {@link #WEEKLY_DROP_LIMIT_PCT}% of body
 *       weight per week. The raw delta is normalized to a weekly rate using the actual {@code
 *       comparisonDays} (no fake precision — a two-day gap is not treated as a week). The bound is
 *       <em>exclusive</em>: exactly 1%/week is not flagged.
 *   <li><b>Worsening</b>: any measured body-fat increase ({@code weeklyBodyFatChange > 0}).
 *   <li><b>Positive</b>: body fat improving ({@code < 0}) while weight is stable (weekly change
 *       within ±{@link #WEEKLY_DROP_LIMIT_PCT}%).
 *   <li><b>Insufficient data</b>: fewer than two measurements ({@code comparisonDays == null}).
 * </ul>
 *
 * <h2>Precedence</h2>
 *
 * At most one recommendation is emitted, in priority order: insufficient-data → excessive-drop →
 * worsening → positive → neutral fallback. Safety (excessive drop) outranks the slower body-fat
 * signals; when no directional signal is present a neutral "keep the plan" is returned. This keeps
 * the output small and unambiguous; returning a {@link List} leaves room for FOR-45 to combine
 * recommendations from several rule sets.
 */
public final class BodyTrendRules {

  /** Weekly body-weight change, as a percentage, considered "too fast" to lose (exclusive). */
  public static final double WEEKLY_DROP_LIMIT_PCT = 1.0;

  private BodyTrendRules() {}

  /** Evaluates the body trend, stamping any recommendation with {@code createdAt}. */
  public static List<Recommendation> evaluate(WeeklyBodySummary summary, Instant createdAt) {
    if (summary == null || summary.comparisonDays() == null) {
      return List.of(insufficientData(createdAt));
    }

    Double weeklyDropPct = weeklyWeightChangePct(summary);
    if (weeklyDropPct != null && weeklyDropPct < -WEEKLY_DROP_LIMIT_PCT) {
      return List.of(excessiveDrop(summary, weeklyDropPct, createdAt));
    }

    Double bodyFatChange = summary.weeklyBodyFatChange();
    if (bodyFatChange != null && bodyFatChange > 0) {
      return List.of(worsening(summary, createdAt));
    }

    boolean weightStable =
        weeklyDropPct == null || Math.abs(weeklyDropPct) <= WEEKLY_DROP_LIMIT_PCT;
    if (bodyFatChange != null && bodyFatChange < 0 && weightStable) {
      return List.of(positive(summary, createdAt));
    }

    return List.of(neutral(createdAt));
  }

  /**
   * Weekly body-weight change as a percentage of the latest weight, normalized from the raw delta
   * over the actual {@code comparisonDays}. Negative means weight lost. Null when the inputs do not
   * support it.
   */
  private static Double weeklyWeightChangePct(WeeklyBodySummary summary) {
    Double change = summary.weeklyWeightChangeKg();
    Double latest = summary.latestWeightKg();
    Integer days = summary.comparisonDays();
    if (change == null || latest == null || latest == 0 || days == null || days == 0) {
      return null;
    }
    double weeklyChangeKg = change / days * 7.0;
    return weeklyChangeKg / latest * 100.0;
  }

  private static Recommendation insufficientData(Instant createdAt) {
    return new Recommendation(
        createdAt,
        RecommendationCategory.BODY,
        RecommendationSeverity.INFO,
        "Registra otra medición para analizar tu tendencia corporal.",
        "Hacen falta al menos dos mediciones para comparar la evolución.",
        null);
  }

  private static Recommendation excessiveDrop(
      WeeklyBodySummary summary, double weeklyDropPct, Instant createdAt) {
    String reason =
        String.format(
            Locale.ROOT,
            "El peso baja %.1f kg en %d días (~%+.1f%% por semana), por encima del %.0f%% semanal recomendado.",
            summary.weeklyWeightChangeKg(),
            summary.comparisonDays(),
            weeklyDropPct,
            WEEKLY_DROP_LIMIT_PCT);
    return new Recommendation(
        createdAt,
        RecommendationCategory.BODY,
        RecommendationSeverity.ACTION,
        "El peso baja rápido; considera aumentar un poco las calorías para frenar la pérdida.",
        reason,
        "weeklyWeightChangeKg");
  }

  private static Recommendation worsening(WeeklyBodySummary summary, Instant createdAt) {
    String reason =
        String.format(
            Locale.ROOT,
            "La grasa corporal sube %+.1f%% respecto a la medición anterior.",
            summary.weeklyBodyFatChange());
    return new Recommendation(
        createdAt,
        RecommendationCategory.BODY,
        RecommendationSeverity.ACTION,
        "La grasa corporal sube; prueba un ajuste pequeño reduciendo unas 100 kcal al día.",
        reason,
        "weeklyBodyFatChange");
  }

  private static Recommendation positive(WeeklyBodySummary summary, Instant createdAt) {
    String reason =
        String.format(
            Locale.ROOT,
            "La grasa corporal baja %+.1f%% con el peso estable.",
            summary.weeklyBodyFatChange());
    return new Recommendation(
        createdAt,
        RecommendationCategory.BODY,
        RecommendationSeverity.INFO,
        "Buena evolución; mantén el plan sin cambios.",
        reason,
        "weeklyBodyFatChange");
  }

  private static Recommendation neutral(Instant createdAt) {
    return new Recommendation(
        createdAt,
        RecommendationCategory.BODY,
        RecommendationSeverity.INFO,
        "Sin cambios significativos; mantén el plan y sigue registrando.",
        "El peso y la grasa corporal se mantienen estables respecto a la medición anterior.",
        null);
  }
}
