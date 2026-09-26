package dev.diegobarrioh.forma.delivery.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.TrainingSetLogService;
import dev.diegobarrioh.forma.application.TrainingSetLogView;
import dev.diegobarrioh.forma.domain.LoggedSet;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link TrainingSetLogController} (training-progression-and-logging slice B,
 * design D7): the read grid shape, the per-set write endpoint (not bulk), and the standard
 * validation/not-found mappings (ADR-005).
 */
@WebMvcTest(TrainingSetLogController.class)
@Import(WebMvcAuthTestConfig.class)
class TrainingSetLogControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private TrainingSetLogService setLogService;

  @Test
  void returnsTheSessionIdAndSetsGrid() throws Exception {
    when(setLogService.getSets("STRENGTH:PUSH"))
        .thenReturn(
            new TrainingSetLogView(
                "STRENGTH:PUSH",
                List.of(
                    new LoggedSet("push-up", 1, 60.0, 8, true),
                    new LoggedSet("push-up", 2, null, null, false))));

    mockMvc
        .perform(get("/api/v1/training/sessions/STRENGTH:PUSH/sets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value("STRENGTH:PUSH"))
        .andExpect(jsonPath("$.sets[0].exerciseId").value("push-up"))
        .andExpect(jsonPath("$.sets[0].setNumber").value(1))
        .andExpect(jsonPath("$.sets[0].weightKg").value(60.0))
        .andExpect(jsonPath("$.sets[0].reps").value(8))
        .andExpect(jsonPath("$.sets[0].done").value(true))
        .andExpect(jsonPath("$.sets[1].weightKg").isEmpty())
        .andExpect(jsonPath("$.sets[1].done").value(false));
  }

  @Test
  void writesOneSetAndReturnsItNotTheWholeSession() throws Exception {
    when(setLogService.putSet(eq("STRENGTH:PUSH"), eq("push-up"), eq(1), eq(60.0), eq(8), eq(true)))
        .thenReturn(new LoggedSet("push-up", 1, 60.0, 8, true));

    mockMvc
        .perform(
            put("/api/v1/training/sessions/STRENGTH:PUSH/sets/push-up/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weightKg\":60.0,\"reps\":8,\"done\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exerciseId").value("push-up"))
        .andExpect(jsonPath("$.setNumber").value(1))
        .andExpect(jsonPath("$.weightKg").value(60.0))
        .andExpect(jsonPath("$.reps").value(8))
        .andExpect(jsonPath("$.done").value(true));

    verify(setLogService).putSet("STRENGTH:PUSH", "push-up", 1, 60.0, 8, true);
  }

  @Test
  void acceptsAPartialWriteWithOnlyWeight() throws Exception {
    when(setLogService.putSet(
            eq("STRENGTH:PUSH"), eq("push-up"), eq(1), eq(60.0), eq(null), eq(false)))
        .thenReturn(new LoggedSet("push-up", 1, 60.0, null, false));

    mockMvc
        .perform(
            put("/api/v1/training/sessions/STRENGTH:PUSH/sets/push-up/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weightKg\":60.0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weightKg").value(60.0))
        .andExpect(jsonPath("$.reps").isEmpty());
  }

  @Test
  void rejectsANegativeWeightWithValidationError() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/training/sessions/STRENGTH:PUSH/sets/push-up/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weightKg\":-5.0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("weightKg"));
  }

  @Test
  void anUnknownSessionReturns404OnRead() throws Exception {
    when(setLogService.getSets("X"))
        .thenThrow(new NotFoundException("No existe la sesión de entrenamiento: X"));

    mockMvc
        .perform(get("/api/v1/training/sessions/X/sets"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void aSetTheTemplateDoesNotPrescribeReturns404OnWrite() throws Exception {
    when(setLogService.putSet(
            eq("STRENGTH:PUSH"), eq("push-up"), eq(99), any(), any(), anyBoolean()))
        .thenThrow(new NotFoundException("La plantilla actual no prescribe la serie 99"));

    mockMvc
        .perform(
            put("/api/v1/training/sessions/STRENGTH:PUSH/sets/push-up/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
