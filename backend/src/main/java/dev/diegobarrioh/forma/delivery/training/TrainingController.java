package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Training calendar REST endpoint (FOR-26): {@code GET /api/v1/training/week}.
 *
 * <p>Thin controller (ADR-001, ADR-005): it maps the composed {@link WeeklyTrainingScheduleService}
 * result to the delivery read model and returns it, never exposing application/domain types.
 * Mounted under {@link ApiPaths#V1}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/training")
public class TrainingController {

  private final WeeklyTrainingScheduleService service;

  public TrainingController(WeeklyTrainingScheduleService service) {
    this.service = service;
  }

  /** Returns the current week's training calendar (Monday through Sunday). */
  @GetMapping("/week")
  public TrainingWeekResponse week() {
    return TrainingWeekResponse.from(service.currentWeek());
  }
}
