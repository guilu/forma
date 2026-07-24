package dev.diegobarrioh.forma.delivery.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.adapter.withings.WithingsHttpTransport;
import dev.diegobarrioh.forma.application.BodyMeasurementRepository;
import dev.diegobarrioh.forma.application.ExchangedTokens;
import dev.diegobarrioh.forma.application.IntegrationRepository;
import dev.diegobarrioh.forma.application.IntegrationService;
import dev.diegobarrioh.forma.application.IntegrationTokenStore;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack, real-database round trip for {@code POST /api/v1/integrations/withings/sync} (FOR-132
 * tests.md API Tests): real {@link IntegrationService}, real JDBC repositories/markers store, real
 * Flyway schema (through V16) — only the Withings HTTP transport is faked, so the Getmeas call
 * resolves to a recorded fixture instead of the live API (spec FOR-132 tests.md: "never call the
 * live Withings API in tests").
 *
 * <p>A CONNECTED Withings connection and a test OAuth token are seeded directly via {@link
 * IntegrationRepository}/{@link IntegrationTokenStore} (their FOR-131 test helpers), rather than
 * driving the real OAuth connect/callback dance — that round trip is already covered by {@code
 * IntegrationServiceTest}/{@code IntegrationControllerTest} (FOR-131); this test's only concern is
 * the FOR-132 sync-import path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(WebMvcAuthTestConfig.class)
class IntegrationSyncEndToEndTest {

  private static final String OWNER = IntegrationService.OWNER_ID;
  private static final String ACCESS_TOKEN = "fixture-access-token-e2e";
  private static final String REFRESH_TOKEN = "fixture-refresh-token-e2e";

  @Autowired private MockMvc mockMvc;
  @Autowired private IntegrationRepository integrationRepository;
  @Autowired private IntegrationTokenStore tokenStore;
  @Autowired private BodyMeasurementRepository bodyMeasurementRepository;
  @Autowired private WithingsHttpTransport transport;
  @Autowired private JdbcTemplate jdbcTemplate;

  private FakeWithingsHttpTransport fakeTransport;

  @BeforeEach
  void seedAConnectedWithingsConnectionWithATestToken() {
    jdbcTemplate.update("DELETE FROM body_measurements");
    jdbcTemplate.update("DELETE FROM integration_measure_marker");
    jdbcTemplate.update("DELETE FROM integration_connection");
    jdbcTemplate.update("DELETE FROM integration_token");

    integrationRepository.save(
        OWNER,
        IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS)
            .connect(Instant.now()));
    tokenStore.store(
        OWNER,
        IntegrationProvider.WITHINGS,
        new ExchangedTokens(ACCESS_TOKEN, REFRESH_TOKEN, Instant.now().plusSeconds(3600)));

    fakeTransport = (FakeWithingsHttpTransport) transport;
    fakeTransport.nextGetmeasResponse = null;
  }

  @Test
  void syncImportsTheFixtureMeasureGroupsIntoBodyMeasurementAndReturns200WithRealCounts()
      throws Exception {
    fakeTransport.nextGetmeasResponse = fixture("getmeas-two-groups.json");

    mockMvc
        .perform(post("/api/v1/integrations/withings/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("OK"))
        .andExpect(jsonPath("$.importedCount").value(2))
        .andExpect(jsonPath("$.duplicatesSkipped").value(0));

    assertThat(bodyMeasurementRepository.list()).hasSize(2);
  }

  @Test
  void reSyncingTheSameFixtureImportsZeroNewRowsAndReportsDuplicatesSkipped() throws Exception {
    fakeTransport.nextGetmeasResponse = fixture("getmeas-two-groups.json");
    mockMvc.perform(post("/api/v1/integrations/withings/sync")).andExpect(status().isOk());

    mockMvc
        .perform(post("/api/v1/integrations/withings/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("OK"))
        .andExpect(jsonPath("$.importedCount").value(0))
        .andExpect(jsonPath("$.duplicatesSkipped").value(2));

    // The headline idempotency assertion: no duplicate BodyMeasurement rows after re-sync.
    assertThat(bodyMeasurementRepository.list()).hasSize(2);
  }

  @Test
  void syncOnAnEmptyGetmeasResponseImportsZeroWithoutError() throws Exception {
    fakeTransport.nextGetmeasResponse = fixture("getmeas-empty.json");

    mockMvc
        .perform(post("/api/v1/integrations/withings/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("OK"))
        .andExpect(jsonPath("$.importedCount").value(0));

    assertThat(bodyMeasurementRepository.list()).isEmpty();
  }

  @Test
  void syncResponseNeverContainsTheAccessOrRefreshToken() throws Exception {
    fakeTransport.nextGetmeasResponse = fixture("getmeas-two-groups.json");

    mockMvc
        .perform(post("/api/v1/integrations/withings/sync"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsStringIgnoringCase(ACCESS_TOKEN))))
        .andExpect(content().string(not(containsStringIgnoringCase(REFRESH_TOKEN))))
        .andExpect(content().string(not(containsStringIgnoringCase("token"))))
        .andExpect(content().string(not(containsStringIgnoringCase("secret"))));
  }

  @Test
  void storedBodyMeasurementsCarryNoWithingsGroupIdColumnValueLeak() throws Exception {
    // Defense-in-depth companion to the dedup unit tests: confirm the persisted
    // body_measurements row shape has no grpid/external-id column at all (ADR-004: BodyMeasurement
    // stays provider-clean; the dedup key lives only in integration_measure_marker).
    fakeTransport.nextGetmeasResponse = fixture("getmeas-two-groups.json");

    mockMvc.perform(post("/api/v1/integrations/withings/sync")).andExpect(status().isOk());

    Integer markerRowCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM integration_measure_marker WHERE owner_id = ? AND provider = ?",
            Integer.class,
            OWNER,
            IntegrationProvider.WITHINGS.name());
    assertThat(markerRowCount).isEqualTo(2);
  }

  private static String fixture(String name) {
    try {
      return Files.readString(
          Path.of("src/test/resources/fixtures/withings/" + name), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @TestConfiguration
  static class FakeWithingsTransportConfig {
    @Bean
    @Primary
    WithingsHttpTransport fakeWithingsHttpTransport() {
      return new FakeWithingsHttpTransport();
    }
  }

  /**
   * Test-only {@link WithingsHttpTransport} replacing the real {@code JdkHttpWithingsTransport}
   * bean for this test's Spring context (never performs a real network call).
   */
  static class FakeWithingsHttpTransport implements WithingsHttpTransport {
    String nextGetmeasResponse;

    @Override
    public String post(String url, Map<String, String> formParams) {
      throw new UnsupportedOperationException("Not exercised by this end-to-end test");
    }

    @Override
    public String postAuthenticated(
        String url, Map<String, String> formParams, String accessToken) {
      return nextGetmeasResponse;
    }
  }
}
