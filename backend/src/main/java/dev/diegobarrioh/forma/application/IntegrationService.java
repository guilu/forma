package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.SyncOutcome;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application use cases for provider-neutral integration connections (FOR-126, first implementable
 * slice of FOR-103): status read model, connect/disconnect/manual-sync. No real OAuth, no token
 * storage, no real provider sync — this slice's connect is a mock and its sync is a stub/no-op
 * import (spec FOR-126 Summary).
 *
 * <p>Single-user MVP (ADR-002): every use case operates on the one {@link #OWNER_ID} row, mirroring
 * {@link UserProfileService#OWNER_ID} / {@link GoalService#OWNER_ID}. No shared "current account"
 * abstraction exists yet, so this constant is duplicated here (same rationale as {@code
 * GoalService}) and will collapse onto a real account id once authentication lands.
 */
@Service
public class IntegrationService {

  /** Fixed single-user owner id for the MVP (ADR-002), mirroring {@link GoalService#OWNER_ID}. */
  public static final String OWNER_ID = "default-user";

  private final IntegrationRepository repository;

  public IntegrationService(IntegrationRepository repository) {
    this.repository = repository;
  }

  /**
   * Per-provider connection status for every known {@link IntegrationProvider}, defaulting to
   * {@link IntegrationStatus#DISCONNECTED} for any provider never connected (spec FOR-126 Edge
   * Cases: "GET before any connection → 200 with all known providers DISCONNECTED, never 404").
   */
  public List<IntegrationConnection> status() {
    return List.of(IntegrationProvider.values()).stream().map(this::currentOrDefault).toList();
  }

  /**
   * Marks {@code provider} connected (mock, no OAuth this slice). Idempotent when already connected
   * (spec FOR-126 Open Questions).
   */
  public IntegrationConnection connect(IntegrationProvider provider) {
    IntegrationConnection connected = currentOrDefault(provider).connect(Instant.now());
    return repository.save(OWNER_ID, connected);
  }

  /**
   * Marks {@code provider} disconnected. Idempotent no-op when already disconnected (spec FOR-126
   * Edge Cases).
   */
  public IntegrationConnection disconnect(IntegrationProvider provider) {
    IntegrationConnection disconnected = currentOrDefault(provider).disconnect();
    return repository.save(OWNER_ID, disconnected);
  }

  /**
   * Triggers a manual sync. When connected, performs a stub/no-op import — {@code importedCount} is
   * always {@code 0}, never fabricated (spec FOR-126 Functional Requirements) — persists the
   * outcome and returns it. When disconnected, resolves the spec's open question by returning a
   * readable {@link SyncResult#NOT_CONNECTED} outcome instead of a 409 (keeps the FOR-57/FOR-123
   * frontend error handling simple, spec FOR-126 Edge Cases) — nothing is persisted, since no sync
   * actually ran.
   */
  public IntegrationConnection sync(IntegrationProvider provider) {
    IntegrationConnection current = currentOrDefault(provider);
    if (current.status() == IntegrationStatus.DISCONNECTED) {
      SyncOutcome notConnected =
          new SyncOutcome(SyncResult.NOT_CONNECTED, 0, "El proveedor no está conectado.");
      return current.withSyncOutcome(current.lastSyncAt(), notConnected);
    }
    SyncOutcome stubOutcome = new SyncOutcome(SyncResult.OK, 0, null);
    IntegrationConnection synced = current.withSyncOutcome(Instant.now(), stubOutcome);
    return repository.save(OWNER_ID, synced);
  }

  private IntegrationConnection currentOrDefault(IntegrationProvider provider) {
    return repository
        .findByOwnerAndProvider(OWNER_ID, provider)
        .orElseGet(() -> IntegrationConnection.disconnectedDefault(provider));
  }
}
