package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Computes the weekly training summary (FOR-28) from the FOR-26 schedule and FOR-27 completion
 * status.
 *
 * <p>Rule-based and deterministic (counts + sums, no forecasting), computed on demand — no
 * persisted summary entity and no HTTP endpoint (spec FOR-28), mirroring the FOR-21 body summary.
 * Session counts and statuses come from the composed week; running distances come from the plan and
 * are matched to sessions by their stable id. Completed running distance includes only completed
 * sessions.
 */
@Service
public class WeeklyTrainingSummaryService {

  private static final String COMPLETED = SessionStatus.COMPLETED.name();

  private final WeeklyTrainingScheduleService scheduleService;
  private final RunningPlanService runningPlanService;

  public WeeklyTrainingSummaryService(
      WeeklyTrainingScheduleService scheduleService, RunningPlanService runningPlanService) {
    this.scheduleService = scheduleService;
    this.runningPlanService = runningPlanService;
  }

  /** Computes the current week's training summary. */
  public WeeklyTrainingSummary currentSummary() {
    List<TrainingEntry> entries =
        scheduleService.currentWeek().days().stream()
            .flatMap(day -> day.entries().stream())
            .toList();

    List<TrainingEntry> running = entries.stream().filter(isKind("RUNNING")).toList();
    List<TrainingEntry> strength = entries.stream().filter(isKind("STRENGTH")).toList();

    int plannedRunning = running.size();
    int completedRunning = (int) running.stream().filter(this::isCompleted).count();
    int plannedStrength = strength.size();
    int completedStrength = (int) strength.stream().filter(this::isCompleted).count();

    Map<String, Double> kmById = plannedRunningDistancesById();
    double totalPlannedKm = round(sumKm(running, kmById, entry -> true));
    double completedKm = round(sumKm(running, kmById, this::isCompleted));

    String message =
        message(
            plannedRunning,
            completedRunning,
            plannedStrength,
            completedStrength,
            totalPlannedKm,
            completedKm);

    return new WeeklyTrainingSummary(
        plannedRunning,
        completedRunning,
        plannedStrength,
        completedStrength,
        totalPlannedKm,
        completedKm,
        message);
  }

  private Map<String, Double> plannedRunningDistancesById() {
    return runningPlanService.currentPlan().stream()
        .filter(session -> session.weekNumber() == WeeklyTrainingScheduleService.PLAN_WEEK)
        .collect(
            Collectors.toMap(
                session -> WeeklyTrainingScheduleService.sessionId(session.dayOfWeek(), "RUNNING"),
                dev.diegobarrioh.forma.domain.RunningPlanSession::targetDistanceKm));
  }

  private static double sumKm(
      List<TrainingEntry> running, Map<String, Double> kmById, Predicate<TrainingEntry> filter) {
    return running.stream()
        .filter(filter)
        .mapToDouble(entry -> kmById.getOrDefault(entry.id(), 0.0))
        .sum();
  }

  private static Predicate<TrainingEntry> isKind(String kind) {
    return entry -> entry.kind().equals(kind);
  }

  private boolean isCompleted(TrainingEntry entry) {
    return COMPLETED.equals(entry.status());
  }

  private static double round(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private static String message(
      int plannedRunning,
      int completedRunning,
      int plannedStrength,
      int completedStrength,
      double totalPlannedKm,
      double completedKm) {
    if (plannedRunning + plannedStrength == 0) {
      return "No hay entrenamientos planificados esta semana.";
    }
    return String.format(
        Locale.ROOT,
        "Carrera: %d/%d sesiones (%.1f/%.1f km). Fuerza: %d/%d sesiones.",
        completedRunning,
        plannedRunning,
        completedKm,
        totalPlannedKm,
        completedStrength,
        plannedStrength);
  }
}
