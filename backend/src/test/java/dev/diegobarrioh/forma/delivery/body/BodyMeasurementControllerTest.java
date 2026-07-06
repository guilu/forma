package dev.diegobarrioh.forma.delivery.body;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.BodyMeasurementService;
import dev.diegobarrioh.forma.domain.BodyMeasurement;
import dev.diegobarrioh.forma.domain.MeasurementSource;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link BodyMeasurementController} (FOR-17). Mocks the application service so
 * the test focuses on the delivery contract: routing, validation → {@code VALIDATION_ERROR}, and
 * the response DTO shape (following PingControllerTest / GlobalExceptionHandlerTest patterns,
 * ADR-007).
 */
@WebMvcTest(BodyMeasurementController.class)
class BodyMeasurementControllerTest {

  private static final String PATH = "/api/v1/body/measurements";

  @Autowired private MockMvc mockMvc;
  @MockBean private BodyMeasurementService service;

  private static BodyMeasurement measurement(Instant measuredAt, double weightKg) {
    return new BodyMeasurement(measuredAt, MeasurementSource.MANUAL, weightKg, 25.0, 24.0, null);
  }

  @Test
  void postWithValidBodyReturns201WithDerivedValues() throws Exception {
    when(service.createManual(any(), eq(80.0), eq(25.0), eq(24.0), any()))
        .thenReturn(measurement(Instant.parse("2026-07-05T08:00:00Z"), 80.0));

    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"measuredAt":"2026-07-05T08:00:00Z","weightKg":80.0,
                     "bodyFatPercentage":25.0,"bmi":24.0,"notes":null}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.source").value("MANUAL"))
        .andExpect(jsonPath("$.weightKg").value(80.0))
        // Derived values come from the domain type (80 * 25 / 100 = 20; 80 - 20 = 60).
        .andExpect(jsonPath("$.fatMassKg").value(20.0))
        .andExpect(jsonPath("$.leanMassKg").value(60.0));
  }

  @Test
  void postIgnoresClientSuppliedSourceAndAlwaysRecordsManual() throws Exception {
    when(service.createManual(any(), anyDouble(), any(), any(), any()))
        .thenReturn(measurement(Instant.parse("2026-07-05T08:00:00Z"), 80.0));

    // Extra/unknown "source" field is ignored (Spring Boot disables fail-on-unknown-properties);
    // the server-set source wins.
    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"measuredAt":"2026-07-05T08:00:00Z","weightKg":80.0,
                     "bodyFatPercentage":25.0,"bmi":24.0,"source":"WITHINGS"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.source").value("MANUAL"));
  }

  @Test
  void postMissingRequiredFieldReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"measuredAt":"2026-07-05T08:00:00Z",
                     "bodyFatPercentage":25.0,"bmi":24.0}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("weightKg"));
  }

  @Test
  void postBodyFatOutOfRangeReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"measuredAt":"2026-07-05T08:00:00Z","weightKg":80.0,
                     "bodyFatPercentage":150.0,"bmi":24.0}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("bodyFatPercentage"));
  }

  @Test
  void getWithNoMeasurementsReturnsEmptyArray() throws Exception {
    when(service.list()).thenReturn(List.of());

    mockMvc
        .perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void getReturnsMeasurementsInServiceOrder() throws Exception {
    // The service (backed by FOR-16) already returns most-recent-first; the controller passes
    // that order through unchanged.
    when(service.list())
        .thenReturn(
            List.of(
                measurement(Instant.parse("2026-07-05T08:00:00Z"), 79.5),
                measurement(Instant.parse("2026-07-01T08:00:00Z"), 80.0)));

    mockMvc
        .perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].measuredAt").value("2026-07-05T08:00:00Z"))
        .andExpect(jsonPath("$[1].measuredAt").value("2026-07-01T08:00:00Z"))
        .andExpect(jsonPath("$[0].source").value("MANUAL"));
  }
}
