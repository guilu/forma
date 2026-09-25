package dev.diegobarrioh.forma.delivery.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.MuscleWorkedMap;
import dev.diegobarrioh.forma.application.MuscleWorkedMap.MuscleWorked;
import dev.diegobarrioh.forma.application.MuscleWorkedMapService;
import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.PlanRestartService;
import dev.diegobarrioh.forma.application.StoredSessionStatus;
import dev.diegobarrioh.forma.application.TrainingSessionRescheduleService;
import dev.diegobarrioh.forma.application.TrainingSessionStatusService;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService;
import dev.diegobarrioh.forma.application.WeeklyTrainingSummary;
import dev.diegobarrioh.forma.application.WeeklyTrainingSummaryService;
import dev.diegobarrioh.forma.domain.BodyView;
import dev.diegobarrioh.forma.domain.MuscleLoad;
import dev.diegobarrioh.forma.domain.SessionStatus;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.time.DayOfWeek;
import java.time.Instant;
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
 *
 * <p>There is no gate here (design D2 of training-progression-and-logging): {@code GET
 * /training/week} is always 200, and {@code planState} is what tells the caller whether the account
 * has never accepted a plan, is mid-cycle, or finished it — never a 4xx/204 for that.
 */
@WebMvcTest(TrainingController.class)
@Import(WebMvcAuthTestConfig.class)
class TrainingControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private WeeklyTrainingScheduleService scheduleService;
  @MockBean private TrainingSessionStatusService statusService;
  @MockBean private WeeklyTrainingSummaryService summaryService;
  @MockBean private MuscleWorkedMapService muscleWorkedMapService;
  @MockBean private TrainingSessionRescheduleService rescheduleService;
  @MockBean private PlanRestartService restartService;

  @Test
  void returnsNotStartedWithANullWeekAndAnEmptyCalendarWhenNoPlanWasEverAccepted()
      throws Exception {
    when(scheduleService.currentWeek())
        .thenReturn(
            new WeeklyTrainingSchedule(
                List.of(
                    new TrainingDay(DayOfWeek.MONDAY, List.of()),
                    new TrainingDay(DayOfWeek.TUESDAY, List.of())),
                "NOT_STARTED",
                null,
                16));

    mockMvc
        .perform(get("/api/v1/training/week"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.planState").value("NOT_STARTED"))
        .andExpect(jsonPath("$.planWeek").isEmpty())
        .andExpect(jsonPath("$.planTotalWeeks").value(16))
        .andExpect(jsonPath("$.days[0].rest").value(true))
        .andExpect(jsonPath("$.days[0].sessions").isEmpty());
  }

  @Test
  void returnsCompletedWithANullWeekWhenThePlanCycleIsOver() throws Exception {
    when(scheduleService.currentWeek())
        .thenReturn(
            new WeeklyTrainingSchedule(
                List.of(
                    new TrainingDay(
                        DayOfWeek.TUESDAY,
                        List.of(
                            new TrainingEntry(
                                "STRENGTH:PUSH",
                                "STRENGTH",
                                "Fuerza · Empuje",
                                "5 ejercicios",
                                "PLANNED",
                                null,
                                "PUSH",
                                BodyView.FRONT)))),
                "COMPLETED",
                null,
                16));

    mockMvc
        .perform(get("/api/v1/training/week"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.planState").value("COMPLETED"))
        .andExpect(jsonPath("$.planWeek").isEmpty())
        // Strength keeps going past the terminal state (design D4); only running stops.
        .andExpect(jsonPath("$.days[0].sessions[0].id").value("STRENGTH:PUSH"));
  }

  /**
   * Design D2 kept one guard for {@code GET /training/week}: if a second acceptance gate were ever
   * reintroduced and disagreed with the schedule service's own read, {@code IllegalStateException}
   * must map to 500 {@code INTERNAL_ERROR} with a {@code correlationId} — never leak internally, or
   * silently degrade. {@link dev.diegobarrioh.forma.delivery.error.GlobalExceptionHandler}'s
   * catch-all already does this for every unhandled exception (verified generically in {@code
   * GlobalExceptionHandlerTest}); this test pins that same behavior for this endpoint specifically,
   * so nobody has to re-derive it from the generic case.
   */
  @Test
  void anIllegalStateFromTheScheduleServiceMapsToInternalErrorWithACorrelationId()
      throws Exception {
    when(scheduleService.currentWeek())
        .thenThrow(new IllegalStateException("segundo gate en desacuerdo con el primero"));

    mockMvc
        .perform(get("/api/v1/training/week").header("X-Correlation-Id", "corr-week-500"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.correlationId").value("corr-week-500"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
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
                            "RUNNING:LONG_RUN",
                            "RUNNING",
                            "Tirada larga",
                            "4.0 km",
                            "PLANNED",
                            null,
                            null,
                            BodyView.FRONT),
                        new TrainingEntry(
                            "STRENGTH:PUSH",
                            "STRENGTH",
                            "Fuerza · Empuje",
                            "5 ejercicios",
                            "PLANNED",
                            null,
                            "PUSH",
                            BodyView.FRONT))),
                new TrainingDay(DayOfWeek.FRIDAY, List.of())),
            "ACTIVE",
            1,
            16);
    when(scheduleService.currentWeek()).thenReturn(schedule);

    mockMvc
        .perform(get("/api/v1/training/week"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.planState").value("ACTIVE"))
        .andExpect(jsonPath("$.planWeek").value(1))
        .andExpect(jsonPath("$.planTotalWeeks").value(16))
        .andExpect(jsonPath("$.days[0].sessions[0].id").value("RUNNING:LONG_RUN"))
        .andExpect(jsonPath("$.days[0].sessions[0].status").value("PLANNED"))
        .andExpect(jsonPath("$.days[0].sessions[0].workoutType").doesNotExist())
        .andExpect(jsonPath("$.days[0].sessions[0].bodyView").value("FRONT"))
        .andExpect(jsonPath("$.days[0].sessions[1].workoutType").value("PUSH"))
        .andExpect(jsonPath("$.days[1].rest").value(true));
  }

  @Test
  void movesASessionToAnotherDayAndReturnsTheRedrawnWeek() throws Exception {
    when(scheduleService.currentWeek())
        .thenReturn(
            new WeeklyTrainingSchedule(
                List.of(
                    new TrainingDay(
                        DayOfWeek.MONDAY,
                        List.of(
                            new TrainingEntry(
                                "STRENGTH:PUSH",
                                "STRENGTH",
                                "Fuerza · Empuje",
                                "5 ejercicios",
                                "PLANNED",
                                null,
                                "PUSH",
                                BodyView.FRONT)))),
                "ACTIVE",
                1,
                16));

    mockMvc
        .perform(
            patch("/api/v1/training/sessions/STRENGTH:PUSH/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"day\":\"MONDAY\"}"))
        .andExpect(status().isOk())
        // The whole week comes back so the caller can redraw without a second request.
        .andExpect(jsonPath("$.days[0].sessions[0].id").value("STRENGTH:PUSH"));

    verify(rescheduleService).reschedule("STRENGTH:PUSH", DayOfWeek.MONDAY);
  }

  @Test
  void aNullDayRestoresThePlannedDay() throws Exception {
    when(scheduleService.currentWeek())
        .thenReturn(new WeeklyTrainingSchedule(List.of(), "ACTIVE", 1, 16));

    mockMvc
        .perform(
            patch("/api/v1/training/sessions/STRENGTH:PUSH/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"day\":null}"))
        .andExpect(status().isOk());

    // Null is meaningful here, not missing: it clears the override.
    verify(rescheduleService).reschedule("STRENGTH:PUSH", null);
  }

  /**
   * A2 (design D5): restarting reanchors the cycle; the next {@code GET /training/week} — which
   * this test drives through the same {@code scheduleService} stub every other read here does —
   * comes back at week 1, {@code ACTIVE}.
   */
  @Test
  void restartsThePlanCycleThenTheNextWeekReadIsBackAtWeekOneActive() throws Exception {
    mockMvc.perform(post("/api/v1/training/plan/restart")).andExpect(status().isNoContent());

    verify(restartService).restart();

    when(scheduleService.currentWeek())
        .thenReturn(new WeeklyTrainingSchedule(List.of(), "ACTIVE", 1, 16));

    mockMvc
        .perform(get("/api/v1/training/week"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.planState").value("ACTIVE"))
        .andExpect(jsonPath("$.planWeek").value(1));
  }

  @Test
  void restartingWithoutAuthenticationIsRejected() throws Exception {
    mockMvc
        .perform(post("/api/v1/training/plan/restart").with(anonymous()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(restartService);
  }

  @Test
  void rejectsAnInvalidDayWithValidationError() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/training/sessions/STRENGTH:PUSH/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"day\":\"LUNES\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void marksSessionCompleted() throws Exception {
    when(statusService.updateStatus(eq("RUNNING:LONG_RUN"), eq(SessionStatus.COMPLETED), any()))
        .thenReturn(
            new StoredSessionStatus(
                "RUNNING:LONG_RUN",
                SessionStatus.COMPLETED,
                null,
                Instant.parse("2026-08-22T10:00:00Z"),
                "Hecho"));

    mockMvc
        .perform(
            patch("/api/v1/training/sessions/RUNNING:LONG_RUN/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\",\"notes\":\"Hecho\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("RUNNING:LONG_RUN"))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.notes").value("Hecho"));
  }

  @Test
  void rejectsInvalidStatusWithValidationError() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/training/sessions/RUNNING:LONG_RUN/status")
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
    when(muscleWorkedMapService.resolve("STRENGTH:PUSH"))
        .thenReturn(
            new MuscleWorkedMap(
                "STRENGTH:PUSH",
                List.of(
                    new MuscleWorked("pecho", MuscleLoad.HIGH),
                    new MuscleWorked("tríceps", MuscleLoad.HIGH),
                    new MuscleWorked("hombro", MuscleLoad.MEDIUM))));

    mockMvc
        .perform(get("/api/v1/training/sessions/STRENGTH:PUSH/muscle-map"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value("STRENGTH:PUSH"))
        .andExpect(jsonPath("$.muscles[0].muscle").value("pecho"))
        .andExpect(jsonPath("$.muscles[0].load").value("HIGH"))
        .andExpect(jsonPath("$.muscles[2].muscle").value("hombro"))
        .andExpect(jsonPath("$.muscles[2].load").value("MEDIUM"));
  }

  @Test
  void aNonStrengthSessionReturns200WithAnEmptyMuscleMapNeverA404() throws Exception {
    when(muscleWorkedMapService.resolve("RUNNING:LONG_RUN"))
        .thenReturn(new MuscleWorkedMap("RUNNING:LONG_RUN", List.of()));

    mockMvc
        .perform(get("/api/v1/training/sessions/RUNNING:LONG_RUN/muscle-map"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value("RUNNING:LONG_RUN"))
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
