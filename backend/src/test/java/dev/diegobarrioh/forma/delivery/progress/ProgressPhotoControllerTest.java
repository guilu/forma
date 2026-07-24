package dev.diegobarrioh.forma.delivery.progress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.ProgressPhotoContent;
import dev.diegobarrioh.forma.application.ProgressPhotoMetadata;
import dev.diegobarrioh.forma.application.ProgressPhotoService;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.delivery.error.GlobalExceptionHandler;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Web-slice tests for {@link ProgressPhotoController} (FOR-140): routing, request handling and
 * response shape per {@code specs/FOR-140/api.md} and {@code tests.md}. {@link
 * ProgressPhotoService} is mocked, mirroring {@code GoalControllerTest}. The {@link
 * GlobalExceptionHandler} is loaded so the 403/404/400 mappings this story depends on are exercised
 * end-to-end at the web-slice level.
 *
 * <p>Privacy assertion: no response here ever contains a {@code url}/{@code link} field — only
 * {@code id}, {@code contentType}, {@code sizeBytes}, {@code createdAt} (spec FOR-140 api.md: "No
 * field in any response is a public/static/durable URL").
 */
@WebMvcTest(ProgressPhotoController.class)
@Import(WebMvcAuthTestConfig.class)
class ProgressPhotoControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private ProgressPhotoService service;

  @Test
  void uploadReturnsCreatedWithPrivateReferenceMetadataOnly() throws Exception {
    Instant createdAt = Instant.parse("2026-07-18T10:00:00Z");
    when(service.upload(any()))
        .thenReturn(
            new ProgressPhotoMetadata(
                "photo-1", UUID.randomUUID(), "image/jpeg", 4L, createdAt, "photo-1"));
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", "abcd".getBytes());

    mockMvc
        .perform(multipart("/api/v1/progress/photos").file(file))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("photo-1"))
        .andExpect(jsonPath("$.contentType").value("image/jpeg"))
        .andExpect(jsonPath("$.sizeBytes").value(4))
        .andExpect(jsonPath("$.url").doesNotExist());
  }

  @Test
  void uploadOfAnInvalidFileMapsToValidationError() throws Exception {
    when(service.upload(any())).thenThrow(new ValidationException("Tipo no soportado"));
    MockMultipartFile file =
        new MockMultipartFile("file", "doc.pdf", "application/pdf", "x".getBytes());

    mockMvc
        .perform(multipart("/api/v1/progress/photos").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void listBeforeAnyUploadReturnsAnEmptyArrayNeverA404() throws Exception {
    when(service.list()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/progress/photos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photos").isArray())
        .andExpect(jsonPath("$.photos").isEmpty());
  }

  @Test
  void listReturnsMetadataOnlyNoBinaryNoUrl() throws Exception {
    when(service.list())
        .thenReturn(
            List.of(
                new ProgressPhotoMetadata(
                    "photo-1",
                    UUID.randomUUID(),
                    "image/png",
                    10L,
                    Instant.parse("2026-07-18T10:00:00Z"),
                    "photo-1")));

    mockMvc
        .perform(get("/api/v1/progress/photos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.photos[0].id").value("photo-1"))
        .andExpect(jsonPath("$.photos[0].contentType").value("image/png"))
        .andExpect(jsonPath("$.photos[0].sizeBytes").value(10))
        .andExpect(jsonPath("$.photos[0].url").doesNotExist());
  }

  @Test
  void retrieveReturnsTheStoredBinaryWithItsContentTypeOnlyToTheOwner() throws Exception {
    when(service.retrieve("photo-1"))
        .thenReturn(new ProgressPhotoContent("image/jpeg", "fake-bytes".getBytes()));

    mockMvc
        .perform(get("/api/v1/progress/photos/photo-1"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/jpeg"))
        .andExpect(MockMvcResultMatchers.content().bytes("fake-bytes".getBytes()));
  }

  @Test
  void retrieveOfAnUnknownIdReturnsNotFound() throws Exception {
    when(service.retrieve("unknown")).thenThrow(new NotFoundException("No existe"));

    mockMvc
        .perform(get("/api/v1/progress/photos/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void retrieveOfAnotherOwnersPhotoReturnsNotFound() throws Exception {
    // FOR-145b-1 (ADR-012): cross-owner access is now indistinguishable from unknown-id --
    // ProgressPhotoService maps both to NotFoundException (no existence leak).
    when(service.retrieve("other-owner-photo")).thenThrow(new NotFoundException("No existe"));

    mockMvc
        .perform(get("/api/v1/progress/photos/other-owner-photo"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void deleteReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/v1/progress/photos/photo-1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteOfAnUnknownIdReturnsNotFound() throws Exception {
    org.mockito.Mockito.doThrow(new NotFoundException("No existe")).when(service).delete("unknown");

    mockMvc
        .perform(delete("/api/v1/progress/photos/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void deleteOfAnotherOwnersPhotoReturnsNotFound() throws Exception {
    // FOR-145b-1 (ADR-012): cross-owner access is now indistinguishable from unknown-id.
    org.mockito.Mockito.doThrow(new NotFoundException("No existe"))
        .when(service)
        .delete("other-owner-photo");

    mockMvc
        .perform(delete("/api/v1/progress/photos/other-owner-photo"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
