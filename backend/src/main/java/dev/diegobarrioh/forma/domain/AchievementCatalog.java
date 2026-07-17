package dev.diegobarrioh.forma.domain;

import java.util.List;
import java.util.Optional;

/**
 * The closed, in-code achievement ("logros") catalog (FOR-135, achievements slice of FOR-104): each
 * entry has a stable id and a deterministic {@link AchievementRule} evaluated against {@link
 * AchievementData}. Defined in code, not persisted as a catalog table (spec FOR-135 Data Model
 * Notes: "The catalog + rules are in-code (like {@code FoodCatalog}) — no catalog table"),
 * mirroring {@link FoodCatalog}'s (FOR-30) in-code-catalog shape. Only <em>earned</em> achievements
 * are persisted (application layer's {@code AchievementRepository}).
 *
 * <p><b>Final MVP set (resolves spec FOR-135 Open Question "exact MVP achievement set").</b> Every
 * rule below was confirmed against a real repository's actual query methods before being kept — see
 * each entry's inline comment. Count-based rules are <b>all-time</b>, not windowed (spec FOR-135
 * Open Question default).
 *
 * <p><b>Dropped candidate rules (documented, not implemented).</b> The spec's suggested MVP set
 * also proposed meal-log and hydration achievements ("first meal logged", "N meals logged", "first
 * hydration entry", "hit the daily hydration goal on a day"). Both {@code MealLogRepository}
 * (FOR-127) and {@code WaterIntakeRepository} (FOR-130) were inspected and expose only {@code
 * findByOwnerAndDate(ownerId, date)} plus {@code save} — no all-time list/count query exists on
 * either port. Computing an all-time count or a "first ever" fact would require looping the
 * per-date query across every day since the account's first log, unbounded — unlike {@code
 * AdherenceService} (FOR-129), which loops the same per-date query but only across a documented,
 * bounded ({@code &le;365}-day) window. That is not "cheaply available" per this story's hard rule,
 * and adding a new all-time query method to either port (or duplicating their per-date SQL here)
 * would violate "REUSE existing repositories ... do NOT duplicate their query logic". Both
 * candidate rules are therefore dropped from this MVP catalog; a future story could add a bounded
 * or aggregate query to those ports and revisit.
 *
 * <p><b>No per-date training completion history</b> (documented FOR-129 gap, spec FOR-135 hard
 * rule): {@code training_session_status} (FOR-27, migration V3) stores one current status per
 * weekday session slot, not a timestamped per-date history, so no rule here is (or may ever be)
 * phrased in terms of a specific past date's training completion — streaks are explicitly out of
 * scope for this story (spec FOR-135 Summary).
 */
public final class AchievementCatalog {

  private static final List<Achievement> ACHIEVEMENTS =
      List.of(
          // Reuses BodyMeasurementRepository#list() (FOR-16) — a single, cheap, all-time query
          // already relied on identically by AdherenceService (FOR-129) for its MEASUREMENTS
          // category.
          new Achievement(
              "FIRST_MEASUREMENT",
              "Primera medición",
              "Registra tu primera medición corporal.",
              data -> !data.measurements().isEmpty()),
          new Achievement(
              "TEN_MEASUREMENTS_LOGGED",
              "10 mediciones registradas",
              "Registra 10 mediciones corporales.",
              data -> data.measurements().size() >= 10),
          // Reuses GoalRepository#findAllByOwner (FOR-125) — a single, cheap, all-time query.
          new Achievement(
              "FIRST_GOAL_CREATED",
              "Primer objetivo creado",
              "Crea tu primer objetivo.",
              data -> !data.goals().isEmpty()),
          new Achievement(
              "FIRST_GOAL_ACHIEVED",
              "Primer objetivo logrado",
              "Marca un objetivo como logrado.",
              data -> data.goals().stream().anyMatch(goal -> goal.status() == GoalStatus.ACHIEVED)),
          // Reuses IntegrationRepository#findByOwnerAndProvider (FOR-126/132) — a single, cheap
          // lookup of the one connection row for WITHINGS; the application layer resolves the
          // boolean from IntegrationConnection#lastSyncOutcome()'s SyncResult.OK before this rule
          // ever runs (see AchievementData javadoc).
          new Achievement(
              "FIRST_WITHINGS_SYNC",
              "Primera sincronización con Withings",
              "Completa tu primera sincronización con Withings.",
              AchievementData::withingsSyncCompleted));

  private AchievementCatalog() {}

  /** All catalog achievements, in a stable, deterministic order (immutable). */
  public static List<Achievement> all() {
    return ACHIEVEMENTS;
  }

  /** Finds a catalog achievement by its stable id. */
  public static Optional<Achievement> findById(String id) {
    return ACHIEVEMENTS.stream().filter(achievement -> achievement.id().equals(id)).findFirst();
  }
}
