package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.SessionStatus;

/**
 * A persisted training session status override (FOR-27): the {@link SessionStatus} (and optional
 * notes) recorded for a calendar session id.
 *
 * @param sessionId stable calendar session id (e.g. {@code "SATURDAY:RUNNING"})
 * @param status the recorded status
 * @param notes optional completion note, or {@code null}
 */
public record StoredSessionStatus(String sessionId, SessionStatus status, String notes) {}
