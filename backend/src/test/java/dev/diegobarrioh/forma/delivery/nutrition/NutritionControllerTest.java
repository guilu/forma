package dev.diegobarrioh.forma.delivery.nutrition;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.NutritionDayCatalogService;
import dev.diegobarrioh.forma.domain.MealItem;
import dev.diegobarrioh.forma.domain.MealTemplate;
import dev.diegobarrioh.forma.domain.MealType;
import dev.diegobarrioh.forma.domain.NutritionDay;
import dev.diegobarrioh.forma.domain.NutritionDayTemplate;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link NutritionController} (FOR-34): the day response shape (targets,
 * ordered meals, optional post-run, resolved food names) and not-found handling.
 */
@WebMvcTest(NutritionController.class)
class NutritionControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private NutritionDayCatalogService service;

  private static NutritionDay runningDay() {
    NutritionDayTemplate template =
        new NutritionDayTemplate(NutritionDayType.RUNNING, 1940, 162, 271, 25, "note");
    MealTemplate breakfast =
        new MealTemplate(
            NutritionDayType.RUNNING,
            MealType.BREAKFAST,
            "Desayuno",
            LocalTime.of(8, 0),
            List.of(new MealItem("oats", 120)),
            null);
    MealTemplate postRun =
        new MealTemplate(
            NutritionDayType.RUNNING,
            MealType.POST_WORKOUT,
            "Recuperación (opcional)",
            LocalTime.of(20, 0),
            List.of(new MealItem("whey-protein", 20)),
            null);
    return new NutritionDay(template, List.of(breakfast, postRun));
  }

  @Test
  void returnsTheDayWithTargetsMealsAndOptionalPostRun() throws Exception {
    when(service.findByType(eq(NutritionDayType.RUNNING))).thenReturn(Optional.of(runningDay()));

    mockMvc
        .perform(get("/api/v1/nutrition/days/running"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("RUNNING"))
        .andExpect(jsonPath("$.targets.proteinG").value(162))
        .andExpect(jsonPath("$.meals[0].mealType").value("BREAKFAST"))
        // Food id resolved to its catalog name.
        .andExpect(jsonPath("$.meals[0].items[0].food").value("Avena"))
        .andExpect(jsonPath("$.meals[0].optional").value(false))
        .andExpect(jsonPath("$.meals[1].mealType").value("POST_WORKOUT"))
        .andExpect(jsonPath("$.meals[1].optional").value(true));
  }

  @Test
  void unknownDayTypeReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/nutrition/days/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
