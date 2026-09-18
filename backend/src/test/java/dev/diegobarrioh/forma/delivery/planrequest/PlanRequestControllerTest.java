package dev.diegobarrioh.forma.delivery.planrequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.ConflictException;
import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.PlanRequest;
import dev.diegobarrioh.forma.application.PlanRequestService;
import dev.diegobarrioh.forma.domain.ActivityLevel;
import dev.diegobarrioh.forma.domain.CuisineStyle;
import dev.diegobarrioh.forma.domain.DietPattern;
import dev.diegobarrioh.forma.domain.MainGoal;
import dev.diegobarrioh.forma.domain.PlanObjective;
import dev.diegobarrioh.forma.domain.PlanRequestStatus;
import dev.diegobarrioh.forma.domain.Sex;
import dev.diegobarrioh.forma.domain.TrainingEquipment;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link PlanRequestController} (ADR-015 slice 2). Mocks {@link
 * PlanRequestService} so the test focuses on the delivery contract: routing, {@code @Valid} bounds
 * → {@code VALIDATION_ERROR}, and the response DTO shape (following BodyMeasurementControllerTest's
 * pattern).
 */
@WebMvcTest(PlanRequestController.class)
@Import(WebMvcAuthTestConfig.class)
class PlanRequestControllerTest {

  private static final String PATH = "/api/v1/plan-requests";

  @Autowired private MockMvc mockMvc;
  @MockBean private PlanRequestService service;

  private static PlanRequest openRequest() {
    return new PlanRequest(
        UUID.randomUUID(),
        UUID.randomUUID(),
        PlanRequestStatus.PENDING,
        "1",
        null,
        Sex.MALE,
        38,
        73.6,
        180.0,
        ActivityLevel.MODERATE,
        MainGoal.COMPOSICION,
        PlanObjective.WEIGHT_LOSS,
        5,
        Set.of(DayOfWeek.MONDAY),
        Set.of(TrainingEquipment.DUMBBELLS),
        5,
        DietPattern.OMNIVORE,
        CuisineStyle.ESPANOLA,
        2078,
        160.0,
        260.0,
        70.0,
        null,
        null,
        null,
        null,
        null,
        0,
        OffsetDateTime.parse("2026-09-18T10:00:00Z"),
        null,
        null,
        OffsetDateTime.parse("2026-09-18T10:00:00Z"));
  }

  private static final String VALID_BODY =
      """
      {"direction":"LOSE_FAT","trainingDaysPerWeek":5,
       "trainingWeekdays":["MONDAY","TUESDAY"],"equipment":["DUMBBELLS"],
       "mealsPerDay":5,"dietPattern":"OMNIVORE","cuisineStyle":"ESPANOLA"}
      """;

  @Test
  void postWithValidBodyReturns201WithResponseShape() throws Exception {
    when(service.request(any(), anyInt(), any(), any(), anyInt(), any(), any()))
        .thenReturn(openRequest());

    mockMvc
        .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.planKcal").value(2078))
        .andExpect(jsonPath("$.planObjective").value("WEIGHT_LOSS"))
        .andExpect(jsonPath("$.mainGoal").value("COMPOSICION"));
  }

  @Test
  void postWithMissingDirectionReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"trainingDaysPerWeek":5,"mealsPerDay":5,
                     "dietPattern":"OMNIVORE","cuisineStyle":"ESPANOLA"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("direction"));
  }

  @Test
  void postWithMealsPerDayOutOfRangeReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"direction":"MAINTAIN","trainingDaysPerWeek":3,"mealsPerDay":9,
                     "dietPattern":"OMNIVORE","cuisineStyle":"ESPANOLA"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("mealsPerDay"));
  }

  @Test
  void postWithTrainingDaysOutOfRangeReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"direction":"MAINTAIN","trainingDaysPerWeek":8,"mealsPerDay":3,
                     "dietPattern":"OMNIVORE","cuisineStyle":"ESPANOLA"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("trainingDaysPerWeek"));
  }

  @Test
  void postWhenTheCallerAlreadyHasAnOpenRequestReturns409() throws Exception {
    when(service.request(any(), anyInt(), any(), any(), anyInt(), any(), any()))
        .thenThrow(new ConflictException("Ya tienes una petición de plan en curso."));

    mockMvc
        .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void getCurrentReturnsTheOpenRequest() throws Exception {
    when(service.currentOpen()).thenReturn(openRequest());

    mockMvc
        .perform(get(PATH + "/current"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.planKcal").value(2078));
  }

  @Test
  void getCurrentAnswersNotFoundWhenTheCallerHasNoOpenRequest() throws Exception {
    when(service.currentOpen())
        .thenThrow(new NotFoundException("No tienes ninguna petición de plan en curso."));

    mockMvc
        .perform(get(PATH + "/current"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
