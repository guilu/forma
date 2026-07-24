package dev.diegobarrioh.forma.delivery.integrations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.diegobarrioh.forma.application.ConnectResult;
import dev.diegobarrioh.forma.application.IntegrationService;
import dev.diegobarrioh.forma.application.OAuthStateException;
import dev.diegobarrioh.forma.application.ProviderOAuthException;
import dev.diegobarrioh.forma.domain.IntegrationConnection;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import dev.diegobarrioh.forma.domain.IntegrationStatus;
import dev.diegobarrioh.forma.domain.SyncOutcome;
import dev.diegobarrioh.forma.domain.SyncResult;
import dev.diegobarrioh.forma.support.WebMvcAuthTestConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-slice tests for {@link IntegrationController} (FOR-126 status/sync/disconnect, extended by
 * FOR-131 with real Withings OAuth connect/callback): routing, response shape per {@code
 * specs/FOR-131/api.md}, and the token/secret/code/state-leak guard from {@code tests.md} ("No
 * response body, header, log line, or error message contains a token, code, or state"). {@link
 * IntegrationService} is mocked, like {@code GoalControllerTest} (FOR-125).
 */
@WebMvcTest(IntegrationController.class)
@Import(WebMvcAuthTestConfig.class)
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
            new SyncOutcome(SyncResult.OK, 0, 0, null));
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
  void connectOnAProviderWithARegisteredOAuthGatewayReturnsAnAuthorizationUrl() throws Exception {
    // FOR-131 api.md: connect "changed" — Withings no longer flips status directly.
    when(service.connect(IntegrationProvider.WITHINGS))
        .thenReturn(
            ConnectResult.authorizationRequired(
                "https://account.withings.com/oauth2_user/authorize2?client_id=test&state=abc&redirect_uri=https%3A%2F%2Fforma.diegobarrioh.dev%2Fauth&scope=user.metrics&code_challenge=xyz"));

    mockMvc
        .perform(post("/api/v1/integrations/withings/connect"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.authorizationUrl")
                .value(
                    org.hamcrest.Matchers.startsWith(
                        "https://account.withings.com/oauth2_user/authorize2")))
        .andExpect(
            jsonPath("$.authorizationUrl")
                .value(
                    org.hamcrest.Matchers.containsString(
                        "redirect_uri=https%3A%2F%2Fforma.diegobarrioh.dev%2Fauth")))
        .andExpect(
            jsonPath("$.authorizationUrl")
                .value(org.hamcrest.Matchers.containsString("scope=user.metrics")))
        .andExpect(jsonPath("$.provider").doesNotExist())
        .andExpect(jsonPath("$.status").doesNotExist())
        .andExpect(jsonPath("$.connectedAt").doesNotExist());
  }

  @Test
  void connectOnAProviderWithoutAnOAuthGatewayKeepsTheMockConnectResponseShape() throws Exception {
    // Google Fit/Apple Health have no registered OAuth app (out of FOR-131 scope) — the FOR-126
    // mock-connect fallback still applies, unchanged.
    when(service.connect(IntegrationProvider.GOOGLE_FIT))
        .thenReturn(
            ConnectResult.connected(
                new IntegrationConnection(
                    IntegrationProvider.GOOGLE_FIT,
                    IntegrationStatus.CONNECTED,
                    CONNECTED_AT,
                    null,
                    null)));

    mockMvc
        .perform(post("/api/v1/integrations/google_fit/connect"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorizationUrl").doesNotExist())
        .andExpect(jsonPath("$.provider").value("GOOGLE_FIT"))
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
  void callbackWithAValidRequestReturnsTheConnectedStatus() throws Exception {
    when(service.callback(IntegrationProvider.WITHINGS, "withings-auth-code", "the-state"))
        .thenReturn(
            new IntegrationConnection(
                IntegrationProvider.WITHINGS,
                IntegrationStatus.CONNECTED,
                CONNECTED_AT,
                null,
                null));

    mockMvc
        .perform(
            post("/api/v1/integrations/withings/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"withings-auth-code\",\"state\":\"the-state\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.provider").value("WITHINGS"))
        .andExpect(jsonPath("$.status").value("CONNECTED"))
        .andExpect(jsonPath("$.connectedAt").value("2026-07-15T08:00:00Z"));
  }

  @Test
  void callbackWithAMissingCodeReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/integrations/withings/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"state\":\"the-state\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void callbackWithAMissingStateReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/integrations/withings/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"withings-auth-code\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void callbackWithAnUnknownProviderReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/integrations/not-a-provider/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"c\",\"state\":\"s\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void callbackWithAMismatchedStateReturnsValidationErrorAndNeverEchoesTheState() throws Exception {
    when(service.callback(IntegrationProvider.WITHINGS, "some-code", "wrong-state"))
        .thenThrow(new OAuthStateException());

    mockMvc
        .perform(
            post("/api/v1/integrations/withings/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"some-code\",\"state\":\"wrong-state\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("wrong-state"))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("some-code"))));
  }

  @Test
  void callbackWhenTheProviderExchangeFailsReturnsABadGatewayWithNoSecretLeak() throws Exception {
    when(service.callback(IntegrationProvider.WITHINGS, "some-code", "the-state"))
        .thenThrow(
            new ProviderOAuthException(
                "Withings devolvió un error al procesar la solicitud (status=503)."));

    mockMvc
        .perform(
            post("/api/v1/integrations/withings/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"some-code\",\"state\":\"the-state\"}"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("PROVIDER_ERROR"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("token"))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("secret"))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("some-code"))));
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
                new SyncOutcome(SyncResult.OK, 0, 0, null)));

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
                        SyncResult.NOT_CONNECTED, 0, 0, "El proveedor no está conectado.")));

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
  void syncOnAConnectedWithingsProviderReturnsRealImportedAndDuplicatesSkippedCounts()
      throws Exception {
    // FOR-132: sync now performs a real Withings import; the response carries real counts,
    // including the new duplicatesSkipped field (spec FOR-132 api.md).
    when(service.sync(IntegrationProvider.WITHINGS))
        .thenReturn(
            new IntegrationConnection(
                IntegrationProvider.WITHINGS,
                IntegrationStatus.CONNECTED,
                CONNECTED_AT,
                SYNCED_AT,
                new SyncOutcome(SyncResult.OK, 3, 12, null)));

    mockMvc
        .perform(post("/api/v1/integrations/withings/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("OK"))
        .andExpect(jsonPath("$.importedCount").value(3))
        .andExpect(jsonPath("$.duplicatesSkipped").value(12));
  }

  @Test
  void syncWhenTokenRefreshFailsReturnsANeedsReauthOutcomeAsA200NotAnError() throws Exception {
    when(service.sync(IntegrationProvider.WITHINGS))
        .thenReturn(
            new IntegrationConnection(
                IntegrationProvider.WITHINGS,
                IntegrationStatus.NEEDS_REAUTH,
                CONNECTED_AT,
                SYNCED_AT,
                new SyncOutcome(
                    SyncResult.NEEDS_REAUTH,
                    0,
                    0,
                    "Reconecta el proveedor para seguir sincronizando.")));

    mockMvc
        .perform(post("/api/v1/integrations/withings/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("NEEDS_REAUTH"))
        .andExpect(jsonPath("$.importedCount").value(0));
  }

  @Test
  void syncWhenTheProviderFailsReturnsAReadableErrorOutcomeAsA200NotAnHttp5xx() throws Exception {
    when(service.sync(IntegrationProvider.WITHINGS))
        .thenReturn(
            new IntegrationConnection(
                IntegrationProvider.WITHINGS,
                IntegrationStatus.CONNECTED,
                CONNECTED_AT,
                SYNCED_AT,
                new SyncOutcome(
                    SyncResult.ERROR,
                    0,
                    0,
                    "El proveedor no está disponible temporalmente, inténtalo más tarde.")));

    mockMvc
        .perform(post("/api/v1/integrations/withings/sync"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("ERROR"))
        .andExpect(jsonPath("$.importedCount").value(0))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("token"))));
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
            ConnectResult.authorizationRequired(
                "https://account.withings.com/oauth2_user/authorize2?client_id=test&state=abc&code_challenge=xyz"));

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
  void callbackResponseNeverContainsATokenCodeOrStateField() throws Exception {
    // FOR-131 tests.md: assert explicitly across callback that no token/code/state leaks.
    when(service.callback(IntegrationProvider.WITHINGS, "withings-auth-code", "the-state-value"))
        .thenReturn(
            new IntegrationConnection(
                IntegrationProvider.WITHINGS,
                IntegrationStatus.CONNECTED,
                CONNECTED_AT,
                null,
                null));

    mockMvc
        .perform(
            post("/api/v1/integrations/withings/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"withings-auth-code\",\"state\":\"the-state-value\"}"))
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
                        org.hamcrest.Matchers.containsStringIgnoringCase("secret"))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("withings-auth-code"))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("the-state-value"))));
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
                    new SyncOutcome(SyncResult.OK, 0, 0, null))));

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
