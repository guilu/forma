package dev.diegobarrioh.forma.delivery;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * API smoke test (FOR-88). Uses the web slice so it needs no database, and confirms the versioned
 * base path is reachable and returns JSON.
 */
@WebMvcTest(PingController.class)
class PingControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void pingReturnsOk() throws Exception {
    mockMvc
        .perform(get(ApiPaths.V1 + "/ping"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));
  }
}
