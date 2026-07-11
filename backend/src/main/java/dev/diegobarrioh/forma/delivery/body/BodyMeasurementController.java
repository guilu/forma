package dev.diegobarrioh.forma.delivery.body;

import dev.diegobarrioh.forma.application.BodyMeasurementService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Body measurements REST endpoints (FOR-17): {@code GET} and {@code POST} {@code
 * /api/v1/body/measurements}.
 *
 * <p>Thin controller (ADR-001, ADR-005): it validates the request DTO and maps to/from the delivery
 * read model, delegating all behavior to {@link BodyMeasurementService}. It never accepts or
 * returns domain/persistence types. Validation failures are turned into the standard {@code
 * VALIDATION_ERROR} response by the FOR-88 {@code GlobalExceptionHandler}; no error handling is
 * hand-rolled here.
 *
 * <p>Mounted under {@link ApiPaths#V1} (never a hardcoded prefix). The raw Jira text used {@code
 * /api/body/measurements}; this applies the established {@code /api/v1} versioning convention
 * (docs/api-conventions.md), documented in specs/FOR-17/spec.md.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/body/measurements")
public class BodyMeasurementController {

  private final BodyMeasurementService service;

  public BodyMeasurementController(BodyMeasurementService service) {
    this.service = service;
  }

  /** Lists measurements, most recent first (FOR-16 order). Empty list when none exist. */
  @GetMapping
  public List<BodyMeasurementResponse> list() {
    return service.list().stream().map(BodyMeasurementResponse::from).toList();
  }

  /** Records a manually entered measurement ({@code source = MANUAL}) and returns it. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BodyMeasurementResponse create(@Valid @RequestBody CreateBodyMeasurementRequest request) {
    return BodyMeasurementResponse.from(
        service.createManual(
            request.measuredAt(),
            request.weightKg(),
            request.bodyFatPercentage(),
            request.bmi(),
            request.muscleMassKg(),
            request.waterPercentage(),
            request.notes()));
  }
}
