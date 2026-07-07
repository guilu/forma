package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/training/week} (FOR-26).
 *
 * <p>Delivery read model, distinct from the application {@link WeeklyTrainingSchedule} (ADR-005 —
 * controllers never return application/domain types directly). {@code dayOfWeek} is serialized as
 * its name (e.g. {@code "MONDAY"}); {@code rest} flags days with no sessions.
 */
public record TrainingWeekResponse(List<Day> days) {

  public record Day(String dayOfWeek, boolean rest, List<Session> sessions) {}

  public record Session(String kind, String title, String detail, String status) {}

  /** Maps the composed schedule to its API read model. */
  public static TrainingWeekResponse from(WeeklyTrainingSchedule schedule) {
    List<Day> days =
        schedule.days().stream()
            .map(
                day ->
                    new Day(
                        day.dayOfWeek().name(),
                        day.isRest(),
                        day.entries().stream()
                            .map(
                                entry ->
                                    new Session(
                                        entry.kind(),
                                        entry.title(),
                                        entry.detail(),
                                        entry.status()))
                            .toList()))
            .toList();
    return new TrainingWeekResponse(days);
  }
}
