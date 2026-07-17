package dev.diegobarrioh.forma.delivery.progress;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.diegobarrioh.forma.application.AchievementView;
import dev.diegobarrioh.forma.application.AchievementsView;
import java.time.Instant;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/progress/achievements} (FOR-135 api.md): earned achievements
 * (with {@code earnedAt}) separated from the still-available ones.
 *
 * <p>Delivery read model, distinct from the application {@link AchievementsView} (ADR-005),
 * mirroring {@code AdherenceResponse}'s from-view convention. {@code earnedAt} uses the
 * {@code @JsonInclude(NON_NULL)} convention shared with {@code BodyMeasurementResponse}/{@code
 * UserProfileResponse}: present (as an ISO-8601 instant) on earned entries, entirely absent from
 * the JSON object on available entries (spec FOR-135 api.md — "available entries do not carry
 * earnedAt", not just a JSON {@code null}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AchievementsResponse(
    List<AchievementEntryResponse> earned, List<AchievementEntryResponse> available) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record AchievementEntryResponse(
      String id, String title, String description, Instant earnedAt) {

    static AchievementEntryResponse from(AchievementView view) {
      return new AchievementEntryResponse(
          view.id(), view.title(), view.description(), view.earnedAt());
    }
  }

  public static AchievementsResponse from(AchievementsView view) {
    return new AchievementsResponse(
        view.earned().stream().map(AchievementEntryResponse::from).toList(),
        view.available().stream().map(AchievementEntryResponse::from).toList());
  }
}
