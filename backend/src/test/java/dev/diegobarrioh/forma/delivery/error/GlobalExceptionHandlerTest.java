package dev.diegobarrioh.forma.delivery.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.ForbiddenException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Verifies the error baseline (FOR-88): unexpected errors map to a safe generic response with no
 * internal leakage, and validation failures map to the standard shape with field details. Uses a
 * standalone MockMvc with a tiny in-test controller, so no product endpoints are added to the
 * application.
 */
class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void unexpectedErrorMapsToSafeGenericResponse() throws Exception {
    mockMvc
        .perform(get("/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
        // The internal exception message must never reach the client.
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret internal detail"))));
  }

  @Test
  void includesCorrelationIdWhenHeaderPresent() throws Exception {
    mockMvc
        .perform(get("/boom").header(GlobalExceptionHandler.CORRELATION_HEADER, "abc-123"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.correlationId").value("abc-123"));
  }

  @Test
  void validationErrorMapsToStandardShapeWithFieldDetails() throws Exception {
    mockMvc
        .perform(post("/echo").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("name"));
  }

  /**
   * {@link ForbiddenException} maps to 403 {@code FORBIDDEN} (FOR-140, first real use of the
   * placeholder {@link ApiErrorCode#FORBIDDEN} reserved by FOR-88) with the caller-safe message,
   * mirroring {@link #validationErrorMapsToStandardShapeWithFieldDetails}'s pattern for {@code
   * NotFoundException}/{@code ValidationException}.
   */
  @Test
  void forbiddenExceptionMapsToForbiddenResponse() throws Exception {
    mockMvc
        .perform(get("/forbidden"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("No tienes acceso a este recurso"));
  }

  /**
   * A multipart upload exceeding the configured {@code spring.servlet.multipart.max-file-size}
   * (FOR-140: "Oversized... upload -> 400 VALIDATION_ERROR") maps to 400, not the framework's
   * default 500/413 — this is the servlet-level counterpart to {@code ProgressPhotoService}'s own
   * application-level size check, since Spring's multipart resolver can reject a request before it
   * ever reaches the service.
   */
  @Test
  void maxUploadSizeExceededMapsToValidationError() throws Exception {
    mockMvc
        .perform(get("/too-big"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  /** In-test controller used only to trigger the handled exceptions. */
  @RestController
  static class TestController {

    @GetMapping("/boom")
    String boom() {
      throw new RuntimeException("secret internal detail");
    }

    @GetMapping("/forbidden")
    String forbidden() {
      throw new ForbiddenException("No tienes acceso a este recurso");
    }

    @GetMapping("/too-big")
    String tooBig() {
      throw new MaxUploadSizeExceededException(5L * 1024 * 1024);
    }

    @PostMapping("/echo")
    String echo(@Valid @RequestBody Payload payload) {
      return payload.name();
    }

    record Payload(@NotBlank String name) {}
  }
}
