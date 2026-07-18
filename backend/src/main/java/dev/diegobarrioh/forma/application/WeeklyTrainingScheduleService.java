package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.SessionStatus;
import dev.diegobarrioh.forma.domain.SessionType;
import dev.diegobarrioh.forma.domain.WeeklyTrainingDayPolicy;
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
 * templates, applying stored completion status (FOR-27).
 *
 * <p>The plan (FOR-22/FOR-23) and templates (FOR-25) are not scheduled to real dates yet, so this
 * service applies a simple, documented scheduling policy for the MVP, defined once in {@link
 * WeeklyTrainingDayPolicy} (extracted for FOR-128 so the nutrition consumption target uses the
 * exact same day-kind classification, not a duplicate):
 *
 * <ul>
 *   <li>Running: plan week {@link #PLAN_WEEK} (the first week), each session on its own day
 *       (Mon/Wed/Sat per the generator, FOR-151).
 *   <li>Strength: one template per day — Tuesday PUSH, Thursday PULL, Sunday LEGS (FOR-151).
 *   <li>Any remaining day (i.e. Friday) is a rest day (no entries).
 * </ul>
 *
 * <p>Each session has a stable id ({@code "<DAY>:<KIND>"}) that is stable because the schedule is
 * deterministic; completion status (FOR-27) is stored against that id and applied here (default
 * {@code PLANNED}). No week navigation or real dates yet (spec FOR-26 Open Questions).
 */
@Service
public class WeeklyTrainingScheduleService {

  /** The plan week shown by the MVP calendar. */
  static final int PLAN_WEEK = 1;

  private final RunningPlanService runningPlanService;
  private final WorkoutTemplateService workoutTemplateService;
  private final TrainingSessionStatusRepository statusRepository;

  public WeeklyTrainingScheduleService(
      RunningPlanService runningPlanService,
      WorkoutTemplateService workoutTemplateService,
      TrainingSessionStatusRepository statusRepository) {
    this.runningPlanService = runningPlanService;
    this.workoutTemplateService = workoutTemplateService;
    this.statusRepository = statusRepository;
  }

  /** The stable session id for a day + kind (e.g. {@code "SATURDAY:RUNNING"}). */
  public static String sessionId(DayOfWeek day, String kind) {
    return day.name() + ":" + kind;
  }

  /** Builds the current week's calendar (Monday through Sunday), with stored status applied. */
  public WeeklyTrainingSchedule currentWeek() {
    Map<String, StoredSessionStatus> stored = statusRepository.findAll();
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
                        entry(
                            session.dayOfWeek(),
                            "RUNNING",
                            runningTitle(session.sessionType()),
                            String.format(Locale.ROOT, "%.1f km", session.targetDistanceKm()),
                            stored)));

    // Strength templates on their assigned days.
    WeeklyTrainingDayPolicy.strengthDays()
        .forEach(
            (day, type) ->
                workoutTemplateService
                    .findByType(type)
                    .ifPresent(
                        template ->
                            entriesByDay
                                .get(day)
                                .add(
                                    entry(
                                        day,
                                        "STRENGTH",
                                        strengthTitle(type),
                                        template.items().size() + " ejercicios",
                                        stored))));

    List<TrainingDay> days = new ArrayList<>(DayOfWeek.values().length);
    for (DayOfWeek day : DayOfWeek.values()) {
      days.add(new TrainingDay(day, List.copyOf(entriesByDay.get(day))));
    }
    return new WeeklyTrainingSchedule(List.copyOf(days));
  }

  private static TrainingEntry entry(
      DayOfWeek day,
      String kind,
      String title,
      String detail,
      Map<String, StoredSessionStatus> stored) {
    String id = sessionId(day, kind);
    StoredSessionStatus status = stored.get(id);
    String statusName = (status == null) ? SessionStatus.PLANNED.name() : status.status().name();
    String notes = (status == null) ? null : status.notes();
    return new TrainingEntry(id, kind, title, detail, statusName, notes);
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
