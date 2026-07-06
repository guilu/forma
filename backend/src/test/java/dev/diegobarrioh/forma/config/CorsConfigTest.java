package dev.diegobarrioh.forma.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.delivery.PingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the CORS baseline: the configured dev frontend origin is allowed to call the API, and an
 * unknown origin is rejected (no wildcard). Uses the web slice with the ping endpoint as a stand-in
 * for any {@code /api/v1} route.
 */
@WebMvcTest(PingController.class)
@Import(CorsConfig.class)
class CorsConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void allowsConfiguredDevOriginOnPreflight() throws Exception {
    mockMvc
        .perform(
            options(ApiPaths.V1 + "/ping")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  @Test
  void echoesAllowOriginOnActualRequest() throws Exception {
    mockMvc
        .perform(get(ApiPaths.V1 + "/ping").header("Origin", "http://localhost:5173"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  @Test
  void rejectsUnknownOriginOnPreflight() throws Exception {
    mockMvc
        .perform(
            options(ApiPaths.V1 + "/ping")
                .header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }
}
