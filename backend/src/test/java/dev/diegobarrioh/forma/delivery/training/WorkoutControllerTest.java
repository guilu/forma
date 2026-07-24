package dev.diegobarrioh.forma.delivery.training;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.WorkoutTemplateService;
import dev.diegobarrioh.forma.domain.StrengthWorkoutItem;
import dev.diegobarrioh.forma.domain.StrengthWorkoutTemplate;
import dev.diegobarrioh.forma.domain.WorkoutType;
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
 * Web-slice tests for {@link WorkoutController} (FOR-99): listing templates, resolving one by type,
 * exercise-name resolution from the FOR-24 catalog (including a dangling-reference fallback), and
 * not-found handling.
 */
@WebMvcTest(WorkoutController.class)
@Import(WebMvcAuthTestConfig.class)
class WorkoutControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private WorkoutTemplateService service;

  private static StrengthWorkoutTemplate pushTemplate() {
    return new StrengthWorkoutTemplate(
        WorkoutType.PUSH, List.of(StrengthWorkoutItem.range("push-up", 1, 3, 8, 12, 90, 2)));
  }

  @Test
  void returnsAllTemplatesWithResolvedExerciseNames() throws Exception {
    when(service.allTemplates()).thenReturn(List.of(pushTemplate()));

    mockMvc
        .perform(get("/api/v1/training/workouts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].workoutType").value("PUSH"))
        .andExpect(jsonPath("$[0].items[0].exerciseId").value("push-up"))
        .andExpect(jsonPath("$[0].items[0].exerciseName").value("Flexiones"))
        .andExpect(jsonPath("$[0].items[0].order").value(1))
        .andExpect(jsonPath("$[0].items[0].sets").value(3))
        .andExpect(jsonPath("$[0].items[0].repScheme").value("RANGE"))
        .andExpect(jsonPath("$[0].items[0].repsMin").value(8))
        .andExpect(jsonPath("$[0].items[0].repsMax").value(12))
        .andExpect(jsonPath("$[0].items[0].restSeconds").value(90))
        .andExpect(jsonPath("$[0].items[0].rir").value(2));
  }

  @Test
  void anAmrapItemOmitsRepBoundsFromTheResponse() throws Exception {
    StrengthWorkoutTemplate template =
        new StrengthWorkoutTemplate(
            WorkoutType.PUSH, List.of(StrengthWorkoutItem.amrap("push-up", 1, 3, 60, 1)));
    when(service.allTemplates()).thenReturn(List.of(template));

    mockMvc
        .perform(get("/api/v1/training/workouts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].items[0].repScheme").value("AMRAP"))
        .andExpect(jsonPath("$[0].items[0].repsMin").doesNotExist())
        .andExpect(jsonPath("$[0].items[0].repsMax").doesNotExist());
  }

  @Test
  void aTimeHoldItemExposesDurationBoundsInsteadOfReps() throws Exception {
    StrengthWorkoutTemplate template =
        new StrengthWorkoutTemplate(
            WorkoutType.PUSH, List.of(StrengthWorkoutItem.timeHold("plank", 1, 3, 45, 75, 45, 2)));
    when(service.allTemplates()).thenReturn(List.of(template));

    mockMvc
        .perform(get("/api/v1/training/workouts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].items[0].repScheme").value("TIME"))
        .andExpect(jsonPath("$[0].items[0].durationSecondsMin").value(45))
        .andExpect(jsonPath("$[0].items[0].durationSecondsMax").value(75))
        .andExpect(jsonPath("$[0].items[0].repsMin").doesNotExist());
  }

  @Test
  void returnsOneTemplateByType() throws Exception {
    when(service.findByType(eq(WorkoutType.PUSH))).thenReturn(Optional.of(pushTemplate()));

    mockMvc
        .perform(get("/api/v1/training/workouts/PUSH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workoutType").value("PUSH"))
        .andExpect(jsonPath("$.items[0].exerciseName").value("Flexiones"));
  }

  @Test
  void unknownTypeStringReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/training/workouts/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void validTypeWithNoTemplateReturnsNotFound() throws Exception {
    when(service.findByType(eq(WorkoutType.FULL_BODY))).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/training/workouts/FULL_BODY"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void fallsBackToExerciseIdWhenNotInCatalog() throws Exception {
    StrengthWorkoutTemplate templateWithDanglingReference =
        new StrengthWorkoutTemplate(
            WorkoutType.PUSH,
            List.of(StrengthWorkoutItem.range("no-such-exercise", 1, 3, 8, 12, 90, 2)));
    when(service.allTemplates()).thenReturn(List.of(templateWithDanglingReference));

    mockMvc
        .perform(get("/api/v1/training/workouts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].items[0].exerciseName").value("no-such-exercise"));
  }
}
