package dev.diegobarrioh.forma.delivery.shopping;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import dev.diegobarrioh.forma.domain.ShoppingUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link ShoppingListController} (FOR-39, FOR-108, FOR-109): the list + budget
 * response including {@code unit}/{@code servings}/{@code generatedAt}/{@code productUrl}, the
 * check-toggle, the regenerate command, the quantity-edit command, and not-found/validation
 * handling.
 */
@WebMvcTest(ShoppingListController.class)
class ShoppingListControllerTest {

  private static final Instant GENERATED_AT = Instant.parse("2026-07-06T08:00:00Z");

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
                false,
                ShoppingUnit.KG,
                4,
                "https://tienda.mercadona.es/p1")),
        new ShoppingBudget(
            new BigDecimal("24.60"), new BigDecimal("106.52"), new BigDecimal("120.00"), false),
        GENERATED_AT);
  }

  @Test
  void returnsListWithItemsAndBudget() throws Exception {
    when(service.currentView()).thenReturn(view());

    mockMvc
        .perform(get("/api/v1/shopping/list"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.generatedAt").value(GENERATED_AT.toString()))
        .andExpect(jsonPath("$.items[0].id").value("i1"))
        .andExpect(jsonPath("$.items[0].productId").value("p1"))
        .andExpect(jsonPath("$.items[0].productName").value("Avena"))
        .andExpect(jsonPath("$.items[0].category").value("CEREALES_Y_LEGUMBRES"))
        .andExpect(jsonPath("$.items[0].unit").value("KG"))
        .andExpect(jsonPath("$.items[0].servings").value(4))
        .andExpect(jsonPath("$.items[0].productUrl").value("https://tienda.mercadona.es/p1"))
        .andExpect(jsonPath("$.budget.weeklyEur").value(24.60))
        .andExpect(jsonPath("$.budget.monthlyEur").value(106.52))
        .andExpect(jsonPath("$.budget.weeklyThresholdEur").value(120.00))
        .andExpect(jsonPath("$.budget.overThreshold").value(false));
  }

  @Test
  void nullServingsAreReturnedAsNullNotOmitted() throws Exception {
    ShoppingListView viewWithNoServings =
        new ShoppingListView(
            LocalDate.of(2026, 7, 6),
            ShoppingListStatus.ACTIVE,
            List.of(
                new ShoppingListView.Entry(
                    "i1",
                    "p1",
                    "Bolsas",
                    ShoppingCategory.OTROS,
                    1,
                    new BigDecimal("0.90"),
                    false,
                    ShoppingUnit.UD,
                    null,
                    null)),
            new ShoppingBudget(
                new BigDecimal("0.90"), new BigDecimal("3.90"), new BigDecimal("120.00"), false),
            GENERATED_AT);
    when(service.currentView()).thenReturn(viewWithNoServings);

    // Explicit-null check (not just "no value at path", which jsonPath cannot distinguish from
    // absence) — the spec requires servings: null / productUrl: null in the payload, never
    // omitted.
    mockMvc
        .perform(get("/api/v1/shopping/list"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"servings\":null")))
        .andExpect(content().string(containsString("\"productUrl\":null")));
  }

  @Test
  void togglesItemChecked() throws Exception {
    when(service.setChecked(eq("i1"), eq(true)))
        .thenReturn(
            new StoredShoppingListItem(
                "i1",
                new ShoppingListItem(
                    "p1", 2, new BigDecimal("3.90"), true, ShoppingUnit.UD, null)));

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

  @Test
  void regenerateReturnsRebuiltListWithNewGeneratedAt() throws Exception {
    Instant newGeneratedAt = Instant.parse("2026-07-13T09:00:00Z");
    ShoppingListView regenerated =
        new ShoppingListView(
            LocalDate.of(2026, 7, 6),
            ShoppingListStatus.ACTIVE,
            List.of(
                new ShoppingListView.Entry(
                    "i2",
                    "p1",
                    "Avena",
                    ShoppingCategory.CEREALES_Y_LEGUMBRES,
                    1,
                    new BigDecimal("1.95"),
                    false,
                    ShoppingUnit.UD,
                    null,
                    "https://tienda.mercadona.es/p1")),
            new ShoppingBudget(
                new BigDecimal("1.95"), new BigDecimal("8.44"), new BigDecimal("120.00"), false),
            newGeneratedAt);
    when(service.regenerate()).thenReturn(regenerated);

    mockMvc
        .perform(post("/api/v1/shopping/list/regenerate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.generatedAt").value(newGeneratedAt.toString()))
        .andExpect(jsonPath("$.items[0].id").value("i2"))
        .andExpect(jsonPath("$.items[0].quantity").value(1))
        .andExpect(jsonPath("$.items[0].checked").value(false));
  }

  @Test
  void regenerateWithNoActiveListReturnsNotFound() throws Exception {
    when(service.regenerate()).thenThrow(new NotFoundException("No hay lista de compra activa"));

    mockMvc
        .perform(post("/api/v1/shopping/list/regenerate"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void updatesItemQuantityAndRecalculatedCost() throws Exception {
    when(service.updateQuantity(eq("i1"), eq(5)))
        .thenReturn(
            new StoredShoppingListItem(
                "i1",
                new ShoppingListItem(
                    "p1", 5, new BigDecimal("9.75"), false, ShoppingUnit.KG, null)));

    mockMvc
        .perform(
            patch("/api/v1/shopping/list/items/i1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("i1"))
        .andExpect(jsonPath("$.quantity").value(5))
        .andExpect(jsonPath("$.estimatedCostEur").value(9.75))
        .andExpect(jsonPath("$.unit").value("KG"));
  }

  @Test
  void quantityEditOnUnknownItemReturnsNotFound() throws Exception {
    when(service.updateQuantity(eq("nope"), anyInt()))
        .thenThrow(new NotFoundException("No existe el artículo: nope"));

    mockMvc
        .perform(
            patch("/api/v1/shopping/list/items/nope")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":3}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void rejectsQuantityLessThanOne() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/shopping/list/items/i1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }
}
