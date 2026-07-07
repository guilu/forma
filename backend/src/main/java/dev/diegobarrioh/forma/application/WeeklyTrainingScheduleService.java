package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.SessionType;
import dev.diegobarrioh.forma.domain.WorkoutType;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Composes the weekly training calendar (FOR-26) from the FOR-23 running plan and FOR-25 workout
 * templates.
 *
 * <p>The plan (FOR-22/FOR-23) and templates (FOR-25) are not scheduled to real dates yet, so this
 * service applies a simple, documented scheduling policy for the MVP:
 *
 * <ul>
 *   <li>Running: plan week {@link #PLAN_WEEK} (the first week), each session on its own day
 *       (Tue/Thu/Sat per the generator).
 *   <li>Strength: one template per day — Monday PUSH, Wednesday PULL, Friday LEGS.
 *   <li>Any remaining day (e.g. Sunday) is a rest day (no entries).
 * </ul>
 *
 * There is no week navigation and no date arithmetic in this slice (spec FOR-26 Open Questions); a
 * later story can schedule to real dates. All entries are {@code PLANNED}.
 */
@Service
public class WeeklyTrainingScheduleService {

  /** The plan week shown by the MVP calendar. */
  static final int PLAN_WEEK = 1;

  private static final String PLANNED = "PLANNED";

  private static final Map<DayOfWeek, WorkoutType> STRENGTH_DAYS =
      Map.of(
          DayOfWeek.MONDAY, WorkoutType.PUSH,
          DayOfWeek.WEDNESDAY, WorkoutType.PULL,
          DayOfWeek.FRIDAY, WorkoutType.LEGS);

  private final RunningPlanService runningPlanService;
  private final WorkoutTemplateService workoutTemplateService;

  public WeeklyTrainingScheduleService(
      RunningPlanService runningPlanService, WorkoutTemplateService workoutTemplateService) {
    this.runningPlanService = runningPlanService;
    this.workoutTemplateService = workoutTemplateService;
  }

  /** Builds the current week's calendar (Monday through Sunday). */
  public WeeklyTrainingSchedule currentWeek() {
    Map<DayOfWeek, List<TrainingEntry>> entriesByDay = new EnumMap<>(DayOfWeek.class);
    for (DayOfWeek day : DayOfWeek.values()) {
      entriesByDay.put(day, new ArrayList<>());
    }

    // Running sessions from the first plan week, on their planned days.
    runningPlanService.currentPlan().stream()
        .filter(session -> session.weekNumber() == PLAN_WEEK)
        .forEach(
            session ->
                entriesByDay
                    .get(session.dayOfWeek())
                    .add(
                        new TrainingEntry(
                            "RUNNING",
                            runningTitle(session.sessionType()),
                            String.format(Locale.ROOT, "%.1f km", session.targetDistanceKm()),
                            PLANNED)));

    // Strength templates on their assigned days.
    STRENGTH_DAYS.forEach(
        (day, type) ->
            workoutTemplateService
                .findByType(type)
                .ifPresent(
                    template ->
                        entriesByDay
                            .get(day)
                            .add(
                                new TrainingEntry(
                                    "STRENGTH",
                                    strengthTitle(type),
                                    template.items().size() + " ejercicios",
                                    PLANNED))));

    List<TrainingDay> days = new ArrayList<>(DayOfWeek.values().length);
    for (DayOfWeek day : DayOfWeek.values()) {
      days.add(new TrainingDay(day, List.copyOf(entriesByDay.get(day))));
    }
    return new WeeklyTrainingSchedule(List.copyOf(days));
  }

  private static String runningTitle(SessionType type) {
    return switch (type) {
      case EASY -> "Rodaje suave";
      case INTERVALS -> "Series";
      case LONG_RUN -> "Tirada larga";
      case RECOVERY -> "Recuperación";
    };
  }

  private static String strengthTitle(WorkoutType type) {
    return switch (type) {
      case PUSH -> "Fuerza · Empuje";
      case PULL -> "Fuerza · Tirón";
      case LEGS -> "Fuerza · Pierna y core";
      case FULL_BODY -> "Fuerza · Cuerpo completo";
    };
  }
}
