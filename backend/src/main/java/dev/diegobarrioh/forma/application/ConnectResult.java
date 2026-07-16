package dev.diegobarrioh.forma.application;

import dev.diegobarrioh.forma.domain.IntegrationConnection;

/**
 * Result of {@link IntegrationService#connect} (FOR-131): exactly one of {@link #authorizationUrl}
 * or {@link #connection} is set, never both.
 *
 * <p>Two connect behaviors coexist in this slice (documented scope decision, spec FOR-131 only
 * covers Withings): a provider with a registered {@link ProviderOAuthGateway} (Withings) starts a
 * real OAuth round-trip and returns {@link #authorizationUrl}; a provider without one (Google Fit,
 * Apple Health — their OAuth apps are out of scope for FOR-131 and do not exist yet) falls back to
 * the FOR-126 mock immediate-connect and returns the resulting {@link #connection}. This keeps
 * FOR-126 behavior unchanged for providers this story does not touch, rather than inventing OAuth
 * plumbing for providers with no registered app.
 */
public record ConnectResult(String authorizationUrl, IntegrationConnection connection) {

  public static ConnectResult authorizationRequired(String authorizationUrl) {
    return new ConnectResult(authorizationUrl, null);
  }

  public static ConnectResult connected(IntegrationConnection connection) {
    return new ConnectResult(null, connection);
  }
}
