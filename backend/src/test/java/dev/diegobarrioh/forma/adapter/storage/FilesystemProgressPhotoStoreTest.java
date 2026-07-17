package dev.diegobarrioh.forma.adapter.storage;

import static org.assertj.core.api.Assertions.assertThat;

import dev.diegobarrioh.forma.application.ProgressPhotoStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit test for {@link FilesystemProgressPhotoStore} (FOR-140): a private, non-web-served
 * filesystem directory behind the {@link ProgressPhotoStore} port (spec FOR-140 Storage decision,
 * Option A — "filesystem-backed private dir now"). No Spring context needed; the store is a plain
 * class over {@code java.nio.file}.
 */
class FilesystemProgressPhotoStoreTest {

  @TempDir Path tempDir;

  private ProgressPhotoStore store;

  @BeforeEach
  void setUp() {
    // @TempDir field injection happens after instance construction, so the store must be built
    // here rather than in a field initializer (which would see a still-null tempDir).
    store = new FilesystemProgressPhotoStore(tempDir.toString());
  }

  @Test
  void savedContentRoundTripsThroughLoad() {
    byte[] content = "fake-jpeg-bytes".getBytes();

    store.save("photo-1", content);
    Optional<byte[]> loaded = store.load("photo-1");

    assertThat(loaded).isPresent();
    assertThat(loaded.orElseThrow()).isEqualTo(content);
  }

  @Test
  void loadOfAnUnknownStorageRefIsEmpty() {
    assertThat(store.load("does-not-exist")).isEmpty();
  }

  @Test
  void deleteRemovesTheStoredContentAndIsANoOpWhenNothingIsStored() {
    store.save("photo-1", "bytes".getBytes());

    store.delete("photo-1");
    store.delete("does-not-exist");

    assertThat(store.load("photo-1")).isEmpty();
  }

  @Test
  void savingUnderTheSameStorageRefTwiceOverwritesThePreviousContent() {
    store.save("photo-1", "first".getBytes());
    store.save("photo-1", "second".getBytes());

    assertThat(store.load("photo-1").orElseThrow()).isEqualTo("second".getBytes());
  }

  @Test
  void createsTheStorageDirectoryIfItDoesNotExistYet() throws IOException {
    Path nested = tempDir.resolve("nested/private-photos");
    ProgressPhotoStore nestedStore = new FilesystemProgressPhotoStore(nested.toString());

    nestedStore.save("photo-1", "bytes".getBytes());

    assertThat(Files.isDirectory(nested)).isTrue();
  }

  @Test
  void rejectsAStorageRefThatWouldEscapeTheStorageDirectory() {
    assertThat(store.load("../escape")).isEmpty();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> store.save("../escape", "bytes".getBytes()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
