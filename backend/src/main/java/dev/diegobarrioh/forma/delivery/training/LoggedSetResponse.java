package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.domain.LoggedSet;

/**
 * One set in a {@link TrainingSetLogResponse}, and the response body for {@code PUT
 * /api/v1/training/sessions/{sessionId}/sets/{exerciseId}/{setNumber}} (training-progression-and-
 * logging slice B, design D7): a not-yet-logged set has {@code weightKg}/{@code reps} as {@code
 * null} and {@code done} as {@code false}, distinct from an actual zero.
 */
public record LoggedSetResponse(
    String exerciseId, int setNumber, Double weightKg, Integer reps, boolean done) {

  public static LoggedSetResponse from(LoggedSet set) {
    return new LoggedSetResponse(
        set.exerciseId(), set.setNumber(), set.weightKg(), set.reps(), set.done());
  }
}
