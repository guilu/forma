package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Conservative, explainable body-trend rules (FOR-42; rule 1 re-thresholded by FOR-150): turn a
 * {@link WeeklyBodySummary} (FOR-21) into at most one {@code BODY} {@link Recommendation}. Pure,
 * framework-free domain logic (ADR-001); deterministic — the same trend always yields the same
 * recommendation.
 *
 * <p>Reads the FOR-21 weekly deltas ({@code weeklyWeightChangeKg}, {@code weeklyBodyFatChange}) and
 * {@code comparisonDays}. The FOR-40 {@link WeeklyCheckIn} deliberately does not carry these
 * deltas, so the rules read the summary directly (see FOR-40/FOR-41 decisions).
 *
 * <h2>Thresholds</h2>
 *
 * <ul>
 *   <li><b>Excessive weight drop</b> (FOR-150 rule 1, sheet *Reglas*: "Peso baja rápido"): losing
 *       more than {@link #WEEKLY_DROP_LIMIT_KG} kg of body weight per week, in <em>absolute</em>
 *       terms (the original FOR-42 threshold was a relative percentage; the Excel is an absolute
 *       kg/week figure — realigned by FOR-150). The raw delta is normalized to a weekly rate using
 *       the actual {@code comparisonDays} (no fake precision — a two-day gap is not treated as a
 *       week). The bound is <em>exclusive</em>: exactly -0.4 kg/week is not flagged (Excel "&lt;
 *       -0.4"). The recommendation carries the Excel's explicit +100/+150 kcal adjustment.
 *   <li><b>Early body-fat rise</b>: a single measured body-fat increase ({@code weeklyBodyFatChange
 *       > 0}) is now only an {@code INFO} note ("worth watching"), not an actionable trigger —
 *       FOR-150 rule 2 requires a <em>sustained</em> 2-3 week rise before it becomes an action,
 *       which needs multi-week history this summary does not carry (see {@link BodyFatTrendRules},
 *       sourced from the FOR-155 weekly records). Downgrading (rather than silently dropping) keeps
 *       the observation honest instead of falsely reporting "no change."
 *   <li><b>Positive</b>: body fat improving ({@code < 0}) while weight is stable (weekly change
 *       within ±{@link #WEEKLY_DROP_LIMIT_KG} kg).
 *   <li><b>Insufficient data</b>: fewer than two measurements ({@code comparisonDays == null}).
 * </ul>
 *
 * <h2>Precedence</h2>
 *
 * At most one recommendation is emitted, in priority order: insufficient-data → excessive-drop →
 * early body-fat rise → positive → neutral fallback. Safety (excessive drop) outranks the slower
 * body-fat signals; when no directional signal is present a neutral "keep the plan" is returned.
 * This keeps the output small and unambiguous; returning a {@link List} leaves room for FOR-45 to
 * combine recommendations from several rule sets.
 */
public final class BodyTrendRules {

  /**
   * Weekly body-weight change, in kilograms, considered "too fast" to lose (exclusive; FOR-150 rule
   * 1, sheet *Reglas* "&lt; -0.4 kg/semana"). Replaces the original FOR-42 relative-percentage
   * threshold.
   */
  public static final double WEEKLY_DROP_LIMIT_KG = 0.4;

  private BodyTrendRules() {}

  /** Evaluates the body trend, stamping any recommendation with {@code createdAt}. */
  public static List<Recommendation> evaluate(WeeklyBodySummary summary, Instant createdAt) {
    if (summary == null || summary.comparisonDays() == null) {
      return List.of(insufficientData(createdAt));
    }

    Double weeklyDropKg = weeklyWeightChangeKgPerWeek(summary);
    if (weeklyDropKg != null && weeklyDropKg < -WEEKLY_DROP_LIMIT_KG) {
      return List.of(excessiveDrop(summary, weeklyDropKg, createdAt));
    }

    Double bodyFatChange = summary.weeklyBodyFatChange();
    if (bodyFatChange != null && bodyFatChange > 0) {
      return List.of(earlyBodyFatRise(summary, createdAt));
    }

    boolean weightStable = weeklyDropKg == null || Math.abs(weeklyDropKg) <= WEEKLY_DROP_LIMIT_KG;
    if (bodyFatChange != null && bodyFatChange < 0 && weightStable) {
      return List.of(positive(summary, createdAt));
    }

    return List.of(neutral(createdAt));
  }

  /**
   * Weekly body-weight change in kilograms, normalized from the raw delta over the actual {@code
   * comparisonDays}. Negative means weight lost. Null when the inputs do not support it.
   */
  private static Double weeklyWeightChangeKgPerWeek(WeeklyBodySummary summary) {
    Double change = summary.weeklyWeightChangeKg();
    Integer days = summary.comparisonDays();
    if (change == null || days == null || days == 0) {
      return null;
    }
    return change / days * 7.0;
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
      WeeklyBodySummary summary, double weeklyDropKg, Instant createdAt) {
    String reason =
        String.format(
            Locale.ROOT,
            "El peso baja %.1f kg en %d días (~%.1f kg/semana), por encima del límite de %.1f "
                + "kg/semana.",
            summary.weeklyWeightChangeKg(),
            summary.comparisonDays(),
            weeklyDropKg,
            WEEKLY_DROP_LIMIT_KG);
    return new Recommendation(
        createdAt,
        RecommendationCategory.BODY,
        RecommendationSeverity.ACTION,
        "El peso baja rápido; sube 100–150 kcal para no perder masa magra.",
        reason,
        "weeklyWeightChangeKg");
  }

  private static Recommendation earlyBodyFatRise(WeeklyBodySummary summary, Instant createdAt) {
    String reason =
        String.format(
            Locale.ROOT,
            "La grasa corporal sube %+.1f%% respecto a la medición anterior; aún no es una "
                + "tendencia sostenida de varias semanas.",
            summary.weeklyBodyFatChange());
    return new Recommendation(
        createdAt,
        RecommendationCategory.BODY,
        RecommendationSeverity.INFO,
        "La grasa corporal sube un poco esta semana; vale la pena vigilar la tendencia.",
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
