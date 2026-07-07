package dev.diegobarrioh.forma.delivery.training;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/training/week} (FOR-26).
 *
 * <p>Delivery read model, distinct from the application {@link WeeklyTrainingSchedule} (ADR-005 —
 * controllers never return application/domain types directly). {@code dayOfWeek} is serialized as
 * its name (e.g. {@code "MONDAY"}); {@code rest} flags days with no sessions. Each session carries
 * a stable {@code id} for marking completion (FOR-27) and its current {@code status}/{@code notes}.
 */
public record TrainingWeekResponse(List<Day> days) {

  public record Day(String dayOfWeek, boolean rest, List<Session> sessions) {}

  /** Null {@code notes} are omitted from the JSON. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Session(
      String id, String kind, String title, String detail, String status, String notes) {}

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
                                        entry.id(),
                                        entry.kind(),
                                        entry.title(),
                                        entry.detail(),
                                        entry.status(),
                                        entry.notes()))
                            .toList()))
            .toList();
    return new TrainingWeekResponse(days);
  }
}
