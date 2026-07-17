package dev.diegobarrioh.forma.delivery.training;

import dev.diegobarrioh.forma.application.MuscleWorkedMap;
import dev.diegobarrioh.forma.application.MuscleWorkedMap.MuscleWorked;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/training/sessions/{sessionId}/muscle-map} (FOR-136 api.md):
 * the session's worked muscles with their derived load. {@code muscles} is empty for a non-strength
 * session (never an error).
 */
public record MuscleWorkedMapResponse(String sessionId, List<MuscleResponse> muscles) {

  public record MuscleResponse(String muscle, String load) {
    static MuscleResponse from(MuscleWorked muscleWorked) {
      return new MuscleResponse(muscleWorked.muscle(), muscleWorked.load().name());
    }
  }

  public static MuscleWorkedMapResponse from(MuscleWorkedMap map) {
    return new MuscleWorkedMapResponse(
        map.sessionId(), map.muscles().stream().map(MuscleResponse::from).toList());
  }
}
