package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.SessionStatus;
import java.util.Map;
import java.util.UUID;

/**
 * Port for persisting training session completion status (FOR-27). Owned by the application side;
 * adapters implement it (ADR-001).
 *
 * <p>{@code userId} is a real account id (FOR-145c "gap table" closure, migration V31) — {@code
 * training_session_status}'s primary key was rebuilt from the bare {@code session_id} (a
 * day-of-week-keyed id shared by every user, e.g. {@code "SATURDAY:RUNNING"} — a genuine cross-user
 * collision bug) to the composite {@code (user_id, session_id)}. Before this slice the table had NO
 * owner-scoping at all.
 */
public interface TrainingSessionStatusRepository {

  /**
   * All of {@code userId}'s stored statuses, keyed by session id. Sessions without a row default to
   * PLANNED.
   */
  Map<String, StoredSessionStatus> findAllByUser(UUID userId);

  /** Inserts or updates the status (and optional notes) for {@code userId}'s session id. */
  void upsert(UUID userId, String sessionId, SessionStatus status, String notes);
}
