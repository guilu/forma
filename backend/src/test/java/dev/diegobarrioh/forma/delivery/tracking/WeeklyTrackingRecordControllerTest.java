package dev.diegobarrioh.forma.delivery.tracking;

import static org.hamcrest.Matchers.closeTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.WeeklyTrackingRecordService;
import dev.diegobarrioh.forma.domain.WeeklyTrackingRecord;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link WeeklyTrackingRecordController} (FOR-155): {@code GET}/{@code POST}
 * {@code /api/v1/tracking/weekly} and {@code GET .../{week}}. Mocks the application service so the
 * test focuses on the delivery contract (routing, validation, empty-state, 404), mirroring {@code
 * BodyMeasurementControllerTest} (ADR-007).
 */
@WebMvcTest(WeeklyTrackingRecordController.class)
class WeeklyTrackingRecordControllerTest {

  private static final String PATH = "/api/v1/tracking/weekly";

  @Autowired private MockMvc mockMvc;
  @MockBean private WeeklyTrackingRecordService service;

  private static WeeklyTrackingRecord week1() {
    return new WeeklyTrackingRecord(
        1, LocalDate.parse("2026-07-06"), 73.6, 14.7, 22.7, 13.0, "6:00", 2300.0, "nota");
  }

  @Test
  void getWithNoRecordsReturnsEmptyArrayNotAnError() throws Exception {
    when(service.list()).thenReturn(List.of());

    mockMvc
        .perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void getReturnsStoredRecordsInServiceOrder() throws Exception {
    when(service.list()).thenReturn(List.of(week1()));

    mockMvc
        .perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].week").value(1))
        .andExpect(jsonPath("$[0].weightKg").value(73.6))
        .andExpect(jsonPath("$[0].fatMassKg").value(closeTo(10.8192, 0.001)))
        .andExpect(jsonPath("$[0].leanMassKg").value(closeTo(62.7808, 0.001)));
  }

  @Test
  void postCreatesRecordAndReturnsIt() throws Exception {
    when(service.save(any())).thenReturn(week1());

    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"week":1,"date":"2026-07-06","weightKg":73.6,"bodyFatPercentage":14.7,
                     "bmi":22.7,"runningKm":13.0,"pace4kmMinPerKm":"6:00",
                     "recommendedKcal":2300,"comment":"nota"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.week").value(1))
        .andExpect(jsonPath("$.comment").value("nota"));
  }

  @Test
  void postWithOnlyWeekAndDateIsValidPartialRecord() throws Exception {
    WeeklyTrackingRecord partial =
        new WeeklyTrackingRecord(
            2, LocalDate.parse("2026-07-13"), null, null, null, null, null, null, null);
    when(service.save(any())).thenReturn(partial);

    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"week":2,"date":"2026-07-13"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.week").value(2))
        .andExpect(jsonPath("$.weightKg").doesNotExist());
  }

  @Test
  void postMissingWeekReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-07-06"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("week"));
  }

  @Test
  void postNegativeWeightReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"week":1,"date":"2026-07-06","weightKg":-5.0}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("weightKg"));
  }

  @Test
  void postMalformedPaceReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"week":1,"date":"2026-07-06","pace4kmMinPerKm":"not-a-pace"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("pace4kmMinPerKm"));
  }

  @Test
  void getByWeekReturnsRecordWhenPresent() throws Exception {
    when(service.getByWeek(1)).thenReturn(week1());

    mockMvc
        .perform(get(PATH + "/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.week").value(1));
  }

  @Test
  void getByWeekReturns404WhenMissing() throws Exception {
    when(service.getByWeek(9))
        .thenThrow(new NotFoundException("No existe registro para la semana: 9"));

    mockMvc
        .perform(get(PATH + "/9"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
