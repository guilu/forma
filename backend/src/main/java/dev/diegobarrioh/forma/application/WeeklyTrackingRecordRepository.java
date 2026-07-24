package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and reading {@link WeeklyTrackingRecord}s (FOR-155). Owned by the
 * application/domain side; adapters implement it (ADR-001). Every method is owner-scoped (ADR-002)
 * — the caller always supplies the owner id, the adapter never returns another owner's rows,
 * mirroring {@code GoalRepository}.
 *
 * <p>{@code userId} is a real account id (FOR-145b-1, migration V27) — {@code
 * weekly_tracking_record.user_id UUID}, FK-referencing {@code users(id)}.
 */
public interface WeeklyTrackingRecordRepository {

  /**
   * All of {@code userId}'s weekly tracking records, most recent week first. Empty when none are
   * stored yet — SEGUIMIENTO starts empty (spec FOR-155), this is the default state, not an error.
   */
  List<WeeklyTrackingRecord> findAllByOwner(UUID userId);

  /**
   * Finds {@code userId}'s record for {@code week}; empty if no record exists for that week or it
   * belongs to another owner.
   */
  Optional<WeeklyTrackingRecord> findByOwnerAndWeek(UUID userId, int week);

  /**
   * Inserts or updates {@code userId}'s record for {@code record.week()} (one record per week; spec
   * FOR-155 Edge Cases: "Week uniqueness"). Returns the persisted record.
   */
  WeeklyTrackingRecord upsert(UUID userId, WeeklyTrackingRecord record);
}
