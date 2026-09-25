package dev.diegobarrioh.forma.application;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory {@link PlanAcceptanceRepository} for unit tests (no Spring — ADR-007), mirroring the
 * real table's insert-if-absent semantics for {@code accepted_at} and, separately, a restartable
 * {@code cycle_started_at} (D5).
 */
public final class FakePlanAcceptanceRepository implements PlanAcceptanceRepository {

  private final Map<UUID, Instant> acceptedAt = new HashMap<>();
  private final Map<UUID, Instant> cycleStartedAt = new HashMap<>();

  @Override
  public boolean accepted(UUID userId) {
    return acceptedAt.containsKey(userId);
  }

  @Override
  public void markAccepted(UUID userId, Instant at) {
    acceptedAt.putIfAbsent(userId, at);
  }

  @Override
  public Optional<Instant> planStartedAt(UUID userId) {
    Instant cycle = cycleStartedAt.get(userId);
    return cycle != null ? Optional.of(cycle) : Optional.ofNullable(acceptedAt.get(userId));
  }

  /** Test seam for D5: restarts the cycle at {@code at} without touching {@code accepted_at}. */
  public void restartCycleAt(UUID userId, Instant at) {
    cycleStartedAt.put(userId, at);
  }
}
