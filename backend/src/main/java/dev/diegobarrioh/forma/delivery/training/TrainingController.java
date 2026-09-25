package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.MuscleWorkedMapService;
import dev.diegobarrioh.forma.application.PlanRestartService;
import dev.diegobarrioh.forma.application.TrainingSessionRescheduleService;
import dev.diegobarrioh.forma.application.TrainingSessionStatusService;
import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService;
import dev.diegobarrioh.forma.application.WeeklyTrainingSummaryService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.SessionStatus;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Training REST endpoints (FOR-26/FOR-27/FOR-98/FOR-136) under {@link ApiPaths#V1}{@code
 * /training}: read the weekly calendar, read the weekly adherence summary, mark a session's
 * completion status, move a session to another day of the week, and read a strength session's
 * worked-muscle map.
 *
 * <p>Thin controller (ADR-001, ADR-005): it maps to/from delivery DTOs and delegates to the
 * application services. Validation and not-found failures are turned into the standard {@code
 * ApiError} shapes by the FOR-88/FOR-27 {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/training")
public class TrainingController {

  private final WeeklyTrainingScheduleService scheduleService;
  private final TrainingSessionStatusService statusService;
  private final WeeklyTrainingSummaryService summaryService;
  private final MuscleWorkedMapService muscleWorkedMapService;
  private final TrainingSessionRescheduleService rescheduleService;
  private final PlanRestartService restartService;

  public TrainingController(
      WeeklyTrainingScheduleService scheduleService,
      TrainingSessionStatusService statusService,
      WeeklyTrainingSummaryService summaryService,
      MuscleWorkedMapService muscleWorkedMapService,
      TrainingSessionRescheduleService rescheduleService,
      PlanRestartService restartService) {
    this.scheduleService = scheduleService;
    this.statusService = statusService;
    this.summaryService = summaryService;
    this.muscleWorkedMapService = muscleWorkedMapService;
    this.rescheduleService = rescheduleService;
    this.restartService = restartService;
  }

  /**
   * Returns the current week's training calendar (Monday through Sunday). Always 200 (design D2 of
   * training-progression-and-logging): whether the account never accepted a plan, is mid-cycle, or
   * finished it, is answered by {@code planState} in the body, not by the status code.
   *
   * <p>There used to be a second gate here on the account having accepted its plan (V58), separate
   * from {@link WeeklyTrainingScheduleService}'s own read of that fact. That duplication is gone:
   * the schedule service's {@code PlanAcceptanceRepository} read is now the single portero both the
   * schedule and this response derive from, so "gate says yes but no acceptance instant exists" is
   * unreachable by construction.
   */
  @GetMapping("/week")
  public TrainingWeekResponse week() {
    return TrainingWeekResponse.from(scheduleService.currentWeek());
  }

  /**
   * Starts a new 16-week cycle for an account whose plan already reached its terminal state (design
   * D5 of training-progression-and-logging): reanchors the cycle to now, so the next {@code GET
   * /training/week} derives week 1 again.
   */
  @PostMapping("/plan/restart")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void restartPlan() {
    restartService.restart();
  }

  /** Returns the current week's training adherence summary (FOR-28). */
  @GetMapping("/weekly-summary")
  public WeeklyTrainingSummaryResponse weeklySummary() {
    return WeeklyTrainingSummaryResponse.from(summaryService.currentSummary());
  }

  /** Marks a session's completion status (running or strength). */
  @PatchMapping("/sessions/{id}/status")
  public SessionStatusResponse updateStatus(
      @PathVariable String id, @Valid @RequestBody UpdateSessionStatusRequest request) {
    return SessionStatusResponse.from(
        statusService.updateStatus(id, SessionStatus.valueOf(request.status()), request.notes()));
  }

  /**
   * Moves a session to another day of the current week (V60), or back to its planned day when
   * {@code day} is null. The move lasts this week only; next Monday the plan is on its policy days
   * again.
   */
  @PatchMapping("/sessions/{id}/schedule")
  public TrainingWeekResponse reschedule(
      @PathVariable String id, @Valid @RequestBody RescheduleSessionRequest request) {
    rescheduleService.reschedule(
        id, request.day() == null ? null : DayOfWeek.valueOf(request.day()));
    // The whole week comes back: moving one session changes which day every other session shares
    // it with, and the caller would otherwise have to refetch to redraw the calendar anyway.
    return TrainingWeekResponse.from(scheduleService.currentWeek());
  }

  /**
   * Worked-muscle map for a strength session (FOR-136), derived from its exercises' {@code
   * primaryMuscles}. A non-strength session (running) returns an empty map (200), never an error;
   * an unknown session id returns 404.
   */
  @GetMapping("/sessions/{sessionId}/muscle-map")
  public MuscleWorkedMapResponse muscleMap(@PathVariable String sessionId) {
    return MuscleWorkedMapResponse.from(muscleWorkedMapService.resolve(sessionId));
  }
}
