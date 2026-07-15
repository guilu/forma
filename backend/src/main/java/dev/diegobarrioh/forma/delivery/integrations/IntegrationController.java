package dev.diegobarrioh.forma.delivery.integrations;

import dev.diegobarrioh.forma.application.IntegrationService;
import dev.diegobarrioh.forma.application.ValidationException;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.IntegrationProvider;
import java.util.Locale;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration connection REST endpoints (FOR-126, first implementable slice of FOR-103) under
 * {@link ApiPaths#V1}{@code /integrations}: per-provider status, and connect/disconnect/manual-sync
 * commands that actually resolve — replacing the FOR-57 frontend mock and its {@code
 * Promise<never>} calls with a real contract. No OAuth, no tokens, no real provider sync this slice
 * (spec FOR-126 Summary).
 *
 * <p>Thin controller (ADR-001, ADR-005): parses the {@code provider} path segment to the domain
 * enum and delegates all behavior to {@link IntegrationService}. Never accepts or returns
 * domain/persistence types directly, and never returns a token/secret (ADR-004) — the domain and
 * DTOs it flows through carry none.
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
   * Marks the provider connected (mock, no OAuth this slice); idempotent when already connected.
   */
  @PostMapping("/{provider}/connect")
  public ConnectionStatusResponse connect(@PathVariable String provider) {
    return ConnectionStatusResponse.from(service.connect(parseProvider(provider)));
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

  /** Marks the provider disconnected; idempotent no-op when already disconnected. */
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
