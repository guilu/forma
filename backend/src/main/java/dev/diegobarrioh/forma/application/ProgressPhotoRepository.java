package dev.diegobarrioh.forma.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and reading progress-photo metadata (FOR-140). Owned by the
 * application/domain side; adapters implement it (ADR-001). The binary itself is never handled here
 * — see {@link ProgressPhotoStore}.
 *
 * <p>{@code userId} is a real account id (FOR-145b-1, migration V27) — {@code
 * progress_photo.user_id UUID}, FK-referencing {@code users(id)}.
 */
public interface ProgressPhotoRepository {

  /** Persists a new photo's metadata row and returns it. */
  ProgressPhotoMetadata create(
      String id,
      UUID userId,
      String contentType,
      long sizeBytes,
      String storageRef,
      Instant createdAt);

  /** All photos belonging to {@code userId}, metadata only. Empty when none exist yet. */
  List<ProgressPhotoMetadata> findAllByOwner(UUID userId);

  /**
   * Finds one of {@code userId}'s photos by id; empty if it doesn't exist OR belongs to another
   * user (FOR-145b-1: cross-owner access is now indistinguishable from unknown-id at the SQL level,
   * so the service maps both to 404 — no existence leak, spec FOR-145 "Cross-user isolation — 11
   * existing domains"). This SUPERSEDES the pre-145b behavior where {@code findById} was
   * deliberately unscoped so the service could distinguish 403 (another owner) from 404 (unknown);
   * that distinction is intentionally removed by this slice's design (ADR-012 §3/§7 145b review
   * focus: "ProgressPhoto's app-level 403 ownership check becomes a real per-user query filter,
   * cross-user -> 404").
   */
  Optional<ProgressPhotoMetadata> findById(UUID userId, String id);

  /**
   * Deletes the metadata row for {@code id}. No-op if it doesn't exist. Ownership must already be
   * verified by the caller (service) before invoking this — this method does not re-check it.
   */
  void deleteById(String id);
}
