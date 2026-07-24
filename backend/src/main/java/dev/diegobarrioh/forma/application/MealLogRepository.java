package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.MealLogEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Port for persisting and reading logged meal entries (FOR-127). Owned by the application/domain
 * side; adapters implement it (ADR-001). Every method is owner-scoped (ADR-002) — the caller always
 * supplies the owner id, the adapter never returns another owner's rows. Append-only for this slice
 * (spec FOR-127 Open Questions): no update/delete method exists yet.
 *
 * <p>{@code userId} is a real account id (FOR-145b-1, migration V27) — {@code
 * meal_log_entry.user_id UUID}, FK-referencing {@code users(id)}.
 */
public interface MealLogRepository {

  /**
   * {@code userId}'s logged entries for {@code date}, in the order they were logged. Empty when
   * nothing has been logged yet — never an error (spec FOR-127 edge case).
   */
  List<StoredMealLogEntry> findByOwnerAndDate(UUID userId, LocalDate date);

  /** Persists a new entry for {@code userId}, generating and returning its id. */
  StoredMealLogEntry save(UUID userId, MealLogEntry entry);
}
