package dev.diegobarrioh.forma.delivery.goals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.GoalService;
import dev.diegobarrioh.forma.application.GoalView;
import dev.diegobarrioh.forma.application.MilestonePatch;
import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.domain.Goal;
import dev.diegobarrioh.forma.domain.GoalMetric;
import dev.diegobarrioh.forma.domain.GoalProgress;
import dev.diegobarrioh.forma.domain.GoalStatus;
import dev.diegobarrioh.forma.domain.Milestone;
import dev.diegobarrioh.forma.domain.ProgressSource;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link GoalController} (FOR-125): routing, request validation and response
 * shape per {@code specs/FOR-125/api.md} and {@code tests.md}. {@link GoalService} is mocked, like
 * {@code UserProfileControllerTest} (FOR-107).
 */
@WebMvcTest(GoalController.class)
class GoalControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private GoalService service;

  private static GoalView view(String id, GoalProgress progress, List<Milestone> milestones) {
    Goal goal =
        new Goal(
            "Bajar a 12% grasa",
            GoalMetric.BODY_FAT_PCT,
            12.0,
            LocalDate.of(2026, 12, 31),
            GoalStatus.ACTIVE,
            milestones);
    return new GoalView(id, goal, progress);
  }

  @Test
  void listBeforeAnyGoalReturnsAnEmptyArrayNeverA404() throws Exception {
    when(service.list()).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/goals"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.goals").isArray())
        .andExpect(jsonPath("$.goals").isEmpty());
  }

  @Test
  void listReturnsGoalsWithDerivedProgressAndMilestones() throws Exception {
    GoalProgress progress =
        new GoalProgress(16.4, 12.0, 16.4 / 12.0, ProgressSource.BODY_MEASUREMENT);
    when(service.list())
        .thenReturn(
            List.of(view("g1", progress, List.of(new Milestone("m1", "15%", 15.0, false)))));

    mockMvc
        .perform(get("/api/v1/goals"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.goals[0].id").value("g1"))
        .andExpect(jsonPath("$.goals[0].title").value("Bajar a 12% grasa"))
        .andExpect(jsonPath("$.goals[0].metric").value("BODY_FAT_PCT"))
        .andExpect(jsonPath("$.goals[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$.goals[0].progress.current").value(16.4))
        .andExpect(jsonPath("$.goals[0].progress.source").value("BODY_MEASUREMENT"))
        .andExpect(jsonPath("$.goals[0].milestones[0].id").value("m1"))
        .andExpect(jsonPath("$.goals[0].milestones[0].completed").value(false));
  }

  @Test
  void listRepresentsUnlinkedOrMissingProgressAsExplicitNulls() throws Exception {
    GoalProgress progress = new GoalProgress(null, 12.0, null, ProgressSource.BODY_MEASUREMENT);
    when(service.list()).thenReturn(List.of(view("g1", progress, List.of())));

    mockMvc
        .perform(get("/api/v1/goals"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.goals[0].progress.current").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.goals[0].progress.ratio").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  void createsAGoalWithMilestones() throws Exception {
    GoalProgress progress = new GoalProgress(null, 12.0, null, ProgressSource.BODY_MEASUREMENT);
    when(service.create(any()))
        .thenReturn(view("new-id", progress, List.of(new Milestone("m1", "15%", 15.0, false))));

    mockMvc
        .perform(
            post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Bajar a 12% grasa","metric":"BODY_FAT_PCT","target":12.0,
                     "dueDate":"2026-12-31","milestones":[{"title":"15%","target":15.0}]}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("new-id"))
        .andExpect(jsonPath("$.milestones[0].title").value("15%"));
  }

  @Test
  void rejectsAnUnknownMetricWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"X\",\"metric\":\"NOT_A_METRIC\",\"target\":12.0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("metric"));
  }

  @Test
  void rejectsANonNumericTargetWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"X\",\"metric\":\"BODY_FAT_PCT\",\"target\":\"not-a-number\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void rejectsABlankTitleWithValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/goals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\",\"metric\":\"BODY_FAT_PCT\",\"target\":12.0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details[0].field").value("title"));
  }

  @Test
  void patchUpdatesFieldsAndMilestoneState() throws Exception {
    GoalProgress progress = new GoalProgress(null, 11.0, null, ProgressSource.BODY_MEASUREMENT);
    when(service.update(eq("g1"), eq("Bajar a 11% grasa"), eq(11.0), isNull(), isNull(), any()))
        .thenReturn(view("g1", progress, List.of(new Milestone("m1", "15%", 15.0, true))));

    mockMvc
        .perform(
            patch("/api/v1/goals/g1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Bajar a 11% grasa","target":11.0,
                     "milestones":[{"id":"m1","completed":true}]}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("g1"))
        .andExpect(jsonPath("$.milestones[0].completed").value(true));
  }

  @Test
  void patchOfAnUnknownIdReturnsNotFound() throws Exception {
    when(service.update(eq("nope"), any(), any(), any(), any(), any()))
        .thenThrow(new NotFoundException("No existe el objetivo: nope"));

    mockMvc
        .perform(
            patch("/api/v1/goals/nope")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"X\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void milestonePatchCompletedFieldMapsThrough() throws Exception {
    GoalProgress progress = new GoalProgress(null, 12.0, null, ProgressSource.BODY_MEASUREMENT);
    when(service.update(
            eq("g1"),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(List.of(new MilestonePatch("m1", true)))))
        .thenReturn(view("g1", progress, List.of(new Milestone("m1", "15%", 15.0, true))));

    mockMvc
        .perform(
            patch("/api/v1/goals/g1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"milestones\":[{\"id\":\"m1\",\"completed\":true}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.milestones[0].completed").value(true));
  }
}
