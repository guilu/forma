package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.LoggedSet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory {@link TrainingSetLogRepository} for unit tests (no Spring — ADR-007), mirroring
 * {@link FakeTrainingSessionStatusRepository}: keyed exactly like the real table's {@code
 * (user_id, week_start, session_key, exercise_id, set_number)} primary key (migration V67) so a
 * test cannot quietly disagree with the schema about whose row is whose.
 */
public final class FakeTrainingSetLogRepository implements TrainingSetLogRepository {

  private final Map<OwnerWeekSession, List<LoggedSet>> byOwnerWeekSession = new HashMap<>();

  @Override
  public List<LoggedSet> findByUserWeekAndSession(
      UUID userId, LocalDate weekStart, String sessionKey) {
    return List.copyOf(
        byOwnerWeekSession.getOrDefault(
            new OwnerWeekSession(userId, weekStart, sessionKey), List.of()));
  }

  @Override
  public void upsertSet(
      UUID userId, LocalDate weekStart, String sessionKey, LoggedSet set, Instant loggedAt) {
    List<LoggedSet> sets =
        byOwnerWeekSession.computeIfAbsent(
            new OwnerWeekSession(userId, weekStart, sessionKey), key -> new ArrayList<>());
    sets.removeIf(
        existing ->
            existing.exerciseId().equals(set.exerciseId())
                && existing.setNumber() == set.setNumber());
    sets.add(set);
  }

  private record OwnerWeekSession(UUID userId, LocalDate weekStart, String sessionKey) {}
}
