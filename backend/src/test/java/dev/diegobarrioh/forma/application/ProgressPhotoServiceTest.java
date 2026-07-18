package dev.diegobarrioh.forma.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Application use case tests for {@link ProgressPhotoService} (FOR-140, progress-photos slice of
 * FOR-104): owner-scoped upload/list/retrieve/delete, using hand-rolled in-memory fakes (no Spring,
 * no Mockito), matching {@code GoalServiceTest}/{@code UserProfileServiceTest} (ADR-007).
 *
 * <p>Privacy is the primary property under test here (spec FOR-140): a photo owned by another owner
 * is denied with {@link ForbiddenException} (403), never silently 404'd, mirroring the distinction
 * {@code GoalRepository} does not need to make. {@link ProgressPhotoMetadata} itself has no URL
 * field, so there is structurally no way for this service to leak a public/durable link.
 */
class ProgressPhotoServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-07-18T10:00:00Z");
  private static final String OTHER_OWNER = "someone-else";

  private final InMemoryProgressPhotoRepository repository = new InMemoryProgressPhotoRepository();
  private final InMemoryProgressPhotoStore store = new InMemoryProgressPhotoStore();
  private final ProgressPhotoService service =
      new ProgressPhotoService(repository, store, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));

  @Test
  void uploadStoresBinaryViaTheStoreWritesMetadataAndReturnsAPrivateReferenceId() {
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes());

    ProgressPhotoMetadata created = service.upload(file);

    assertThat(created.id()).isNotBlank();
    assertThat(created.contentType()).isEqualTo("image/jpeg");
    assertThat(created.sizeBytes()).isEqualTo(file.getSize());
    assertThat(created.createdAt()).isEqualTo(FIXED_NOW);
    assertThat(store.blobs).containsKey(created.storageRef());
  }

  @Test
  void ownerRetrievalReturnsTheExactBytesRoundTrippedThroughTheStore() {
    byte[] bytes = "fake-png-bytes".getBytes();
    MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", bytes);
    ProgressPhotoMetadata created = service.upload(file);

    ProgressPhotoContent content = service.retrieve(created.id());

    assertThat(content.contentType()).isEqualTo("image/png");
    assertThat(content.content()).isEqualTo(bytes);
  }

  @Test
  void listIsEmptyWhenNoPhotosExistYet() {
    assertThat(service.list()).isEmpty();
  }

  @Test
  void listReturnsOwnerScopedMetadataOnly() {
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", "bytes".getBytes());
    ProgressPhotoMetadata created = service.upload(file);

    List<ProgressPhotoMetadata> listed = service.list();

    assertThat(listed).extracting(ProgressPhotoMetadata::id).containsExactly(created.id());
  }

  @Test
  void retrievalOfAnUnknownIdThrowsNotFound() {
    assertThatThrownBy(() -> service.retrieve("unknown-id")).isInstanceOf(NotFoundException.class);
  }

  @Test
  void retrievalOfAnotherOwnersPhotoIsDeniedWithForbidden() {
    repository.seed(
        new ProgressPhotoMetadata(
            "other-owner-photo", OTHER_OWNER, "image/jpeg", 10L, FIXED_NOW, "other-owner-photo"));

    assertThatThrownBy(() -> service.retrieve("other-owner-photo"))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void deleteRemovesMetadataAndBinaryThenRetrieveIsNotFound() {
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", "bytes".getBytes());
    ProgressPhotoMetadata created = service.upload(file);

    service.delete(created.id());

    assertThatThrownBy(() -> service.retrieve(created.id())).isInstanceOf(NotFoundException.class);
    assertThat(service.list()).isEmpty();
    assertThat(store.blobs).doesNotContainKey(created.storageRef());
  }

  @Test
  void deleteOfAnUnknownIdThrowsNotFound() {
    assertThatThrownBy(() -> service.delete("unknown-id")).isInstanceOf(NotFoundException.class);
  }

  @Test
  void deleteOfAnotherOwnersPhotoIsDeniedWithForbiddenAndDoesNotDeleteIt() {
    repository.seed(
        new ProgressPhotoMetadata(
            "other-owner-photo", OTHER_OWNER, "image/jpeg", 10L, FIXED_NOW, "other-owner-photo"));
    store.blobs.put("other-owner-photo", "still-there".getBytes());

    assertThatThrownBy(() -> service.delete("other-owner-photo"))
        .isInstanceOf(ForbiddenException.class);
    assertThat(repository.findById("other-owner-photo")).isPresent();
    assertThat(store.blobs).containsKey("other-owner-photo");
  }

  @Test
  void uploadRejectsANonImageContentTypeWithValidationError() {
    MockMultipartFile file =
        new MockMultipartFile("file", "doc.pdf", "application/pdf", "not-an-image".getBytes());

    assertThatThrownBy(() -> service.upload(file)).isInstanceOf(ValidationException.class);
  }

  @Test
  void uploadRejectsAnOversizedFileWithValidationError() {
    byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
    MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", tooBig);

    assertThatThrownBy(() -> service.upload(file)).isInstanceOf(ValidationException.class);
  }

  @Test
  void uploadRejectsAnEmptyFileWithValidationError() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

    assertThatThrownBy(() -> service.upload(file)).isInstanceOf(ValidationException.class);
  }

  /** In-memory fake, matching {@code RecordingGoalRepository} (FOR-125). */
  private static class InMemoryProgressPhotoRepository implements ProgressPhotoRepository {
    final Map<String, ProgressPhotoMetadata> rows = new LinkedHashMap<>();

    void seed(ProgressPhotoMetadata metadata) {
      rows.put(metadata.id(), metadata);
    }

    @Override
    public ProgressPhotoMetadata create(
        String id,
        String ownerId,
        String contentType,
        long sizeBytes,
        String storageRef,
        Instant createdAt) {
      ProgressPhotoMetadata metadata =
          new ProgressPhotoMetadata(id, ownerId, contentType, sizeBytes, createdAt, storageRef);
      rows.put(id, metadata);
      return metadata;
    }

    @Override
    public List<ProgressPhotoMetadata> findAllByOwner(String ownerId) {
      return rows.values().stream().filter(m -> m.ownerId().equals(ownerId)).toList();
    }

    @Override
    public Optional<ProgressPhotoMetadata> findById(String id) {
      return Optional.ofNullable(rows.get(id));
    }

    @Override
    public void deleteById(String id) {
      rows.remove(id);
    }
  }

  /** In-memory fake for {@link ProgressPhotoStore}. */
  private static class InMemoryProgressPhotoStore implements ProgressPhotoStore {
    final Map<String, byte[]> blobs = new LinkedHashMap<>();

    @Override
    public void save(String storageRef, byte[] content) {
      blobs.put(storageRef, content);
    }

    @Override
    public Optional<byte[]> load(String storageRef) {
      return Optional.ofNullable(blobs.get(storageRef));
    }

    @Override
    public void delete(String storageRef) {
      blobs.remove(storageRef);
    }
  }
}
