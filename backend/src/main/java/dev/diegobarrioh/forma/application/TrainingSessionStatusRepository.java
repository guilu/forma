package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Port for persisting training session overrides (FOR-27). Owned by the application side; adapters
 * implement it (ADR-001).
 *
 * <p>Every operation is scoped to one {@code weekStart} (that week's Monday) as well as one owner,
 * since migration V60. Before it, rows were keyed by {@code (user_id, session_id)} with the day of
 * the week baked into the id, so a status recorded once applied to every subsequent week for ever —
 * there was no week to scope a read to.
 */
public interface TrainingSessionStatusRepository {

  /**
   * {@code userId}'s stored overrides for the week starting at {@code weekStart}, keyed by session
   * key. Sessions without a row that week default to PLANNED, on their policy day.
   */
  Map<String, StoredSessionStatus> findByUserAndWeek(UUID userId, LocalDate weekStart);

  /**
   * Inserts or updates the status of one session in one week.
   *
   * <p>{@code completedAt} is the moment the session was actually done — which need not be the day
   * it was planned for, and is exactly the fact the pre-V60 table never stored.
   */
  void upsertStatus(
      UUID userId,
      LocalDate weekStart,
      String sessionKey,
      SessionStatus status,
      Instant completedAt,
      String notes);

  /**
   * Moves one session to {@code scheduledDay} for that week only, leaving its status untouched.
   * Passing {@code null} restores the day {@code WeeklyTrainingDayPolicy} assigns it.
   */
  void upsertScheduledDay(
      UUID userId, LocalDate weekStart, String sessionKey, DayOfWeek scheduledDay);
}
