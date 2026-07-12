package dev.diegobarrioh.forma.delivery.shopping;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.ShoppingListService;
import dev.diegobarrioh.forma.application.ShoppingListView;
import dev.diegobarrioh.forma.application.StoredShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingBudget;
import dev.diegobarrioh.forma.domain.ShoppingCategory;
import dev.diegobarrioh.forma.domain.ShoppingListItem;
import dev.diegobarrioh.forma.domain.ShoppingListStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link ShoppingListController} (FOR-39): the list + budget response, the
 * check-toggle, and not-found handling.
 */
@WebMvcTest(ShoppingListController.class)
class ShoppingListControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private ShoppingListService service;

  private static ShoppingListView view() {
    return new ShoppingListView(
        LocalDate.of(2026, 7, 6),
        ShoppingListStatus.ACTIVE,
        List.of(
            new ShoppingListView.Entry(
                "i1",
                "p1",
                "Avena",
                ShoppingCategory.CEREALES_Y_LEGUMBRES,
                2,
                new BigDecimal("3.90"),
                false)),
        new ShoppingBudget(new BigDecimal("24.60"), new BigDecimal("106.52")));
  }

  @Test
  void returnsListWithItemsAndBudget() throws Exception {
    when(service.currentView()).thenReturn(view());

    mockMvc
        .perform(get("/api/v1/shopping/list"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.items[0].id").value("i1"))
        .andExpect(jsonPath("$.items[0].productId").value("p1"))
        .andExpect(jsonPath("$.items[0].productName").value("Avena"))
        .andExpect(jsonPath("$.items[0].category").value("CEREALES_Y_LEGUMBRES"))
        .andExpect(jsonPath("$.budget.weeklyEur").value(24.60))
        .andExpect(jsonPath("$.budget.monthlyEur").value(106.52));
  }

  @Test
  void togglesItemChecked() throws Exception {
    when(service.setChecked(eq("i1"), eq(true)))
        .thenReturn(
            new StoredShoppingListItem(
                "i1", new ShoppingListItem("p1", 2, new BigDecimal("3.90"), true)));

    mockMvc
        .perform(
            patch("/api/v1/shopping/list/items/i1/checked")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checked\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("i1"))
        .andExpect(jsonPath("$.checked").value(true));
  }

  @Test
  void unknownItemReturnsNotFound() throws Exception {
    when(service.setChecked(eq("nope"), eq(true)))
        .thenThrow(new NotFoundException("No existe el artículo: nope"));

    mockMvc
        .perform(
            patch("/api/v1/shopping/list/items/nope/checked")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"checked\":true}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
