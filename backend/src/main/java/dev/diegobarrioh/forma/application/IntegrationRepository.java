package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and reading {@link IntegrationConnection}s (FOR-126). Owned by the
 * application/domain side; adapters implement it (ADR-001). Every method is owner-scoped (ADR-002)
 * — the caller always supplies the owner id, the adapter never returns another owner's rows.
 *
 * <p><b>Token-free by design</b> (ADR-004, spec FOR-126 boundary rule): this port declares no
 * accessor that reads or writes a token/secret. Later FOR-103 slices add encrypted token storage
 * entirely inside the adapter implementation — this interface must not change to accommodate that.
 *
 * <p>{@code userId} is a real account id (FOR-145b-2, migration V28) — {@code
 * integration_connection}'s composite primary key was rebuilt from the legacy {@code owner_id
 * VARCHAR} column to {@code user_id UUID}, FK-referencing {@code users(id)}.
 */
public interface IntegrationRepository {

  /**
   * All connections stored for {@code userId}, in a stable order. May be a strict subset of {@link
   * IntegrationProvider#values()} — providers never connected have no row (see {@code
   * IntegrationService#status}, which fills the gaps with defaults).
   */
  List<IntegrationConnection> findAllByOwner(UUID userId);

  /**
   * Finds {@code userId}'s connection for {@code provider}; empty if none was ever stored or it
   * belongs to another owner.
   */
  Optional<IntegrationConnection> findByOwnerAndProvider(UUID userId, IntegrationProvider provider);

  /**
   * Inserts or updates (upserts) the connection for {@code userId}/{@code connection.provider()}.
   * Callers always pass the fully-merged connection; this port never partially patches a row.
   *
   * @return the persisted connection
   */
  IntegrationConnection save(UUID userId, IntegrationConnection connection);
}
