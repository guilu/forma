package dev.diegobarrioh.forma.application;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Application use cases for progress photos (FOR-140, progress-photos slice of FOR-104): upload,
 * list, owner-scoped retrieve and delete. Privacy is the primary property this class enforces (spec
 * FOR-140):
 *
 * <ul>
 *   <li>The binary is written/read only through {@link ProgressPhotoStore} — never held in the DB
 *       (ADR-003-consistent; metadata lives in {@link ProgressPhotoRepository}).
 *   <li>Retrieval and deletion check {@link ProgressPhotoMetadata#ownerId()} explicitly and throw
 *       {@link ForbiddenException} (403) on a mismatch, even though the MVP has only one real
 *       account (ADR-002, AGENTS.md: do not bypass authorization "because the MVP is currently
 *       single-user"). An unknown id is a distinct {@link NotFoundException} (404) — the two must
 *       never be conflated (spec FOR-140 Edge Cases).
 *   <li>Nothing in this class ever logs — a photo's bytes, filename-derived content or storage path
 *       must never reach a log line (spec FOR-140 NFR). Exception messages here are always fixed,
 *       generic strings, never the file's content.
 * </ul>
 *
 * <p>Single-user MVP (ADR-002): every use case operates on the one {@link #OWNER_ID} row, mirroring
 * {@link GoalService#OWNER_ID}. Duplicated here rather than a shared abstraction, for the same
 * documented reason as {@link GoalService}.
 */
@Service
public class ProgressPhotoService {

  /** Fixed single-user owner id for the MVP (ADR-002), mirroring {@link GoalService#OWNER_ID}. */
  public static final String OWNER_ID = "default-user";

  /** Content-type allow-list for uploads (spec FOR-140 Open Questions, resolved for this MVP). */
  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

  /** Max upload size for this MVP (spec FOR-140 Open Questions, resolved for this MVP): 5 MB. */
  private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

  private final ProgressPhotoRepository repository;
  private final ProgressPhotoStore store;
  private final Clock clock;

  public ProgressPhotoService(
      ProgressPhotoRepository repository, ProgressPhotoStore store, Clock clock) {
    this.repository = repository;
    this.store = store;
    this.clock = clock;
  }

  /**
   * Validates the upload (content-type allow-list, max size, non-empty), stores the binary via the
   * port, writes the metadata row, and returns it. The id doubles as the store's opaque {@code
   * storageRef} — no separate token is needed since neither is ever surfaced as a resolvable URL.
   *
   * @throws ValidationException if the content type is not allowed, the file is empty, or it
   *     exceeds {@link #MAX_SIZE_BYTES}
   */
  public ProgressPhotoMetadata upload(MultipartFile file) {
    validate(file);
    String id = UUID.randomUUID().toString();
    byte[] content = readBytes(file);
    store.save(id, content);
    return repository.create(
        id, OWNER_ID, file.getContentType(), content.length, id, clock.instant());
  }

  /** Lists the owner's photos, metadata only. Empty when none exist yet, never a not-found. */
  public List<ProgressPhotoMetadata> list() {
    return repository.findAllByOwner(OWNER_ID);
  }

  /**
   * Returns the exact bytes and content type for one of the owner's photos.
   *
   * @throws NotFoundException if no photo with {@code id} exists, or its binary is unexpectedly
   *     missing from the store
   * @throws ForbiddenException if {@code id} exists but belongs to another owner
   */
  public ProgressPhotoContent retrieve(String id) {
    ProgressPhotoMetadata metadata = findOwnedOrThrow(id);
    byte[] content =
        store
            .load(metadata.storageRef())
            .orElseThrow(() -> new NotFoundException("No existe la foto de progreso: " + id));
    return new ProgressPhotoContent(metadata.contentType(), content);
  }

  /**
   * Deletes a photo's metadata and binary.
   *
   * @throws NotFoundException if no photo with {@code id} exists
   * @throws ForbiddenException if {@code id} exists but belongs to another owner
   */
  public void delete(String id) {
    ProgressPhotoMetadata metadata = findOwnedOrThrow(id);
    repository.deleteById(id);
    store.delete(metadata.storageRef());
  }

  private ProgressPhotoMetadata findOwnedOrThrow(String id) {
    ProgressPhotoMetadata metadata =
        repository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("No existe la foto de progreso: " + id));
    if (!metadata.ownerId().equals(OWNER_ID)) {
      throw new ForbiddenException("No tienes acceso a esta foto de progreso");
    }
    return metadata;
  }

  private static void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ValidationException("El archivo de la foto es obligatorio");
    }
    String contentType = file.getContentType();
    if (contentType == null
        || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
      throw new ValidationException(
          "Tipo de contenido no soportado: solo se admiten imágenes JPEG o PNG");
    }
    if (file.getSize() > MAX_SIZE_BYTES) {
      throw new ValidationException("El archivo supera el tamaño máximo permitido (5 MB)");
    }
  }

  private static byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new ValidationException("No se pudo leer el archivo subido");
    }
  }
}
