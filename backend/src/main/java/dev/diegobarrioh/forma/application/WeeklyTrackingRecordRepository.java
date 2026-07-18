package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.util.List;
import java.util.Optional;

/**
 * Port for persisting and reading {@link WeeklyTrackingRecord}s (FOR-155). Owned by the
 * application/domain side; adapters implement it (ADR-001). Every method is owner-scoped (ADR-002)
 * — the caller always supplies the owner id, the adapter never returns another owner's rows,
 * mirroring {@code GoalRepository}.
 */
public interface WeeklyTrackingRecordRepository {

  /**
   * All of {@code ownerId}'s weekly tracking records, most recent week first. Empty when none are
   * stored yet — SEGUIMIENTO starts empty (spec FOR-155), this is the default state, not an error.
   */
  List<WeeklyTrackingRecord> findAllByOwner(String ownerId);

  /**
   * Finds {@code ownerId}'s record for {@code week}; empty if no record exists for that week or it
   * belongs to another owner.
   */
  Optional<WeeklyTrackingRecord> findByOwnerAndWeek(String ownerId, int week);

  /**
   * Inserts or updates {@code ownerId}'s record for {@code record.week()} (one record per week;
   * spec FOR-155 Edge Cases: "Week uniqueness"). Returns the persisted record.
   */
  WeeklyTrackingRecord upsert(String ownerId, WeeklyTrackingRecord record);
}
