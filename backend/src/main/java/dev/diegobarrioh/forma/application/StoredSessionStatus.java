package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.DayOfWeek;
import java.time.Instant;

/**
 * A persisted training session override (FOR-27, re-keyed by migration V60): what the user recorded
 * about one session of one week.
 *
 * <p>Carries the three facts the old {@code "MONDAY:RUNNING"} row fused into a single string — what
 * the session is ({@code sessionKey}), when it is planned ({@code scheduledDay}) and when it was
 * actually done ({@code completedAt}). Rows live inside a week, so the absence of a row means
 * "PLANNED, on its policy day" for that week rather than forever.
 *
 * @param sessionKey the session's identity, with no day in it (e.g. {@code "RUNNING:LONG_RUN"})
 * @param status the recorded status
 * @param scheduledDay the day this session was moved to, or {@code null} to keep its policy day
 * @param completedAt when it was marked COMPLETED, or {@code null} if it is not completed
 * @param notes optional completion note, or {@code null}
 */
public record StoredSessionStatus(
    String sessionKey,
    SessionStatus status,
    DayOfWeek scheduledDay,
    Instant completedAt,
    String notes) {}
