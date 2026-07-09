package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Neutral, non-shaming training-adherence rules (FOR-43): turn a {@link WeeklyCheckIn}'s
 * planned/completed session counts (assembled from the FOR-28 training summary) into at most one
 * {@code TRAINING} {@link Recommendation}. Pure, framework-free domain logic (ADR-001);
 * deterministic — the same completion data always yields the same recommendation.
 *
 * <p>Reads the {@link WeeklyCheckIn} directly: unlike the FOR-42 body rules (which needed the
 * FOR-21 deltas the check-in does not carry), the running/strength counts this rule set needs
 * already live on the check-in — the Insights snapshot FOR-40 assembles for exactly this purpose.
 *
 * <h2>Thresholds (documented per spec FOR-43 Open Questions)</h2>
 *
 * Adherence is {@code totalCompleted / totalPlanned}. Both bounds are <em>inclusive</em>:
 *
 * <ul>
 *   <li><b>High</b>: adherence ≥ {@link #HIGH_ADHERENCE} → positive note.
 *   <li><b>Low</b>: adherence ≤ {@link #LOW_ADHERENCE} → supportive nudge (non-shaming).
 *   <li><b>Imbalance</b>: one discipline trained while the other was fully missed ({@code planned >
 *       0} but {@code completed == 0}) → balance suggestion.
 *   <li><b>No planned training</b>: {@code totalPlanned == 0} → safe neutral note (no
 *       divide-by-zero, no shame).
 * </ul>
 *
 * <h2>Precedence</h2>
 *
 * At most one recommendation, in priority order: no-planned → low adherence → running-done/
 * strength-missed → strength-done/running-missed → high adherence → neutral. Support (low
 * adherence) is surfaced before the more specific imbalance nudges; a fully balanced strong week
 * reaches the positive note. Returns a {@link List} so FOR-45 can combine it with the other rule
 * sets.
 */
public final class TrainingAdherenceRules {

  /** Completion ratio at or above which the week counts as high adherence (inclusive). */
  public static final double HIGH_ADHERENCE = 0.8;

  /** Completion ratio at or below which the week counts as low adherence (inclusive). */
  public static final double LOW_ADHERENCE = 0.4;

  private TrainingAdherenceRules() {}

  /** Evaluates training adherence, stamping any recommendation with {@code createdAt}. */
  public static List<Recommendation> evaluate(WeeklyCheckIn checkIn, Instant createdAt) {
    if (checkIn == null) {
      return List.of(noPlannedTraining(createdAt));
    }

    int plannedRunning = checkIn.plannedRunningSessions();
    int completedRunning = checkIn.completedRunningSessions();
    int plannedStrength = checkIn.plannedStrengthSessions();
    int completedStrength = checkIn.completedStrengthSessions();

    int totalPlanned = plannedRunning + plannedStrength;
    if (totalPlanned == 0) {
      return List.of(noPlannedTraining(createdAt));
    }

    int totalCompleted = completedRunning + completedStrength;
    double adherence = (double) totalCompleted / totalPlanned;

    if (adherence <= LOW_ADHERENCE) {
      return List.of(lowAdherence(totalCompleted, totalPlanned, createdAt));
    }

    boolean runningDone = plannedRunning > 0 && completedRunning > 0;
    boolean strengthMissed = plannedStrength > 0 && completedStrength == 0;
    if (runningDone && strengthMissed) {
      return List.of(strengthMissedImbalance(completedRunning, plannedStrength, createdAt));
    }

    boolean strengthDone = plannedStrength > 0 && completedStrength > 0;
    boolean runningMissed = plannedRunning > 0 && completedRunning == 0;
    if (strengthDone && runningMissed) {
      return List.of(runningMissedImbalance(completedStrength, plannedRunning, createdAt));
    }

    if (adherence >= HIGH_ADHERENCE) {
      return List.of(highAdherence(totalCompleted, totalPlanned, createdAt));
    }

    return List.of(steady(totalCompleted, totalPlanned, createdAt));
  }

  private static Recommendation noPlannedTraining(Instant createdAt) {
    return new Recommendation(
        createdAt,
        RecommendationCategory.TRAINING,
        RecommendationSeverity.INFO,
        "No hay entrenamientos planificados esta semana; planifica alguno cuando quieras.",
        "La semana no tiene sesiones de carrera ni de fuerza planificadas.",
        null);
  }

  private static Recommendation lowAdherence(int completed, int planned, Instant createdAt) {
    return new Recommendation(
        createdAt,
        RecommendationCategory.TRAINING,
        RecommendationSeverity.ACTION,
        "Retoma poco a poco; una sola sesión esta semana ya suma.",
        String.format(
            Locale.ROOT, "Se completaron %d de %d sesiones planificadas.", completed, planned),
        null);
  }

  private static Recommendation strengthMissedImbalance(
      int completedRunning, int plannedStrength, Instant createdAt) {
    return new Recommendation(
        createdAt,
        RecommendationCategory.TRAINING,
        RecommendationSeverity.INFO,
        "Buen trabajo con la carrera; añade una sesión de fuerza para equilibrar la semana.",
        String.format(
            Locale.ROOT,
            "Carrera con %d sesión(es) completada(s) y %d sesión(es) de fuerza sin completar.",
            completedRunning,
            plannedStrength),
        null);
  }

  private static Recommendation runningMissedImbalance(
      int completedStrength, int plannedRunning, Instant createdAt) {
    return new Recommendation(
        createdAt,
        RecommendationCategory.TRAINING,
        RecommendationSeverity.INFO,
        "Buen trabajo con la fuerza; añade una sesión de carrera para equilibrar la semana.",
        String.format(
            Locale.ROOT,
            "Fuerza con %d sesión(es) completada(s) y %d sesión(es) de carrera sin completar.",
            completedStrength,
            plannedRunning),
        null);
  }

  private static Recommendation highAdherence(int completed, int planned, Instant createdAt) {
    return new Recommendation(
        createdAt,
        RecommendationCategory.TRAINING,
        RecommendationSeverity.INFO,
        "Semana muy constante; mantén este ritmo.",
        String.format(
            Locale.ROOT, "Se completaron %d de %d sesiones planificadas.", completed, planned),
        null);
  }

  private static Recommendation steady(int completed, int planned, Instant createdAt) {
    return new Recommendation(
        createdAt,
        RecommendationCategory.TRAINING,
        RecommendationSeverity.INFO,
        "Vas por buen camino; intenta cerrar las sesiones que quedan.",
        String.format(
            Locale.ROOT, "Se completaron %d de %d sesiones planificadas.", completed, planned),
        null);
  }
}
