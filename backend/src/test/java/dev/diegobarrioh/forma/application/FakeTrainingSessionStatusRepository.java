package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory {@link TrainingSessionStatusRepository} for unit tests (no Spring — ADR-007).
 *
 * <p>One shared fake rather than a copy per test class: it is keyed by owner and week exactly like
 * the real table's {@code (user_id, week_start, session_key)} primary key (migrations V31 and V60),
 * and four separate copies of that keying were four chances for a test to quietly disagree with the
 * schema about when a status expires or whose it is.
 */
public final class FakeTrainingSessionStatusRepository implements TrainingSessionStatusRepository {

  private final Map<OwnerWeek, Map<String, StoredSessionStatus>> byOwnerWeek = new HashMap<>();

  @Override
  public Map<String, StoredSessionStatus> findByUserAndWeek(UUID userId, LocalDate weekStart) {
    return byOwnerWeek.getOrDefault(new OwnerWeek(userId, weekStart), Map.of());
  }

  @Override
  public void upsertStatus(
      UUID userId,
      LocalDate weekStart,
      String sessionKey,
      SessionStatus status,
      Instant completedAt,
      String notes) {
    Map<String, StoredSessionStatus> week = week(userId, weekStart);
    StoredSessionStatus previous = week.get(sessionKey);
    // A status write must not silently undo a move, the same way the SQL UPDATE leaves
    // scheduled_day alone.
    DayOfWeek scheduledDay = (previous == null) ? null : previous.scheduledDay();
    week.put(
        sessionKey, new StoredSessionStatus(sessionKey, status, scheduledDay, completedAt, notes));
  }

  @Override
  public void upsertScheduledDay(
      UUID userId, LocalDate weekStart, String sessionKey, DayOfWeek scheduledDay) {
    Map<String, StoredSessionStatus> week = week(userId, weekStart);
    StoredSessionStatus previous = week.get(sessionKey);
    week.put(
        sessionKey,
        previous == null
            ? new StoredSessionStatus(sessionKey, SessionStatus.PLANNED, scheduledDay, null, null)
            : new StoredSessionStatus(
                sessionKey,
                previous.status(),
                scheduledDay,
                previous.completedAt(),
                previous.notes()));
  }

  private Map<String, StoredSessionStatus> week(UUID userId, LocalDate weekStart) {
    return byOwnerWeek.computeIfAbsent(
        new OwnerWeek(userId, weekStart), key -> new LinkedHashMap<>());
  }

  private record OwnerWeek(UUID userId, LocalDate weekStart) {}
}
