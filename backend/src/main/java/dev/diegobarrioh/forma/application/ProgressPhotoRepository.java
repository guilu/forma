package dev.diegobarrioh.forma.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port for persisting and reading progress-photo metadata (FOR-140). Owned by the
 * application/domain side; adapters implement it (ADR-001). The binary itself is never handled here
 * — see {@link ProgressPhotoStore}.
 */
public interface ProgressPhotoRepository {

  /** Persists a new photo's metadata row and returns it. */
  ProgressPhotoMetadata create(
      String id,
      String ownerId,
      String contentType,
      long sizeBytes,
      String storageRef,
      Instant createdAt);

  /** All photos belonging to {@code ownerId}, metadata only. Empty when none exist yet. */
  List<ProgressPhotoMetadata> findAllByOwner(String ownerId);

  /**
   * Finds a photo's metadata by id, regardless of owner. Deliberately NOT owner-scoped, unlike
   * {@link GoalRepository#findById} — the service needs to know both whether the id exists and who
   * owns it, so it can distinguish an unknown id (404) from another owner's photo (403, spec
   * FOR-140 Edge Cases: "Non-owner access to any photo -> denied (403)... Unknown photo id ->
   * 404"), a distinction {@code GoalRepository} does not need to make. Callers must verify {@link
   * ProgressPhotoMetadata#ownerId()} themselves before returning any content to the caller.
   */
  Optional<ProgressPhotoMetadata> findById(String id);

  /**
   * Deletes the metadata row for {@code id}. No-op if it doesn't exist. Ownership must already be
   * verified by the caller (service) before invoking this — this method does not re-check it.
   */
  void deleteById(String id);
}
