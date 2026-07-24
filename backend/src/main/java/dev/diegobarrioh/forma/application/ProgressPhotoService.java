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
 *   <li>FOR-145b-1 (ADR-012): retrieval and deletion resolve the metadata via {@link
 *       ProgressPhotoRepository#findById(java.util.UUID, String)}, which is now SQL-scoped by the
 *       caller's {@code user_id}. A cross-owner id and an unknown id are therefore
 *       indistinguishable and both surface as {@link NotFoundException} (404) — this deliberately
 *       REPLACES the pre-145b behavior of throwing {@link ForbiddenException} (403) on a
 *       cross-owner mismatch (spec FOR-145 "Cross-user isolation — 11 existing domains": "MUST
 *       return 404, never 403, to avoid existence leakage").
 *   <li>Nothing in this class ever logs — a photo's bytes, filename-derived content or storage path
 *       must never reach a log line (spec FOR-140 NFR). Exception messages here are always fixed,
 *       generic strings, never the file's content.
 * </ul>
 *
 * <p>Real multi-user auth (FOR-145b-1, ADR-012): every use case resolves the caller's account id
 * via {@link CurrentUserProvider} instead of the old fixed {@code OWNER_ID = "default-user"}
 * constant (removed by this slice).
 */
@Service
public class ProgressPhotoService {

  /** Content-type allow-list for uploads (spec FOR-140 Open Questions, resolved for this MVP). */
  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

  /** Max upload size for this MVP (spec FOR-140 Open Questions, resolved for this MVP): 5 MB. */
  private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

  private final ProgressPhotoRepository repository;
  private final ProgressPhotoStore store;
  private final Clock clock;
  private final CurrentUserProvider currentUserProvider;

  public ProgressPhotoService(
      ProgressPhotoRepository repository,
      ProgressPhotoStore store,
      Clock clock,
      CurrentUserProvider currentUserProvider) {
    this.repository = repository;
    this.store = store;
    this.clock = clock;
    this.currentUserProvider = currentUserProvider;
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
        id,
        currentUserProvider.currentUserId(),
        file.getContentType(),
        content.length,
        id,
        clock.instant());
  }

  /** Lists the owner's photos, metadata only. Empty when none exist yet, never a not-found. */
  public List<ProgressPhotoMetadata> list() {
    return repository.findAllByOwner(currentUserProvider.currentUserId());
  }

  /**
   * Returns the exact bytes and content type for one of the owner's photos.
   *
   * @throws NotFoundException if no photo with {@code id} exists, or it belongs to another owner
   *     (FOR-145b-1: the two are now indistinguishable — no existence leak), or its binary is
   *     unexpectedly missing from the store
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
   * @throws NotFoundException if no photo with {@code id} exists, or it belongs to another owner
   *     (FOR-145b-1: the two are now indistinguishable — no existence leak)
   */
  public void delete(String id) {
    ProgressPhotoMetadata metadata = findOwnedOrThrow(id);
    repository.deleteById(id);
    store.delete(metadata.storageRef());
  }

  private ProgressPhotoMetadata findOwnedOrThrow(String id) {
    return repository
        .findById(currentUserProvider.currentUserId(), id)
        .orElseThrow(() -> new NotFoundException("No existe la foto de progreso: " + id));
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
