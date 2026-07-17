package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.MuscleWorkedMapService;
import dev.diegobarrioh.forma.application.TrainingSessionStatusService;
import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService;
import dev.diegobarrioh.forma.application.WeeklyTrainingSummaryService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.SessionStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Training REST endpoints (FOR-26/FOR-27/FOR-98/FOR-136) under {@link ApiPaths#V1}{@code
 * /training}: read the weekly calendar, read the weekly adherence summary, mark a session's
 * completion status, and read a strength session's worked-muscle map.
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

  public TrainingController(
      WeeklyTrainingScheduleService scheduleService,
      TrainingSessionStatusService statusService,
      WeeklyTrainingSummaryService summaryService,
      MuscleWorkedMapService muscleWorkedMapService) {
    this.scheduleService = scheduleService;
    this.statusService = statusService;
    this.summaryService = summaryService;
    this.muscleWorkedMapService = muscleWorkedMapService;
  }

  /** Returns the current week's training calendar (Monday through Sunday). */
  @GetMapping("/week")
  public TrainingWeekResponse week() {
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
   * Worked-muscle map for a strength session (FOR-136), derived from its exercises' {@code
   * primaryMuscles}. A non-strength session (running) returns an empty map (200), never an error;
   * an unknown session id returns 404.
   */
  @GetMapping("/sessions/{sessionId}/muscle-map")
  public MuscleWorkedMapResponse muscleMap(@PathVariable String sessionId) {
    return MuscleWorkedMapResponse.from(muscleWorkedMapService.resolve(sessionId));
  }
}
