package dev.diegobarrioh.forma.delivery.integrations;

import java.util.List;

/**
 * Response body for {@code GET /api/v1/integrations} (FOR-126 api.md): {@code {"providers":
 * [...]}}.
 */
public record IntegrationsListResponse(List<IntegrationConnectionResponse> providers) {}
