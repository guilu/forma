package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.TrainingSetLogService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.LoggedSet;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-set training-log REST endpoints (training-progression-and-logging slice B, design D6/D7)
 * under {@link ApiPaths#V1}{@code /training}.
 *
 * <p>A separate controller from {@link TrainingController} (single responsibility, and keeps this
 * slice's diff isolated from the already-shipped training endpoints): reads and writes the per-set
 * log, nothing else.
 *
 * <p>Thin controller (ADR-001, ADR-005): it maps to/from delivery DTOs and delegates to {@link
 * TrainingSetLogService}. Validation and not-found failures are turned into the standard {@code
 * ApiError} shapes by {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/training")
public class TrainingSetLogController {

  private final TrainingSetLogService setLogService;

  public TrainingSetLogController(TrainingSetLogService setLogService) {
    this.setLogService = setLogService;
  }

  /**
   * The current week's full set grid for {@code sessionId} — one entry per {@code (exerciseId,
   * setNumber)} the current template prescribes, left-joined with anything already logged (design
   * D6).
   */
  @GetMapping("/sessions/{sessionId}/sets")
  public TrainingSetLogResponse getSets(@PathVariable String sessionId) {
    return TrainingSetLogResponse.from(setLogService.getSets(sessionId));
  }

  /**
   * Writes one set — not the whole session (design D7). Returns just the written set, not the whole
   * grid: unlike a session move, writing one set never changes any other set's identity.
   */
  @PutMapping("/sessions/{sessionId}/sets/{exerciseId}/{setNumber}")
  public LoggedSetResponse putSet(
      @PathVariable String sessionId,
      @PathVariable String exerciseId,
      @PathVariable int setNumber,
      @Valid @RequestBody LogSetRequest request) {
    LoggedSet set =
        setLogService.putSet(
            sessionId, exerciseId, setNumber, request.weightKg(), request.reps(), request.done());
    return LoggedSetResponse.from(set);
  }
}
