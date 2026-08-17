package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.BodyView;
import dev.diegobarrioh.forma.domain.SessionStatus;
import dev.diegobarrioh.forma.domain.SessionType;
import dev.diegobarrioh.forma.domain.WeeklyTrainingDayPolicy;
import dev.diegobarrioh.forma.domain.WorkoutType;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Composes the weekly training calendar (FOR-26) from the FOR-23 running plan and FOR-25 workout
 * templates, applying this week's stored status and day overrides (FOR-27, re-keyed by V60).
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
 * <p><b>A session is not named after its day (V60).</b> Its id is its content — {@code
 * "RUNNING:LONG_RUN"}, {@code "STRENGTH:PUSH"} — and the policy above only supplies its
 * <em>default</em> day. Before V60 the id was {@code "MONDAY:RUNNING"}, which fused two different
 * facts into one string and made both of these impossible: expiring a status at the end of its week
 * (there was no week on the row, so a completed session was replayed for ever) and moving a session
 * to another day (which would have changed its identity rather than its date). A stored {@code
 * scheduledDay} now overrides the policy day for that week only; a stored {@code completedAt}
 * records when it was really done, which need not be the day it was planned for.
 *
 * <p>Ids stay unique within a week by construction: the generator emits one running session per
 * type per week (EASY Monday, INTERVALS-or-RECOVERY Wednesday, LONG_RUN Saturday) and the policy
 * assigns one template per {@link WorkoutType}.
 *
 * <p>Real multi-user auth (FOR-145c, ADR-012, migration V31): resolves the caller's account id via
 * {@link CurrentUserProvider} and reads only their stored rows.
 */
@Service
public class WeeklyTrainingScheduleService {

  /** The plan week shown by the MVP calendar. */
  static final int PLAN_WEEK = 1;

  private static final String RUNNING_KIND = "RUNNING";
  private static final String STRENGTH_KIND = "STRENGTH";

  private final RunningPlanService runningPlanService;
  private final WorkoutTemplateService workoutTemplateService;
  private final TrainingSessionStatusRepository statusRepository;
  private final CurrentUserProvider currentUserProvider;
  private final Clock clock;

  public WeeklyTrainingScheduleService(
      RunningPlanService runningPlanService,
      WorkoutTemplateService workoutTemplateService,
      TrainingSessionStatusRepository statusRepository,
      CurrentUserProvider currentUserProvider,
      Clock clock) {
    this.runningPlanService = runningPlanService;
    this.workoutTemplateService = workoutTemplateService;
    this.statusRepository = statusRepository;
    this.currentUserProvider = currentUserProvider;
    this.clock = clock;
  }

  /** The stable id for a running session of a given type (e.g. {@code "RUNNING:LONG_RUN"}). */
  public static String runningSessionKey(SessionType type) {
    return RUNNING_KIND + ":" + type.name();
  }

  /** The stable id for a strength session of a given type (e.g. {@code "STRENGTH:PUSH"}). */
  public static String strengthSessionKey(WorkoutType type) {
    return STRENGTH_KIND + ":" + type.name();
  }

  /** The Monday of the week being shown, i.e. the week every stored row is scoped to. */
  public LocalDate currentWeekStart() {
    return LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }

  /** Builds the current week's calendar (Monday through Sunday), with this week's rows applied. */
  public WeeklyTrainingSchedule currentWeek() {
    Map<String, StoredSessionStatus> stored =
        statusRepository.findByUserAndWeek(currentUserProvider.currentUserId(), currentWeekStart());

    Map<DayOfWeek, List<TrainingEntry>> entriesByDay = new EnumMap<>(DayOfWeek.class);
    for (DayOfWeek day : DayOfWeek.values()) {
      entriesByDay.put(day, new ArrayList<>());
    }

    for (PlannedSession planned : plannedSessions()) {
      StoredSessionStatus override = stored.get(planned.key());
      // The stored day wins over the policy's, for this week only.
      DayOfWeek day =
          (override == null || override.scheduledDay() == null)
              ? planned.defaultDay()
              : override.scheduledDay();
      entriesByDay.get(day).add(planned.toEntry(override));
    }

    List<TrainingDay> days = new ArrayList<>(DayOfWeek.values().length);
    for (DayOfWeek day : DayOfWeek.values()) {
      days.add(new TrainingDay(day, List.copyOf(entriesByDay.get(day))));
    }
    return new WeeklyTrainingSchedule(List.copyOf(days));
  }

  /**
   * This week's sessions with the days the policy assigns them, before any stored override. Running
   * first, then strength, so a day holding both lists them in that order.
   */
  public List<PlannedSession> plannedSessions() {
    List<PlannedSession> planned = new ArrayList<>();

    runningPlanService.currentPlan().stream()
        .filter(session -> session.weekNumber() == PLAN_WEEK)
        .forEach(
            session ->
                planned.add(
                    new PlannedSession(
                        runningSessionKey(session.sessionType()),
                        session.dayOfWeek(),
                        RUNNING_KIND,
                        runningTitle(session.sessionType()),
                        String.format(Locale.ROOT, "%.1f km", session.targetDistanceKm()),
                        null,
                        BodyView.FRONT)));

    WeeklyTrainingDayPolicy.strengthDays()
        .forEach(
            (day, type) ->
                workoutTemplateService
                    .findByType(type)
                    .ifPresent(
                        template ->
                            planned.add(
                                new PlannedSession(
                                    strengthSessionKey(type),
                                    day,
                                    STRENGTH_KIND,
                                    strengthTitle(type),
                                    template.items().size() + " ejercicios",
                                    type.name(),
                                    type.bodyView()))));

    return List.copyOf(planned);
  }

  /**
   * One session of the plan before any stored override is applied: what it is and which day the
   * policy puts it on.
   */
  public record PlannedSession(
      String key,
      DayOfWeek defaultDay,
      String kind,
      String title,
      String detail,
      String workoutType,
      BodyView bodyView) {

    TrainingEntry toEntry(StoredSessionStatus override) {
      String status = (override == null) ? SessionStatus.PLANNED.name() : override.status().name();
      String notes = (override == null) ? null : override.notes();
      return new TrainingEntry(key, kind, title, detail, status, notes, workoutType, bodyView);
    }
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
