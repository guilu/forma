package dev.diegobarrioh.forma.delivery.plan;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.PlanAcceptance;
import dev.diegobarrioh.forma.application.PlanActivationService;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link PlanAcceptanceController}: what the first-login modal asks and does.
 */
@WebMvcTest(PlanAcceptanceController.class)
@Import(WebMvcAuthTestConfig.class)
class PlanAcceptanceControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private PlanActivationService service;

  @Test
  void reportsThePlanWaitingToBeStarted() throws Exception {
    when(service.pending()).thenReturn(PlanAcceptance.of("Dieta semanal — recomposición"));

    mockMvc
        .perform(get("/api/v1/plan-acceptance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pending").value(true))
        .andExpect(jsonPath("$.planName").value("Dieta semanal — recomposición"));
  }

  /** No plan is an ordinary answer, not a 404: the screens show their own empty state for it. */
  @Test
  void reportsNothingPendingWhenTheAccountHasNoPlan() throws Exception {
    when(service.pending()).thenReturn(PlanAcceptance.nothing());

    mockMvc
        .perform(get("/api/v1/plan-acceptance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pending").value(false))
        .andExpect(jsonPath("$.planName").doesNotExist());
  }

  @Test
  void acceptingStartsThePlan() throws Exception {
    mockMvc.perform(post("/api/v1/plan-acceptance")).andExpect(status().isNoContent());

    verify(service).accept();
  }

  @Test
  void acceptingWithNothingToAcceptIsANotFound() throws Exception {
    doThrow(new NotFoundException("No hay ningún plan pendiente de activar."))
        .when(service)
        .accept();

    mockMvc.perform(post("/api/v1/plan-acceptance")).andExpect(status().isNotFound());
  }
}
