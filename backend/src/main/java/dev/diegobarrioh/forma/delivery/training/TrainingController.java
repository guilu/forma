package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.MuscleWorkedMapService;
import dev.diegobarrioh.forma.application.PlanActivationService;
import dev.diegobarrioh.forma.application.TrainingSessionRescheduleService;
import dev.diegobarrioh.forma.application.TrainingSessionStatusService;
import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService;
import dev.diegobarrioh.forma.application.WeeklyTrainingSummaryService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.SessionStatus;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
  private final PlanActivationService planActivationService;
  private final TrainingSessionRescheduleService rescheduleService;

  public TrainingController(
      WeeklyTrainingScheduleService scheduleService,
      TrainingSessionStatusService statusService,
      WeeklyTrainingSummaryService summaryService,
      MuscleWorkedMapService muscleWorkedMapService,
      PlanActivationService planActivationService,
      TrainingSessionRescheduleService rescheduleService) {
    this.scheduleService = scheduleService;
    this.statusService = statusService;
    this.summaryService = summaryService;
    this.muscleWorkedMapService = muscleWorkedMapService;
    this.planActivationService = planActivationService;
    this.rescheduleService = rescheduleService;
  }

  /**
   * Returns the current week's training calendar (Monday through Sunday).
   *
   * <p>Gated on the account having ACCEPTED its plan (V58), not on having filled in the onboarding
   * form. They used to be the same check and they are not the same question: V57 left accounts
   * holding a seeded plan with an unset onboarding flag, and this endpoint answered "no training"
   * to somebody whose plan was sitting right there.
   *
   * <p>The nutrition endpoints need no equivalent check — their plan is a row whose status already
   * says whether it is being followed. This one's plan lives in code ({@code
   * RunningPlanGenerator}), so the acceptance is the only thing there is to ask.
   */
  @GetMapping("/week")
  public TrainingWeekResponse week() {
    if (!planActivationService.accepted()) {
      return TrainingWeekResponse.empty();
    }
    return TrainingWeekResponse.from(scheduleService.currentWeek());
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
