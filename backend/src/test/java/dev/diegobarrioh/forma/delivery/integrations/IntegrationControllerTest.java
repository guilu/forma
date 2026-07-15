package dev.diegobarrioh.forma.delivery.integrations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.IntegrationService;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.SyncOutcome;
import dev.diegobarrioh.forma.domain.SyncResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link IntegrationController} (FOR-126): routing, response shape per {@code
 * specs/FOR-126/api.md}, and the token/secret-leak guard from {@code tests.md} ("No response body
 * or log line contains a token/secret"). {@link IntegrationService} is mocked, like {@code
 * GoalControllerTest} (FOR-125).
 */
@WebMvcTest(IntegrationController.class)
class IntegrationControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private IntegrationService service;

  private static final Instant CONNECTED_AT = Instant.parse("2026-07-15T08:00:00Z");
  private static final Instant SYNCED_AT = Instant.parse("2026-07-15T09:00:00Z");

  @Test
  void listBeforeAnyConnectionReturnsAllProvidersDisconnectedNeverA404() throws Exception {
    when(service.status())
        .thenReturn(
            List.of(IntegrationProvider.values()).stream()
                .map(IntegrationConnection::disconnectedDefault)
                .toList());

    mockMvc
        .perform(get("/api/v1/integrations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.providers").isArray())
        .andExpect(jsonPath("$.providers.length()").value(IntegrationProvider.values().length))
        .andExpect(jsonPath("$.providers[0].status").value("DISCONNECTED"))
        .andExpect(jsonPath("$.providers[0].connectedAt").doesNotExist());
  }

  @Test
  void listReturnsAConnectedProviderWithLastSyncOutcome() throws Exception {
    IntegrationConnection withings =
        new IntegrationConnection(
            IntegrationProvider.WITHINGS,
            IntegrationStatus.CONNECTED,
            CONNECTED_AT,
            SYNCED_AT,
            new SyncOutcome(SyncResult.OK, 0, null));
    when(service.status()).thenReturn(List.of(withings));

    mockMvc
        .perform(get("/api/v1/integrations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.providers[0].provider").value("WITHINGS"))
        .andExpect(jsonPath("$.providers[0].status").value("CONNECTED"))
        .andExpect(jsonPath("$.providers[0].connectedAt").value("2026-07-15T08:00:00Z"))
        .andExpect(jsonPath("$.providers[0].lastSyncAt").value("2026-07-15T09:00:00Z"))
        .andExpect(jsonPath("$.providers[0].lastSyncOutcome.result").value("OK"))
        .andExpect(jsonPath("$.providers[0].lastSyncOutcome.importedCount").value(0));
  }

  @Test
  void connectMarksTheProviderConnected() throws Exception {
    when(service.connect(IntegrationProvider.WITHINGS))
        .thenReturn(
            new IntegrationConnection(
                IntegrationProvider.WITHINGS,
                IntegrationStatus.CONNECTED,
                CONNECTED_AT,
                null,
                null));

    mockMvc
        .perform(post("/api/v1/integrations/withings/connect"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("WITHINGS"))
        .andExpect(jsonPath("$.status").value("CONNECTED"))
        .andExpect(jsonPath("$.connectedAt").value("2026-07-15T08:00:00Z"));
  }

  @Test
  void connectWithAnUnknownProviderReturnsValidationError() throws Exception {
    mockMvc
        .perform(post("/api/v1/integrations/not-a-provider/connect"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void syncOnAConnectedProviderReturnsARealOutcome() throws Exception {
    when(service.sync(IntegrationProvider.WITHINGS))
        .thenReturn(
            new IntegrationConnection(
                IntegrationProvider.WITHINGS,
                IntegrationStatus.CONNECTED,
                CONNECTED_AT,
                SYNCED_AT,
                new SyncOutcome(SyncResult.OK, 0, null)));

    mockMvc
        .perform(post("/api/v1/integrations/withings/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("OK"))
        .andExpect(jsonPath("$.importedCount").value(0))
        .andExpect(jsonPath("$.lastSyncAt").value("2026-07-15T09:00:00Z"))
        .andExpect(jsonPath("$.message").doesNotExist());
  }

  @Test
  void syncOnADisconnectedProviderReturnsANotConnectedOutcomeNotAnError() throws Exception {
    when(service.sync(IntegrationProvider.GOOGLE_FIT))
        .thenReturn(
            IntegrationConnection.disconnectedDefault(IntegrationProvider.GOOGLE_FIT)
                .withSyncOutcome(
                    null,
                    new SyncOutcome(
                        SyncResult.NOT_CONNECTED, 0, "El proveedor no está conectado.")));

    mockMvc
        .perform(post("/api/v1/integrations/google_fit/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("NOT_CONNECTED"))
        .andExpect(jsonPath("$.importedCount").value(0));
  }

  @Test
  void syncWithAnUnknownProviderReturnsValidationError() throws Exception {
    mockMvc
        .perform(post("/api/v1/integrations/not-a-provider/sync"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void disconnectMarksTheProviderDisconnected() throws Exception {
    when(service.disconnect(IntegrationProvider.WITHINGS))
        .thenReturn(IntegrationConnection.disconnectedDefault(IntegrationProvider.WITHINGS));

    mockMvc
        .perform(delete("/api/v1/integrations/withings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("WITHINGS"))
        .andExpect(jsonPath("$.status").value("DISCONNECTED"));
  }

  @Test
  void disconnectWithAnUnknownProviderReturnsValidationError() throws Exception {
    mockMvc
        .perform(delete("/api/v1/integrations/not-a-provider"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void connectResponseNeverContainsATokenOrSecretField() throws Exception {
    when(service.connect(eq(IntegrationProvider.WITHINGS)))
        .thenReturn(
            new IntegrationConnection(
                IntegrationProvider.WITHINGS,
                IntegrationStatus.CONNECTED,
                CONNECTED_AT,
                null,
                null));

    mockMvc
        .perform(post("/api/v1/integrations/withings/connect"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("token"))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("secret"))));
  }

  @Test
  void statusListResponseNeverContainsATokenOrSecretField() throws Exception {
    when(service.status())
        .thenReturn(
            List.of(
                new IntegrationConnection(
                    IntegrationProvider.WITHINGS,
                    IntegrationStatus.CONNECTED,
                    CONNECTED_AT,
                    SYNCED_AT,
                    new SyncOutcome(SyncResult.OK, 0, null))));

    mockMvc
        .perform(get("/api/v1/integrations"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("token"))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("secret"))));
  }
}
