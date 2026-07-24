package dev.diegobarrioh.forma.delivery.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diegobarrioh.forma.application.ProgressPhotoRepository;
import dev.diegobarrioh.forma.application.ProgressPhotoService;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack, real-database, real-filesystem round trip for {@code /api/v1/progress/photos}
 * (FOR-140 tests.md API Tests): real {@link ProgressPhotoService}, real JDBC metadata repository
 * (migration V19), real {@code FilesystemProgressPhotoStore} writing to an isolated
 * {@code @TempDir} (never the real {@code forma.progress.photos.storage-dir} default), like the
 * FOR-132 {@code IntegrationSyncEndToEndTest}.
 *
 * <p>Privacy is the primary property under test (spec FOR-140):
 *
 * <ul>
 *   <li>Cross-owner access is denied with 403, never silently 404 (a photo seeded for another owner
 *       directly via {@link ProgressPhotoRepository}, bypassing the fixed single-user {@code
 *       OWNER_ID}, simulates the boundary the same way {@code JdbcGoalRepositoryTest}'s {@code
 *       OTHER_OWNER} does).
 *   <li>{@link #photoBytesAndStoragePathNeverAppearInAnyLogLine()} is the first-class no-content-
 *       in-logs assertion required by spec FOR-140 tests.md: a Logback {@link ListAppender}
 *       attached to the application's root logger at {@code TRACE} captures every log event across
 *       a real upload+retrieve HTTP round trip, and the test asserts neither the photo's raw bytes
 *       (a unique per-run marker) nor the resolved storage file path ever appears in a formatted
 *       log message.
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(WebMvcAuthTestConfig.class)
class ProgressPhotoEndToEndTest {

  private static final String OTHER_OWNER = "someone-else";

  @TempDir static Path storageDir;

  @DynamicPropertySource
  static void storageDirProperty(DynamicPropertyRegistry registry) {
    registry.add("forma.progress.photos.storage-dir", storageDir::toString);
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ProgressPhotoRepository repository;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM progress_photo");
  }

  @Test
  void fullRoundTripUploadListRetrieveDelete() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", "real-jpeg-bytes".getBytes());

    String uploadJson =
        mockMvc
            .perform(multipart("/api/v1/progress/photos").file(file))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.url").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = objectMapper.readTree(uploadJson).get("id").asText();

    mockMvc
        .perform(get("/api/v1/progress/photos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photos[0].id").value(id));

    mockMvc
        .perform(get("/api/v1/progress/photos/" + id))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .bytes("real-jpeg-bytes".getBytes()));

    mockMvc.perform(delete("/api/v1/progress/photos/" + id)).andExpect(status().isNoContent());

    mockMvc.perform(get("/api/v1/progress/photos/" + id)).andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/progress/photos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photos").isEmpty());
  }

  @Test
  void nonOwnerCannotRetrieveOrDeleteAnotherOwnersPhoto() throws Exception {
    String id = UUID.randomUUID().toString();
    repository.create(
        id, OTHER_OWNER, "image/jpeg", 10L, id, Instant.parse("2026-07-18T10:00:00Z"));

    mockMvc.perform(get("/api/v1/progress/photos/" + id)).andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/v1/progress/photos/" + id)).andExpect(status().isForbidden());

    // Never deleted by the denied attempt.
    assertThat(repository.findById(id)).isPresent();
  }

  @Test
  void photoBytesAndStoragePathNeverAppearInAnyLogLine() throws Exception {
    String marker = "PRIVACY-MARKER-" + UUID.randomUUID();
    byte[] content = ("fake-jpeg-header:" + marker).getBytes();
    MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", content);

    Logger appLogger = (Logger) LoggerFactory.getLogger("dev.diegobarrioh.forma");
    Level originalLevel = appLogger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    appLogger.addAppender(appender);
    appLogger.setLevel(Level.TRACE);

    String id;
    try {
      String uploadJson =
          mockMvc
              .perform(multipart("/api/v1/progress/photos").file(file))
              .andExpect(status().isCreated())
              .andReturn()
              .getResponse()
              .getContentAsString();
      id = objectMapper.readTree(uploadJson).get("id").asText();

      mockMvc.perform(get("/api/v1/progress/photos/" + id)).andExpect(status().isOk());
    } finally {
      appLogger.detachAppender(appender);
      appLogger.setLevel(originalLevel);
    }

    String storagePath = storageDir.toAbsolutePath().toString();
    for (ILoggingEvent event : appender.list) {
      assertThat(event.getFormattedMessage()).doesNotContain(marker).doesNotContain(storagePath);
      if (event.getThrowableProxy() != null) {
        assertThat(event.getThrowableProxy().getMessage())
            .satisfiesAnyOf(
                message -> assertThat(message).isNull(),
                message -> assertThat(message).doesNotContain(marker).doesNotContain(storagePath));
      }
    }
  }

  @AfterEach
  void cleanUp() {
    jdbcTemplate.update("DELETE FROM progress_photo");
  }
}
