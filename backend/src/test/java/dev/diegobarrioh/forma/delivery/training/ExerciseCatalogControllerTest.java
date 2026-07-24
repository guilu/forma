package dev.diegobarrioh.forma.delivery.training;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.CatalogExercise;
import dev.diegobarrioh.forma.application.CatalogExerciseService;
import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.domain.Modality;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link ExerciseCatalogController} (FOR-172): listing all/filtered exercises,
 * single lookup, and error mapping (bogus {@code ?modality=} -> 400, unknown id -> 404).
 */
@WebMvcTest(ExerciseCatalogController.class)
@Import(WebMvcAuthTestConfig.class)
class ExerciseCatalogControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private CatalogExerciseService service;

  private static CatalogExercise pushUp() {
    return new CatalogExercise(
        "push-up",
        "Flexiones",
        Modality.STRENGTH,
        "PUSH",
        "BODYWEIGHT",
        null,
        null,
        null,
        null,
        null,
        "Cuerpo recto, baja el pecho hasta cerca del suelo y empuja hacia arriba.",
        List.of("pecho", "tríceps"));
  }

  private static CatalogExercise runningEasy() {
    return new CatalogExercise(
        "running-easy",
        "Rodaje suave",
        Modality.RUNNING,
        null,
        null,
        null,
        null,
        null,
        null,
        "EASY",
        null,
        List.of());
  }

  @Test
  void listReturnsAllExercisesWithStrengthMusclesAndRunningSessionKind() throws Exception {
    when(service.list(Optional.empty())).thenReturn(List.of(pushUp(), runningEasy()));

    mockMvc
        .perform(get("/api/v1/training/exercises"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("push-up"))
        .andExpect(jsonPath("$[0].modality").value("STRENGTH"))
        .andExpect(jsonPath("$[0].muscles[0]").value("pecho"))
        .andExpect(jsonPath("$[0].muscles[1]").value("tríceps"))
        .andExpect(jsonPath("$[1].id").value("running-easy"))
        .andExpect(jsonPath("$[1].modality").value("RUNNING"))
        .andExpect(jsonPath("$[1].sessionKind").value("EASY"))
        .andExpect(jsonPath("$[1].muscles").doesNotExist());
  }

  @Test
  void filtersByModalityStrength() throws Exception {
    when(service.list(eq(Optional.of(Modality.STRENGTH)))).thenReturn(List.of(pushUp()));

    mockMvc
        .perform(get("/api/v1/training/exercises").param("modality", "STRENGTH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("push-up"));
  }

  @Test
  void filtersByModalityRunning() throws Exception {
    when(service.list(eq(Optional.of(Modality.RUNNING)))).thenReturn(List.of(runningEasy()));

    mockMvc
        .perform(get("/api/v1/training/exercises").param("modality", "RUNNING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("running-easy"));
  }

  @Test
  void bogusModalityReturnsBadRequest() throws Exception {
    mockMvc
        .perform(get("/api/v1/training/exercises").param("modality", "bogus"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void findByIdReturnsExerciseWithOrderedMuscles() throws Exception {
    when(service.getById("push-up")).thenReturn(pushUp());

    mockMvc
        .perform(get("/api/v1/training/exercises/push-up"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Flexiones"))
        .andExpect(jsonPath("$.muscles[0]").value("pecho"))
        .andExpect(jsonPath("$.muscles[1]").value("tríceps"));
  }

  @Test
  void findByIdOfUnknownIdReturnsNotFound() throws Exception {
    when(service.getById("nope")).thenThrow(new NotFoundException("No existe el ejercicio: nope"));

    mockMvc
        .perform(get("/api/v1/training/exercises/nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
