package dev.diegobarrioh.forma.delivery.progress;

import dev.diegobarrioh.forma.application.WeeklyHistory;
import dev.diegobarrioh.forma.domain.WeeklyHistoryBucket;
import java.time.LocalDate;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/progress/weekly-history} (FOR-139 api.md).
 *
 * <p>Delivery read model, distinct from the application {@link WeeklyHistory} (ADR-005), mirroring
 * {@code AdherenceResponse}'s from-view convention.
 */
public record WeeklyHistoryResponse(List<WeekBucketResponse> weeks) {

  public record WeekBucketResponse(LocalDate weekStart, int planned, int completed) {

    static WeekBucketResponse from(WeeklyHistoryBucket bucket) {
      return new WeekBucketResponse(bucket.weekStart(), bucket.planned(), bucket.completed());
    }
  }

  public static WeeklyHistoryResponse from(WeeklyHistory weeklyHistory) {
    return new WeeklyHistoryResponse(
        weeklyHistory.weeks().stream().map(WeekBucketResponse::from).toList());
  }
}
