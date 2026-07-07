package dev.diegobarrioh.forma.delivery.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.StoredSessionStatus;
import dev.diegobarrioh.forma.application.TrainingSessionStatusService;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService;
import dev.diegobarrioh.forma.domain.SessionStatus;
import java.time.DayOfWeek;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link TrainingController} (FOR-26/FOR-27): the week response shape and the
 * status-update endpoint (happy path, validation, not-found).
 */
@WebMvcTest(TrainingController.class)
class TrainingControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private WeeklyTrainingScheduleService scheduleService;
  @MockBean private TrainingSessionStatusService statusService;

  @Test
  void returnsTheWeekWithSessionIdsAndRestDays() throws Exception {
    WeeklyTrainingSchedule schedule =
        new WeeklyTrainingSchedule(
            List.of(
                new TrainingDay(
                    DayOfWeek.SATURDAY,
                    List.of(
                        new TrainingEntry(
                            "SATURDAY:RUNNING",
                            "RUNNING",
                            "Tirada larga",
                            "4.0 km",
                            "PLANNED",
                            null))),
                new TrainingDay(DayOfWeek.SUNDAY, List.of())));
    when(scheduleService.currentWeek()).thenReturn(schedule);

    mockMvc
        .perform(get("/api/v1/training/week"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days[0].sessions[0].id").value("SATURDAY:RUNNING"))
        .andExpect(jsonPath("$.days[0].sessions[0].status").value("PLANNED"))
        .andExpect(jsonPath("$.days[1].rest").value(true));
  }

  @Test
  void marksSessionCompleted() throws Exception {
    when(statusService.updateStatus(eq("SATURDAY:RUNNING"), eq(SessionStatus.COMPLETED), any()))
        .thenReturn(new StoredSessionStatus("SATURDAY:RUNNING", SessionStatus.COMPLETED, "Hecho"));

    mockMvc
        .perform(
            patch("/api/v1/training/sessions/SATURDAY:RUNNING/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\",\"notes\":\"Hecho\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("SATURDAY:RUNNING"))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.notes").value("Hecho"));
  }

  @Test
  void rejectsInvalidStatusWithValidationError() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/training/sessions/SATURDAY:RUNNING/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("status"));
  }

  @Test
  void unknownSessionReturnsNotFound() throws Exception {
    when(statusService.updateStatus(any(), any(), any()))
        .thenThrow(new NotFoundException("No existe la sesión de entrenamiento: X"));

    mockMvc
        .perform(
            patch("/api/v1/training/sessions/X/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
