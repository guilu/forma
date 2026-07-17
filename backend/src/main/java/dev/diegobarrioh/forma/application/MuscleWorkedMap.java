package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MuscleLoad;
import java.util.List;

/**
 * Muscle-worked read model for a strength session (FOR-136, muscle-worked-map slice of FOR-104):
 * the union of muscles worked by the session's exercises, each with a derived {@link MuscleLoad}.
 * Feeds the FOR-53 heatmap.
 *
 * <p>Application-level view, not persisted (spec FOR-136 Data Model Notes: "no persisted aggregate,
 * no table") — built fresh on every read by {@link MuscleWorkedMapService}, mirroring the {@link
 * Adherence} (FOR-129) / {@code AchievementsView} (FOR-135) pattern.
 *
 * @param sessionId the requested stable session id (the FOR-26 weekly-schedule {@code
 *     "<DAY>:STRENGTH"} id); echoed back unchanged
 * @param muscles one entry per worked muscle; empty for a non-strength session (never an error)
 */
public record MuscleWorkedMap(String sessionId, List<MuscleWorked> muscles) {

  /**
   * One worked muscle and its derived load.
   *
   * @param muscle the raw muscle label from {@link dev.diegobarrioh.forma.domain.Exercise
   *     #primaryMuscles()} (spec: "never fabricate muscles")
   * @param load the frequency-derived {@link MuscleLoad}
   */
  public record MuscleWorked(String muscle, MuscleLoad load) {}
}
