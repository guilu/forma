package dev.diegobarrioh.forma.application;

import java.util.Optional;

/**
 * Port for the private binary storage of progress photos (FOR-140), kept out of PostgreSQL
 * (ADR-003-consistent: metadata in the DB via {@link ProgressPhotoRepository}, binary behind this
 * port). Owned by the application/domain side; adapters implement it (ADR-001). This is the first
 * binary-storage port in the codebase (spec FOR-140: "first binary-storage concern... first
 * multipart upload").
 *
 * <p>{@code storageRef} is an opaque key, never a public/static URL — see {@link
 * ProgressPhotoMetadata#storageRef()}.
 */
public interface ProgressPhotoStore {

  /** Persists {@code content} under {@code storageRef}, overwriting any existing value. */
  void save(String storageRef, byte[] content);

  /** The binary content stored under {@code storageRef}; empty if nothing is stored there. */
  Optional<byte[]> load(String storageRef);

  /** Removes the binary stored under {@code storageRef}. No-op if nothing is stored there. */
  void delete(String storageRef);
}
