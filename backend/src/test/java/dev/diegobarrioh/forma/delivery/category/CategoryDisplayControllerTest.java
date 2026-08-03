package dev.diegobarrioh.forma.delivery.category;

import static dev.diegobarrioh.forma.support.AuthTestSupport.asAdmin;
import static dev.diegobarrioh.forma.support.AuthTestSupport.asUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The category endpoints end to end (FOR-197): who may edit, and that V39 really seeded both
 * vocabularies with what the frontend used to hardcode.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryDisplayControllerTest {

  private static final String PATH = "/api/v1/categories";
  private static final UUID SOMEONE = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void anOrdinaryUserCannotRenameACategory() throws Exception {
    mockMvc
        .perform(
            put(PATH + "/FOOD/LACTEO")
                .with(asUser(SOMEONE, "someone@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Mío\",\"icon\":\"🧀\"}"))
        .andExpect(status().isForbidden());
  }

  /** Every screen that shows a category needs its name, so reading stays open. */
  @Test
  void anyoneSignedInReadsBothVocabularies() throws Exception {
    mockMvc
        .perform(get(PATH).with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        // 10 food groups (V43) + 6 shopping aisles.
        .andExpect(jsonPath("$.length()").value(16))
        .andExpect(jsonPath("$[?(@.code=='CARBOHIDRATO')].label").value("Carbohidrato"))
        .andExpect(jsonPath("$[?(@.code=='CARBOHIDRATO')].scope").value("FOOD"))
        .andExpect(jsonPath("$[?(@.code=='OTROS')].scope").value("SHOPPING"));

    mockMvc
        .perform(get(PATH + "?scope=FOOD").with(asUser(SOMEONE, "someone@forma.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(10));
  }

  @Test
  void anAdminRenamesACategory() throws Exception {
    try {
      mockMvc
          .perform(
              put(PATH + "/FOOD/LACTEO")
                  .with(asAdmin(UUID.randomUUID(), "admin@forma.test"))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"label\":\"Lácteos y derivados\",\"icon\":\"🧀\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.label").value("Lácteos y derivados"))
          .andExpect(jsonPath("$.icon").value("🧀"))
          // The code is what rows point at, and it is not up for editing.
          .andExpect(jsonPath("$.code").value("LACTEO"));
    } finally {
      // Seed data shared with every other test in the suite.
      jdbcTemplate.update(
          "UPDATE category_display SET label = ?, icon = ? WHERE scope = 'FOOD' AND code = 'LACTEO'",
          "Lácteo",
          "🥛");
    }
  }

  /** A code nothing may ever be filed under is not a category to create by the back door. */
  @Test
  void refusesACodeThatDoesNotExist() throws Exception {
    mockMvc
        .perform(
            put(PATH + "/FOOD/INVENTADA")
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Inventada\",\"icon\":\"🎲\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void refusesABlankLabel() throws Exception {
    mockMvc
        .perform(
            put(PATH + "/FOOD/GRASA")
                .with(asAdmin(UUID.randomUUID(), "admin@forma.test"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"  \",\"icon\":\"🫒\"}"))
        .andExpect(status().isBadRequest());
  }
}
