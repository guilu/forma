package dev.diegobarrioh.forma.delivery.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.MuscleWorkedMap;
import dev.diegobarrioh.forma.application.MuscleWorkedMap.MuscleWorked;
import dev.diegobarrioh.forma.application.MuscleWorkedMapService;
import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.StoredSessionStatus;
import dev.diegobarrioh.forma.application.TrainingSessionStatusService;
import dev.diegobarrioh.forma.application.UserProfileService;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService;
import dev.diegobarrioh.forma.application.WeeklyTrainingSummary;
import dev.diegobarrioh.forma.application.WeeklyTrainingSummaryService;
import dev.diegobarrioh.forma.domain.MuscleLoad;
import dev.diegobarrioh.forma.domain.SessionStatus;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.time.DayOfWeek;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link TrainingController} (FOR-26/FOR-27): the week response shape and the
 * status-update endpoint (happy path, validation, not-found).
 */
@WebMvcTest(TrainingController.class)
@Import(WebMvcAuthTestConfig.class)
class TrainingControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private WeeklyTrainingScheduleService scheduleService;
  @MockBean private TrainingSessionStatusService statusService;
  @MockBean private WeeklyTrainingSummaryService summaryService;
  @MockBean private MuscleWorkedMapService muscleWorkedMapService;
  @MockBean private UserProfileService profileService;

  /** Default to a completed first run so the plan is served; individual tests override. */
  @org.junit.jupiter.api.BeforeEach
  void onboardingCompletedByDefault() {
    when(profileService.firstRunCompleted()).thenReturn(true);
  }

  @Test
  void returnsAnEmptyWeekBeforeOnboardingFirstRunGate() throws Exception {
    when(profileService.firstRunCompleted()).thenReturn(false);

    mockMvc
        .perform(get("/api/v1/training/week"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days.length()").value(7))
        .andExpect(jsonPath("$.days[0].rest").value(true))
        .andExpect(jsonPath("$.days[0].sessions").isEmpty());
  }

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
                new TrainingDay(DayOfWeek.FRIDAY, List.of())));
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

  @Test
  void returnsTheWeeklySummaryForAPopulatedWeek() throws Exception {
    WeeklyTrainingSummary summary =
        new WeeklyTrainingSummary(
            3, 2, 3, 1, 8.6, 5.0, "Carrera: 2/3 sesiones (5.0/8.6 km). Fuerza: 1/3 sesiones.");
    when(summaryService.currentSummary()).thenReturn(summary);

    mockMvc
        .perform(get("/api/v1/training/weekly-summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plannedRunningSessions").value(3))
        .andExpect(jsonPath("$.completedRunningSessions").value(2))
        .andExpect(jsonPath("$.plannedStrengthSessions").value(3))
        .andExpect(jsonPath("$.completedStrengthSessions").value(1))
        .andExpect(jsonPath("$.totalPlannedRunningKm").value(8.6))
        .andExpect(jsonPath("$.completedRunningKm").value(5.0))
        .andExpect(
            jsonPath("$.message")
                .value("Carrera: 2/3 sesiones (5.0/8.6 km). Fuerza: 1/3 sesiones."));
  }

  @Test
  void returnsZeroedWeeklySummaryForAnEmptyWeek() throws Exception {
    WeeklyTrainingSummary summary =
        new WeeklyTrainingSummary(
            0, 0, 0, 0, 0.0, 0.0, "No hay entrenamientos planificados esta semana.");
    when(summaryService.currentSummary()).thenReturn(summary);

    mockMvc
        .perform(get("/api/v1/training/weekly-summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plannedRunningSessions").value(0))
        .andExpect(jsonPath("$.completedRunningSessions").value(0))
        .andExpect(jsonPath("$.plannedStrengthSessions").value(0))
        .andExpect(jsonPath("$.completedStrengthSessions").value(0))
        .andExpect(jsonPath("$.totalPlannedRunningKm").value(0.0))
        .andExpect(jsonPath("$.completedRunningKm").value(0.0))
        .andExpect(jsonPath("$.message").value("No hay entrenamientos planificados esta semana."));
  }

  @Test
  void returnsTheMuscleMapForAStrengthSessionPerApiMd() throws Exception {
    when(muscleWorkedMapService.resolve("TUESDAY:STRENGTH"))
        .thenReturn(
            new MuscleWorkedMap(
                "TUESDAY:STRENGTH",
                List.of(
                    new MuscleWorked("pecho", MuscleLoad.HIGH),
                    new MuscleWorked("tríceps", MuscleLoad.HIGH),
                    new MuscleWorked("hombro", MuscleLoad.MEDIUM))));

    mockMvc
        .perform(get("/api/v1/training/sessions/TUESDAY:STRENGTH/muscle-map"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value("TUESDAY:STRENGTH"))
        .andExpect(jsonPath("$.muscles[0].muscle").value("pecho"))
        .andExpect(jsonPath("$.muscles[0].load").value("HIGH"))
        .andExpect(jsonPath("$.muscles[2].muscle").value("hombro"))
        .andExpect(jsonPath("$.muscles[2].load").value("MEDIUM"));
  }

  @Test
  void aNonStrengthSessionReturns200WithAnEmptyMuscleMapNeverA404() throws Exception {
    when(muscleWorkedMapService.resolve("SATURDAY:RUNNING"))
        .thenReturn(new MuscleWorkedMap("SATURDAY:RUNNING", List.of()));

    mockMvc
        .perform(get("/api/v1/training/sessions/SATURDAY:RUNNING/muscle-map"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value("SATURDAY:RUNNING"))
        .andExpect(jsonPath("$.muscles").isEmpty());
  }

  @Test
  void anUnknownSessionIdReturns404ForTheMuscleMap() throws Exception {
    when(muscleWorkedMapService.resolve("X"))
        .thenThrow(new NotFoundException("No existe la sesión de entrenamiento: X"));

    mockMvc
        .perform(get("/api/v1/training/sessions/X/muscle-map"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
