package dev.diegobarrioh.forma.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Conservative recovery-warning rules (FOR-44): detect simple fatigue/overreach signals and emit a
 * single {@code RECOVERY} {@link Recommendation} at {@link RecommendationSeverity#WARNING}
 * suggesting review or a lighter week — never a diagnosis. Pure, framework-free domain logic
 * (ADR-001); deterministic and fail-safe (missing data never warns).
 *
 * <p>Combines the FOR-40 {@link WeeklyCheckIn} session counts with the FOR-21 {@link
 * WeeklyBodySummary} body-fat delta (the check-in does not carry deltas — same reason FOR-42 reads
 * the summary directly).
 *
 * <h2>Supported signals (documented per spec FOR-44 Open Questions)</h2>
 *
 * <ul>
 *   <li><b>High load, low completion</b>: {@code totalPlanned ≥} {@link #HIGH_LOAD_SESSIONS} and
 *       completion {@code ≤} {@link #LOW_COMPLETION} → possible overreach.
 *   <li><b>Worsening body trend under high load</b>: body fat rising ({@code weeklyBodyFatChange >
 *       0}) while {@code totalPlanned ≥} {@link #HIGH_LOAD_SESSIONS} → review.
 * </ul>
 *
 * <h2>Data gaps (not implemented)</h2>
 *
 * <ul>
 *   <li><b>"Several skipped sessions in a row"</b> needs per-session history that the weekly
 *       summaries do not expose — not implemented until such history exists. This is the exact
 *       signal FOR-150 rule 3 ("Fuerza baja: 2 entrenos malos") needs — a per-session strength
 *       "good/bad" quality signal, not the planned/completed <em>counts</em> {@link WeeklyCheckIn}
 *       already exposes. No such signal exists anywhere in the repository (verified against {@code
 *       TrainingSessionStatusRepository} and the check-in), so FOR-150 rule 3 is gated the same
 *       way: not implemented, no stub rule added (AGENTS.md: no speculative abstractions), until a
 *       future story introduces that data source.
 *   <li><b>Week-over-week rising planned load</b> needs previous-week history the summaries do not
 *       expose; the "high load, low completion" signal uses <em>absolute</em> load as the supported
 *       proxy.
 * </ul>
 *
 * <p>Emits at most one combined warning (MVP decision); its reason cites every signal that fired.
 * Returns an empty list when no signal fires — recovery is a warning-only concern, so a healthy or
 * dataless week produces nothing.
 */
public final class RecoveryWarningRules {

  /** Planned weekly sessions at or above which the training load counts as "high" (inclusive). */
  public static final int HIGH_LOAD_SESSIONS = 5;

  /** Completion ratio at or below which completion counts as "low" (inclusive). */
  public static final double LOW_COMPLETION = 0.4;

  private RecoveryWarningRules() {}

  /**
   * Evaluates the recovery signals, stamping a warning (if any) with {@code createdAt}. Returns an
   * empty list when nothing warrants a warning.
   */
  public static List<Recommendation> evaluate(
      WeeklyCheckIn checkIn, WeeklyBodySummary body, Instant createdAt) {
    if (checkIn == null) {
      return List.of();
    }

    int totalPlanned = checkIn.plannedRunningSessions() + checkIn.plannedStrengthSessions();
    int totalCompleted = checkIn.completedRunningSessions() + checkIn.completedStrengthSessions();
    boolean highLoad = totalPlanned >= HIGH_LOAD_SESSIONS;

    boolean lowCompletion = highLoad && (double) totalCompleted / totalPlanned <= LOW_COMPLETION;

    Double bodyFatChange = body == null ? null : body.weeklyBodyFatChange();
    boolean bodyWorsening = highLoad && bodyFatChange != null && bodyFatChange > 0;

    if (!lowCompletion && !bodyWorsening) {
      return List.of();
    }

    StringBuilder reason = new StringBuilder();
    if (lowCompletion) {
      reason.append(
          String.format(
              Locale.ROOT,
              "Carga alta (%d sesiones planificadas) con baja finalización (%d completadas).",
              totalPlanned,
              totalCompleted));
    }
    if (bodyWorsening) {
      if (reason.length() > 0) {
        reason.append(' ');
      }
      reason.append(
          String.format(
              Locale.ROOT,
              "La grasa corporal sube %+.1f%% con una carga de entrenamiento alta.",
              bodyFatChange));
    }

    return List.of(
        new Recommendation(
            createdAt,
            RecommendationCategory.RECOVERY,
            RecommendationSeverity.WARNING,
            "Considera revisar la carga o planificar una semana más ligera.",
            reason.toString(),
            bodyWorsening ? "weeklyBodyFatChange" : null));
  }
}
