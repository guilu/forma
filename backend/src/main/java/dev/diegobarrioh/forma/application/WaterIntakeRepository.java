package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.WaterIntakeEntry;
import java.time.LocalDate;
import java.util.List;

/**
 * Port for persisting and reading logged water-intake entries (FOR-130). Owned by the
 * application/domain side; adapters implement it (ADR-001). Every method is owner-scoped (ADR-002)
 * — the caller always supplies the owner id, the adapter never returns another owner's rows.
 * Append-only for this slice (spec FOR-130 Open Questions): no update/delete method exists yet,
 * mirroring {@code MealLogRepository} (FOR-127).
 */
public interface WaterIntakeRepository {

  /**
   * {@code ownerId}'s logged entries for {@code date}, in the order they were logged. Empty when
   * nothing has been logged yet — never an error (spec FOR-130 edge case).
   */
  List<StoredWaterIntakeEntry> findByOwnerAndDate(String ownerId, LocalDate date);

  /** Persists a new entry for {@code ownerId}, generating and returning its id. */
  StoredWaterIntakeEntry save(String ownerId, WaterIntakeEntry entry);
}
