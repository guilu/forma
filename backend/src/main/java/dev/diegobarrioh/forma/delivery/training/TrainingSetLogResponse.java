package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.TrainingSetLogView;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/training/sessions/{sessionId}/sets}
 * (training-progression-and-logging slice B, design D6): the current template's full set grid,
 * left-joined with any logged sets. {@code sets} always mirrors the template's item/set count,
 * regardless of how much has actually been logged.
 */
public record TrainingSetLogResponse(String sessionId, List<LoggedSetResponse> sets) {

  public static TrainingSetLogResponse from(TrainingSetLogView view) {
    return new TrainingSetLogResponse(
        view.sessionId(), view.sets().stream().map(LoggedSetResponse::from).toList());
  }
}
