package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.SessionStatus;
import java.util.Map;

/**
 * Port for persisting training session completion status (FOR-27). Owned by the application side;
 * adapters implement it (ADR-001).
 */
public interface TrainingSessionStatusRepository {

  /** All stored statuses, keyed by session id. Sessions without a row default to PLANNED. */
  Map<String, StoredSessionStatus> findAll();

  /** Inserts or updates the status (and optional notes) for a session id. */
  void upsert(String sessionId, SessionStatus status, String notes);
}
