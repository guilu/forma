package dev.diegobarrioh.forma.delivery.progress;

import dev.diegobarrioh.forma.application.ProgressPhotoContent;
import dev.diegobarrioh.forma.application.ProgressPhotoService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Progress-photos REST endpoints (FOR-140, progress-photos slice of FOR-104) under {@link
 * ApiPaths#V1}{@code /progress/photos}: upload (multipart), list metadata, owner-scoped binary
 * retrieval and delete.
 *
 * <p>Thin controller (ADR-001, ADR-005): all validation, storage, and the owner-scoped
 * access-control decision live in {@link ProgressPhotoService}; this class only maps the request to
 * the service call and the result to the response shape. It never accepts or returns a
 * public/static URL — {@link #retrieve} streams the bytes directly, and {@link
 * ProgressPhotoResponse}/{@link ProgressPhotoListResponse} carry no URL field (spec FOR-140
 * api.md).
 *
 * <p>Single-user MVP (ADR-002): mirrors {@code GoalController}'s documented limitation — no
 * account/owner path segment or auth header is accepted yet; the owner boundary is nonetheless
 * enforced server-side in {@link ProgressPhotoService} (403 on another owner's photo), never
 * bypassed because there is currently only one real account.
 *
 * <p><b>No content in logs:</b> this controller never logs a request/response body. Any unexpected
 * exception is logged only by {@link dev.diegobarrioh.forma.delivery.error.GlobalExceptionHandler},
 * which logs the exception itself, never the multipart file content.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/progress/photos")
public class ProgressPhotoController {

  private final ProgressPhotoService service;

  public ProgressPhotoController(ProgressPhotoService service) {
    this.service = service;
  }

  /**
   * Uploads a progress photo ({@code multipart/form-data}, part name {@code file}). Returns the
   * private reference metadata — never a public URL. 400 {@code VALIDATION_ERROR} on a disallowed
   * content type, oversized file, or empty upload (enforced by {@link ProgressPhotoService}).
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ProgressPhotoResponse upload(@RequestParam("file") MultipartFile file) {
    return ProgressPhotoResponse.from(service.upload(file));
  }

  /** Lists the owner's photos, metadata only. Empty array, never 404, when none exist yet. */
  @GetMapping
  public ProgressPhotoListResponse list() {
    return ProgressPhotoListResponse.from(service.list());
  }

  /**
   * Owner-scoped, access-controlled binary retrieval. 403 if {@code id} belongs to another owner,
   * 404 if unknown — both mapped by {@link
   * dev.diegobarrioh.forma.delivery.error.GlobalExceptionHandler} from the exceptions {@link
   * ProgressPhotoService#retrieve} throws.
   */
  @GetMapping("/{id}")
  public ResponseEntity<byte[]> retrieve(@PathVariable String id) {
    ProgressPhotoContent content = service.retrieve(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.contentType()))
        .body(content.content());
  }

  /** Owner-scoped delete of metadata + binary. 403/404 mirror {@link #retrieve}'s mapping. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String id) {
    service.delete(id);
  }
}
