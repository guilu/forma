package dev.diegobarrioh.forma.delivery.tracking;

import dev.diegobarrioh.forma.application.WeeklyTrackingRecordService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Weekly tracking record REST endpoints (FOR-155): {@code GET}/{@code POST} {@code
 * /api/v1/tracking/weekly} and {@code GET .../{week}}.
 *
 * <p>Thin controller (ADR-001, ADR-005): validates the request DTO and maps to/from the delivery
 * read model, delegating all behavior to {@link WeeklyTrackingRecordService}. It never accepts or
 * returns domain/persistence types. Missing weeks and validation failures are turned into the
 * standard {@code ApiError} shape by the FOR-88 {@code GlobalExceptionHandler}; no error handling
 * is hand-rolled here.
 *
 * <p>Mounted under {@link ApiPaths#V1}{@code /tracking/weekly} (spec FOR-155 api.md). {@code POST}
 * always returns {@code 200 OK} with the persisted record — it is documented as create-or-update
 * (upsert) by week rather than a pure "create" operation (spec FOR-155 api.md: "Create/upsert a
 * weekly record for a given week"), so a single fixed status keeps client handling simple instead
 * of branching on whether the week already existed.
 *
 * <p>Single-user MVP (ADR-002): every endpoint operates on the one account {@link
 * WeeklyTrackingRecordService} resolves internally; no account/owner path segment or auth header is
 * accepted yet (documented MVP limitation, AGENTS.md).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/tracking/weekly")
public class WeeklyTrackingRecordController {

  private final WeeklyTrackingRecordService service;

  public WeeklyTrackingRecordController(WeeklyTrackingRecordService service) {
    this.service = service;
  }

  /**
   * Lists the owner's weekly tracking records, most recent week first. Empty list (never 404) when
   * SEGUIMIENTO has no rows yet — the agreed empty-start default (spec FOR-155).
   */
  @GetMapping
  public List<WeeklyTrackingRecordResponse> list() {
    return service.list().stream().map(WeeklyTrackingRecordResponse::from).toList();
  }

  /** Creates or updates the week's record (upsert by {@code week}) and returns it. */
  @PostMapping
  public WeeklyTrackingRecordResponse save(
      @Valid @RequestBody CreateWeeklyTrackingRecordRequest request) {
    WeeklyTrackingRecord record =
        new WeeklyTrackingRecord(
            request.week(),
            request.date(),
            request.weightKg(),
            request.bodyFatPercentage(),
            request.bmi(),
            request.runningKm(),
            request.pace4kmMinPerKm(),
            request.recommendedKcal(),
            request.comment());
    return WeeklyTrackingRecordResponse.from(service.save(record));
  }

  /** Reads a single week's record; 404 when no record exists for that week. */
  @GetMapping("/{week}")
  public WeeklyTrackingRecordResponse getByWeek(@PathVariable int week) {
    return WeeklyTrackingRecordResponse.from(service.getByWeek(week));
  }
}
