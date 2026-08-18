package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.BodyView;
import java.time.DayOfWeek;
import java.util.List;

/**
 * A composed week of planned training (FOR-26): one entry list per day, combining running and
 * strength sessions, with rest days represented as days without entries.
 *
 * <p>An application-layer read model built by {@link WeeklyTrainingScheduleService} from the FOR-23
 * running plan and FOR-25 workout templates. All entries are {@code PLANNED} in this story;
 * completion status is FOR-27.
 *
 * @param days seven days, Monday through Sunday, in order
 */
public record WeeklyTrainingSchedule(List<TrainingDay> days) {

  /** One day of the week and the sessions planned on it (empty = rest day). */
  public record TrainingDay(DayOfWeek dayOfWeek, List<TrainingEntry> entries) {
    public boolean isRest() {
      return entries.isEmpty();
    }
  }

  /**
   * A single planned session shown in the calendar.
   *
   * @param id stable session id naming what the session is, with no day in it (e.g. {@code
   *     "RUNNING:LONG_RUN"}); used to mark completion (FOR-27) and to move it (V60)
   * @param kind {@code "RUNNING"} or {@code "STRENGTH"}
   * @param title short human-readable title (e.g. "Tirada larga", "Fuerza · Empuje")
   * @param detail secondary line (e.g. "10.0 km", "4 ejercicios")
   * @param status session status: {@code PLANNED}, {@code COMPLETED} or {@code SKIPPED} (FOR-27)
   * @param notes optional completion note, or {@code null}
   * @param workoutType strength workout template type, or {@code null} for running sessions
   * @param bodyView anatomical side that represents the session's primary muscle focus
   */
  public record TrainingEntry(
      String id,
      String kind,
      String title,
      String detail,
      String status,
      String notes,
      String workoutType,
      BodyView bodyView) {}
}
