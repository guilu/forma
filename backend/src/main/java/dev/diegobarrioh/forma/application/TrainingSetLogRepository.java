package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.LoggedSet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Port for persisting the per-set training log (training-progression-and-logging slice B, design
 * D6/D7, migration V67). Owned by the application side; adapters implement it (ADR-001).
 *
 * <p>Every operation is scoped to one {@code weekStart} and one {@code sessionKey}, mirroring
 * {@link TrainingSessionStatusRepository} since V60: a set logged one week must not leak into the
 * next, and a session's sets never mix with another session's.
 */
public interface TrainingSetLogRepository {

  /**
   * {@code userId}'s logged sets for {@code sessionKey} in the week starting at {@code weekStart}.
   * A session with nothing logged yet returns an empty list, not an error.
   */
  List<LoggedSet> findByUserWeekAndSession(UUID userId, LocalDate weekStart, String sessionKey);

  /**
   * Inserts or updates one set (design D7: the write unit is a single set, not the whole session).
   * Re-sending the same {@code (exerciseId, setNumber)} updates the existing row rather than
   * duplicating it.
   *
   * @param loggedAt when this write happened, stamped on every upsert (mirrors {@code
   *     training_session_status.completed_at}'s reasoning for "when", not "when planned")
   */
  void upsertSet(
      UUID userId, LocalDate weekStart, String sessionKey, LoggedSet set, Instant loggedAt);
}
