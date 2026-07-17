package dev.diegobarrioh.forma.domain;

import java.util.List;

/**
 * The bundle of existing data an {@link AchievementRule} evaluates against (FOR-135, achievements
 * slice of FOR-104). Framework-free (ADR-001) — no Spring, JDBC or HTTP types.
 *
 * <p>Deliberately narrow: it carries only what the final MVP catalog rules need, sourced from
 * repositories/ports already confirmed cheaply queryable end-to-end (spec FOR-135 hard rule —
 * "inspect their ACTUAL query methods first; if the data a candidate rule needs isn't cheaply
 * available, drop that rule"). {@code MealLogRepository} and {@code WaterIntakeRepository} are
 * deliberately absent here: both only expose a per-date lookup ({@code findByOwnerAndDate}), never
 * an all-time list/count, so an all-time "N meals logged" or "first hydration entry" rule would
 * require an unbounded per-date scan since account inception — not cheap, unlike {@code
 * BodyMeasurementRepository#list()}/{@code GoalRepository#findAllByOwner}/{@code
 * IntegrationRepository#findByOwnerAndProvider}, which are each a single query. See {@code
 * AchievementCatalog}'s javadoc for the full accounting of dropped candidate rules.
 *
 * <p><b>No per-date training completion history</b> (documented FOR-129 gap): {@code
 * training_session_status} stores one current status per weekday slot, not a timestamped per-date
 * history, so this type intentionally carries no training/session data at all — a rule cannot be
 * written against data that isn't here.
 *
 * @param measurements all of the owner's body measurements (from {@code
 *     BodyMeasurementRepository#list()}), in any order; count-based rules read {@code size()}
 * @param goals all of the owner's goals (from {@code GoalRepository#findAllByOwner}), each carrying
 *     its own {@link GoalStatus}
 * @param withingsSyncCompleted whether the owner's Withings connection has ever completed a
 *     successful sync ({@code IntegrationConnection#lastSyncOutcome()} with {@link SyncResult#OK}),
 *     resolved once in the application layer from {@code
 *     IntegrationRepository#findByOwnerAndProvider}
 */
public record AchievementData(
    List<BodyMeasurement> measurements, List<Goal> goals, boolean withingsSyncCompleted) {

  public AchievementData {
    if (measurements == null) {
      throw new IllegalArgumentException("measurements must not be null");
    }
    if (goals == null) {
      throw new IllegalArgumentException("goals must not be null");
    }
    measurements = List.copyOf(measurements);
    goals = List.copyOf(goals);
  }
}
