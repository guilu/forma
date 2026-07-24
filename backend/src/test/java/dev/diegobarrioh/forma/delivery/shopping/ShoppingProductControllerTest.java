package dev.diegobarrioh.forma.delivery.shopping;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.ShoppingProductService;
import dev.diegobarrioh.forma.application.StoredShoppingProduct;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingProduct;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link ShoppingProductController} (FOR-36): list/create/update, validation
 * and not-found handling.
 */
@WebMvcTest(ShoppingProductController.class)
@Import(WebMvcAuthTestConfig.class)
class ShoppingProductControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private ShoppingProductService service;

  private static StoredShoppingProduct stored(String id, String name, String price) {
    return stored(id, name, price, ShoppingCategory.CEREALES_Y_LEGUMBRES);
  }

  private static StoredShoppingProduct stored(
      String id, String name, String price, ShoppingCategory category) {
    return new StoredShoppingProduct(
        id,
        new ShoppingProduct(
            name, null, "1 kg", new BigDecimal(price), null, "oats", null, null, category));
  }

  @Test
  void listsProducts() throws Exception {
    when(service.list()).thenReturn(List.of(stored("p1", "Avena", "1.95")));

    mockMvc
        .perform(get("/api/v1/shopping/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("p1"))
        .andExpect(jsonPath("$[0].name").value("Avena"))
        .andExpect(jsonPath("$[0].linkedFoodItemId").value("oats"))
        .andExpect(jsonPath("$[0].category").value("CEREALES_Y_LEGUMBRES"));
  }

  @Test
  void createsAProduct() throws Exception {
    when(service.create(any())).thenReturn(stored("new-id", "Avena", "1.95"));

    mockMvc
        .perform(
            post("/api/v1/shopping/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Avena\",\"estimatedPriceEur\":1.95,\"linkedFoodItemId\":\"oats\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("new-id"));
  }

  @Test
  void createsAProductWithCategory() throws Exception {
    when(service.create(any()))
        .thenReturn(stored("new-id", "Platano", "1.80", ShoppingCategory.FRUTAS_Y_VERDURAS));

    mockMvc
        .perform(
            post("/api/v1/shopping/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Platano\",\"estimatedPriceEur\":1.80,"
                        + "\"category\":\"FRUTAS_Y_VERDURAS\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.category").value("FRUTAS_Y_VERDURAS"));
  }

  @Test
  void rejectsUnknownCategoryWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/shopping/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Avena\",\"estimatedPriceEur\":1.95,\"category\":\"NOT_A_CATEGORY\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("category"));
  }

  @Test
  void productWithNoCategoryDefaultsToOtros() throws Exception {
    when(service.create(any()))
        .thenReturn(stored("new-id", "Avena", "1.95", ShoppingCategory.OTROS));

    mockMvc
        .perform(
            post("/api/v1/shopping/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Avena\",\"estimatedPriceEur\":1.95}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.category").value("OTROS"));
  }

  @Test
  void rejectsMissingNameWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/shopping/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estimatedPriceEur\":1.95}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("name"));
  }

  @Test
  void updatesAProduct() throws Exception {
    when(service.update(eq("p1"), any())).thenReturn(stored("p1", "Avena integral", "2.30"));

    mockMvc
        .perform(
            put("/api/v1/shopping/products/p1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Avena integral\",\"estimatedPriceEur\":2.30}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Avena integral"));
  }

  @Test
  void updateOfUnknownIdReturnsNotFound() throws Exception {
    when(service.update(eq("nope"), any()))
        .thenThrow(new NotFoundException("No existe el producto: nope"));

    mockMvc
        .perform(
            put("/api/v1/shopping/products/nope")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"estimatedPriceEur\":1.00}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
