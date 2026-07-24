package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.UserProfile;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and reading the single-user {@link UserProfile} aggregate (FOR-107). Owned by
 * the application/domain side; adapters implement it (ADR-001). The domain type stays
 * framework-free — this interface speaks only in domain objects, never in rows or SQL types.
 *
 * <p>{@code userId} is a real account id (FOR-145b-2, migration V28) — {@code user_profile}'s
 * primary key was rebuilt from the legacy {@code owner_id VARCHAR} column to {@code user_id UUID},
 * FK-referencing {@code users(id)}.
 */
public interface UserProfileRepository {

  /**
   * Finds the stored profile for the given owner.
   *
   * @param userId the owning account's identifier
   * @return the stored profile, or empty when no row has been saved yet (spec FOR-107 Edge Cases:
   *     first call before any profile row exists is not an error)
   */
  Optional<UserProfile> find(UUID userId);

  /**
   * Inserts or updates the whole profile row for {@code profile.ownerId()} (upsert). Callers always
   * pass the fully-merged aggregate — this port never partially patches a row (FOR-107 Application
   * Tests: "a single changed field" is merged by the caller before {@code save}, never here).
   *
   * @param profile the profile to store; must not be {@code null}
   */
  void save(UserProfile profile);
}
