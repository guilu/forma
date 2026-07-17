package dev.diegobarrioh.forma.adapter.storage;

import dev.diegobarrioh.forma.application.ProgressPhotoStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Private, filesystem-backed adapter for {@link ProgressPhotoStore} (FOR-140 Storage decision,
 * Option A: "private object store... filesystem-backed private dir now, S3/MinIO-compatible
 * later"). This is the first binary-storage adapter in the codebase.
 *
 * <p><b>Privacy:</b> the configured directory ({@code forma.progress.photos.storage-dir}) is a
 * plain filesystem path, never registered as a Spring static resource location or served by any
 * controller directly — the only way to read a photo's bytes is through {@link
 * dev.diegobarrioh.forma.application.ProgressPhotoService#retrieve}, which enforces the owner check
 * before ever calling {@link #load}. There is no public/static URL anywhere in this class (spec
 * FOR-140).
 *
 * <p><b>No content in logs:</b> this class never logs — not the byte content, not a derived digest,
 * not the full resolved file path (spec FOR-140 NFR: "no full path in any log line").
 *
 * <p>{@code storageRef} is always a server-generated UUID string ({@link
 * dev.diegobarrioh.forma.application.ProgressPhotoService#upload}), never caller-supplied, but
 * {@link #resolve} still rejects a ref that would resolve outside the storage directory as a
 * defensive boundary against path traversal, matching the "reject unsafe input" spirit of {@link
 * dev.diegobarrioh.forma.adapter.logging.CorrelationIdFilter}'s correlation-id sanitization
 * (FOR-91).
 */
@Component
public class FilesystemProgressPhotoStore implements ProgressPhotoStore {

  private final Path storageDir;

  public FilesystemProgressPhotoStore(
      @Value("${forma.progress.photos.storage-dir:${user.home}/.forma/progress-photos}")
          String storageDir) {
    this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
  }

  @Override
  public void save(String storageRef, byte[] content) {
    Path file = resolve(storageRef);
    try {
      Files.createDirectories(file.getParent());
      Files.write(file, content);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to persist progress photo binary", e);
    }
  }

  @Override
  public Optional<byte[]> load(String storageRef) {
    Path file = resolveOrNull(storageRef);
    if (file == null || !Files.isRegularFile(file)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Files.readAllBytes(file));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read progress photo binary", e);
    }
  }

  @Override
  public void delete(String storageRef) {
    Path file = resolveOrNull(storageRef);
    if (file == null) {
      return;
    }
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to delete progress photo binary", e);
    }
  }

  /** Resolves {@code storageRef} to a file path, rejecting anything outside {@link #storageDir}. */
  private Path resolve(String storageRef) {
    Path resolved = storageDir.resolve(storageRef + ".bin").normalize();
    if (!resolved.startsWith(storageDir)) {
      throw new IllegalArgumentException("Invalid storage reference");
    }
    return resolved;
  }

  /** Like {@link #resolve}, but returns {@code null} instead of throwing for read-side callers. */
  private Path resolveOrNull(String storageRef) {
    Path resolved = storageDir.resolve(storageRef + ".bin").normalize();
    return resolved.startsWith(storageDir) ? resolved : null;
  }
}
