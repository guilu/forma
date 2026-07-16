package dev.diegobarrioh.forma.delivery.integrations;

import dev.diegobarrioh.forma.application.ConnectResult;
import dev.diegobarrioh.forma.application.IntegrationService;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration connection REST endpoints under {@link ApiPaths#V1}{@code /integrations}:
 * per-provider status, and connect/callback/disconnect/manual-sync commands. FOR-126 (first
 * implementable slice of FOR-103) shipped status + a mock connect/disconnect/manual-sync shell;
 * FOR-131 (slice 2) adds real Withings OAuth — {@code connect} now returns an authorization URL for
 * providers with a registered OAuth gateway (Withings) instead of connecting immediately, and the
 * new {@code callback} endpoint is what the SPA calls (after the browser lands on its {@code /auth}
 * route) to complete the exchange. Real provider sync into {@code BodyMeasurement} is still FOR-103
 * slice 3; {@code sync} stays the FOR-126 stub.
 *
 * <p>Thin controller (ADR-001, ADR-005): parses the {@code provider} path segment to the domain
 * enum and delegates all behavior to {@link IntegrationService}. Never accepts or returns
 * domain/persistence types directly, and never returns a token/secret/authorization {@code code}
 * (ADR-004, spec FOR-131 api.md) — the DTOs it flows through carry none.
 *
 * <p>Single-user MVP (ADR-002): every endpoint operates on the one account {@link
 * IntegrationService} resolves internally; no account/owner path segment or auth header is accepted
 * yet — a known, documented MVP limitation (AGENTS.md), not an oversight.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/integrations")
public class IntegrationController {

  private final IntegrationService service;

  public IntegrationController(IntegrationService service) {
    this.service = service;
  }

  /**
   * Per-provider status, last-sync time and last-sync outcome. Every known provider is listed,
   * defaulting to {@code DISCONNECTED} before any connection — empty is never the result and 404 is
   * never returned (spec FOR-126 Edge Cases).
   */
  @GetMapping
  public IntegrationsListResponse list() {
    return new IntegrationsListResponse(
        service.status().stream().map(IntegrationConnectionResponse::from).toList());
  }

  /**
   * Starts connecting the provider (FOR-131 api.md, changed from FOR-126). For a provider with a
   * registered OAuth gateway (Withings), returns an authorization URL and no longer flips status
   * directly. Providers without one (Google Fit, Apple Health — out of scope for FOR-131) keep the
   * FOR-126 mock immediate-connect behavior.
   */
  @PostMapping("/{provider}/connect")
  public ConnectResponse connect(@PathVariable String provider) {
    ConnectResult result = service.connect(parseProvider(provider));
    return ConnectResponse.from(result);
  }

  /**
   * Completes an OAuth round trip (FOR-131 api.md, new). The SPA calls this after the browser lands
   * on its {@code /auth} route with {@code code}/{@code state} from the Withings redirect — this
   * endpoint is never called by Withings or the browser directly. Validates {@code state},
   * exchanges {@code code} for tokens, stores them encrypted, and marks the connection CONNECTED.
   */
  @PostMapping("/{provider}/callback")
  public ConnectionStatusResponse callback(
      @PathVariable String provider, @Valid @RequestBody CallbackRequest request) {
    return ConnectionStatusResponse.from(
        service.callback(parseProvider(provider), request.code(), request.state()));
  }

  /**
   * Triggers a manual sync now; returns a real outcome (stub/no-op import, {@code importedCount}
   * never fabricated). Syncing a disconnected provider returns a readable {@code NOT_CONNECTED}
   * outcome rather than an error (spec FOR-126 Edge Cases).
   */
  @PostMapping("/{provider}/sync")
  public SyncResponse sync(@PathVariable String provider) {
    return SyncResponse.from(service.sync(parseProvider(provider)));
  }

  /**
   * Marks the provider disconnected and forgets any stored tokens (FOR-131 api.md, changed from
   * FOR-126: "Disconnect now also revokes/forgets the stored tokens"); idempotent no-op when
   * already disconnected.
   */
  @DeleteMapping("/{provider}")
  public ConnectionStatusResponse disconnect(@PathVariable String provider) {
    return ConnectionStatusResponse.from(service.disconnect(parseProvider(provider)));
  }

  /**
   * Converts the path segment to a known {@link IntegrationProvider}, or throws {@link
   * ValidationException} (mapped to 400 {@code VALIDATION_ERROR} by {@code GlobalExceptionHandler})
   * for an unrecognized value (spec FOR-126 api.md: "Unknown provider path value → 400
   * VALIDATION_ERROR" — deliberately not the 404-for-unknown-enum precedent used by {@code
   * WorkoutController}/{@code NutritionController}, since this story's spec explicitly requires 400
   * here).
   */
  private static IntegrationProvider parseProvider(String raw) {
    try {
      return IntegrationProvider.valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new ValidationException("Proveedor de integración desconocido: " + raw);
    }
  }
}
