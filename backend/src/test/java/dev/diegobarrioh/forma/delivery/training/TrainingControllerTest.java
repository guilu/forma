package dev.diegobarrioh.forma.delivery.training;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingDay;
import dev.diegobarrioh.forma.application.WeeklyTrainingSchedule.TrainingEntry;
import dev.diegobarrioh.forma.application.WeeklyTrainingScheduleService;
import java.time.DayOfWeek;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice test for {@link TrainingController} (FOR-26). Mocks the schedule service and asserts
 * the response DTO shape (running/strength/rest days).
 */
@WebMvcTest(TrainingController.class)
class TrainingControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private WeeklyTrainingScheduleService service;

  @Test
  void returnsTheWeekWithSessionsAndRestDays() throws Exception {
    WeeklyTrainingSchedule schedule =
        new WeeklyTrainingSchedule(
            List.of(
                new TrainingDay(
                    DayOfWeek.MONDAY,
                    List.of(
                        new TrainingEntry(
                            "STRENGTH", "Fuerza · Empuje", "3 ejercicios", "PLANNED"))),
                new TrainingDay(
                    DayOfWeek.SATURDAY,
                    List.of(new TrainingEntry("RUNNING", "Tirada larga", "4.0 km", "PLANNED"))),
                new TrainingDay(DayOfWeek.SUNDAY, List.of())));
    when(service.currentWeek()).thenReturn(schedule);

    mockMvc
        .perform(get("/api/v1/training/week"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days.length()").value(3))
        .andExpect(jsonPath("$.days[0].dayOfWeek").value("MONDAY"))
        .andExpect(jsonPath("$.days[0].rest").value(false))
        .andExpect(jsonPath("$.days[0].sessions[0].kind").value("STRENGTH"))
        .andExpect(jsonPath("$.days[1].sessions[0].detail").value("4.0 km"))
        .andExpect(jsonPath("$.days[2].rest").value(true))
        .andExpect(jsonPath("$.days[2].sessions.length()").value(0));
  }
}
