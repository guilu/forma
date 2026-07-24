package dev.diegobarrioh.forma.delivery.food;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.CatalogFood;
import dev.diegobarrioh.forma.application.CatalogFoodService;
import dev.diegobarrioh.forma.application.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link FoodCatalogController} (FOR-173): listing all foods, single lookup
 * (with explicit JSON {@code null} for absent key nutrients), and 404 on an unknown id.
 */
@WebMvcTest(FoodCatalogController.class)
class FoodCatalogControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private CatalogFoodService service;

  private static CatalogFood oats() {
    return new CatalogFood(
        "oats",
        "Copos de avena",
        new BigDecimal("60.0"),
        370,
        new BigDecimal("13.0"),
        new BigDecimal("60.0"),
        new BigDecimal("7.0"),
        new BigDecimal("10.6"),
        new BigDecimal("0.0"),
        new BigDecimal("2.0"),
        new BigDecimal("1.2"));
  }

  private static CatalogFood rice() {
    return new CatalogFood(
        "rice",
        "Arroz",
        new BigDecimal("80.0"),
        360,
        new BigDecimal("7.0"),
        new BigDecimal("79.0"),
        new BigDecimal("1.0"),
        null,
        null,
        null,
        null);
  }

  @Test
  void listReturnsAllFoods() throws Exception {
    when(service.listAll()).thenReturn(List.of(oats(), rice()));

    mockMvc
        .perform(get("/api/v1/foods"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value("oats"))
        .andExpect(jsonPath("$[0].name").value("Copos de avena"))
        .andExpect(jsonPath("$[0].kcal").value(370))
        .andExpect(jsonPath("$[0].fiberG").value(10.6))
        .andExpect(jsonPath("$[0].sodiumMg").value(2.0))
        .andExpect(jsonPath("$[1].id").value("rice"));
  }

  @Test
  void findByIdReturnsFullFoodWhenPopulated() throws Exception {
    when(service.getById("oats")).thenReturn(oats());

    mockMvc
        .perform(get("/api/v1/foods/oats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("oats"))
        .andExpect(jsonPath("$.name").value("Copos de avena"))
        .andExpect(jsonPath("$.servingSizeG").value(60.0))
        .andExpect(jsonPath("$.kcal").value(370))
        .andExpect(jsonPath("$.proteinG").value(13.0))
        .andExpect(jsonPath("$.carbsG").value(60.0))
        .andExpect(jsonPath("$.fatG").value(7.0))
        .andExpect(jsonPath("$.fiberG").value(10.6))
        .andExpect(jsonPath("$.sugarsG").value(0.0))
        .andExpect(jsonPath("$.sodiumMg").value(2.0))
        .andExpect(jsonPath("$.saturatedFatG").value(1.2));
  }

  @Test
  void findByIdReturnsExplicitJsonNullForAbsentKeyNutrients() throws Exception {
    when(service.getById("rice")).thenReturn(rice());

    mockMvc
        .perform(get("/api/v1/foods/rice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("rice"))
        .andExpect(jsonPath("$.kcal").value(360))
        .andExpect(jsonPath("$.fiberG").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.sugarsG").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.sodiumMg").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.saturatedFatG").value(Matchers.nullValue()));
  }

  @Test
  void findByIdOfUnknownIdReturnsNotFound() throws Exception {
    when(service.getById("nope")).thenThrow(new NotFoundException("No existe el alimento: nope"));

    mockMvc
        .perform(get("/api/v1/foods/nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
