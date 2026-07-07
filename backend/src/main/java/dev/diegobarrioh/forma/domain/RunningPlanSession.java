package dev.diegobarrioh.forma.domain;

import java.time.DayOfWeek;
import java.util.Objects;

/**
 * One planned running session in a multi-week progression (FOR-22).
 *
 * <p>The Training context's core planned-run type (docs/domain-model.md, "RunningPlanSession"). It
 * is framework-free — no Spring, JPA/JDBC or HTTP types (ADR-001) — following the FOR-15 {@link
 * BodyMeasurement} precedent. Seeding a full plan is FOR-23; this story only defines the type.
 *
 * <p>This models a <em>planned</em> session, not a logged run: it carries no actual pace, heart
 * rate or calories (those belong to a future {@code RunningSession}). {@code weekNumber} lets many
 * sessions form a multi-week plan.
 *
 * <p>Intensity is expressed as {@code targetEffort} on the RPE (rate of perceived exertion) 1–10
 * scale rather than a precise pace, per the Jira intent ("target effort rather than only pace").
 * {@code targetPaceRange} from docs/domain-model.md is intentionally deferred until a story needs
 * pace display (spec FOR-22 Open Questions).
 *
 * <p>Values are validated at construction for internal consistency (FOR-15 precedent).
 *
 * @param weekNumber 1-based week within the plan; must be >= 1
 * @param dayOfWeek day the session is planned for; required
 * @param sessionType the kind of session; required
 * @param targetDistanceKm planned distance in kilometers; must be strictly positive
 * @param targetEffort target RPE in {@code [1, 10]}
 * @param notes optional free-text note
 */
public record RunningPlanSession(
    int weekNumber,
    DayOfWeek dayOfWeek,
    SessionType sessionType,
    double targetDistanceKm,
    int targetEffort,
    String notes) {

  private static final int MIN_EFFORT = 1;
  private static final int MAX_EFFORT = 10;

  public RunningPlanSession {
    Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
    Objects.requireNonNull(sessionType, "sessionType must not be null");
    if (weekNumber < 1) {
      throw new IllegalArgumentException("weekNumber must be >= 1, was: " + weekNumber);
    }
    if (targetDistanceKm <= 0) {
      throw new IllegalArgumentException(
          "targetDistanceKm must be strictly positive, was: " + targetDistanceKm);
    }
    if (targetEffort < MIN_EFFORT || targetEffort > MAX_EFFORT) {
      throw new IllegalArgumentException(
          "targetEffort must be within [1, 10] (RPE), was: " + targetEffort);
    }
  }
}
