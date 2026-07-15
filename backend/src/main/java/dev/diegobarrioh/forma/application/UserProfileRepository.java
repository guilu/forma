package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.UserProfile;
import java.util.Optional;

/**
 * Port for persisting and reading the single-user {@link UserProfile} aggregate (FOR-107). Owned by
 * the application/domain side; adapters implement it (ADR-001). The domain type stays
 * framework-free — this interface speaks only in domain objects, never in rows or SQL types.
 */
public interface UserProfileRepository {

  /**
   * Finds the stored profile for the given owner.
   *
   * @param ownerId the owning account's identifier
   * @return the stored profile, or empty when no row has been saved yet (spec FOR-107 Edge Cases:
   *     first call before any profile row exists is not an error)
   */
  Optional<UserProfile> find(String ownerId);

  /**
   * Inserts or updates the whole profile row for {@code profile.ownerId()} (upsert). Callers always
   * pass the fully-merged aggregate — this port never partially patches a row (FOR-107 Application
   * Tests: "a single changed field" is merged by the caller before {@code save}, never here).
   *
   * @param profile the profile to store; must not be {@code null}
   */
  void save(UserProfile profile);
}
