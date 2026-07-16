package dev.diegobarrioh.forma.delivery.progress;

import dev.diegobarrioh.forma.application.Adherence;
import dev.diegobarrioh.forma.domain.CategoryAdherence;
import java.time.LocalDate;
import java.util.List;

/**
 * Response body for {@code GET /api/v1/progress/adherence?days=} (FOR-129 api.md).
 *
 * <p>Delivery read model, distinct from the application {@link Adherence} view (ADR-005), mirroring
 * {@code DayConsumptionResponse}'s (FOR-127) from-view convention. {@code rate} serializes as JSON
 * {@code null} when the category has nothing planned (spec FOR-129: never a divide-by-zero).
 */
public record AdherenceResponse(
    int windowDays, LocalDate from, LocalDate to, List<CategoryAdherenceResponse> categories) {

  public record CategoryAdherenceResponse(
      String category, int planned, int completed, Double rate) {

    static CategoryAdherenceResponse from(CategoryAdherence categoryAdherence) {
      return new CategoryAdherenceResponse(
          categoryAdherence.category().name(),
          categoryAdherence.planned(),
          categoryAdherence.completed(),
          categoryAdherence.rate());
    }
  }

  public static AdherenceResponse from(Adherence adherence) {
    return new AdherenceResponse(
        adherence.windowDays(),
        adherence.from(),
        adherence.to(),
        adherence.categories().stream().map(CategoryAdherenceResponse::from).toList());
  }
}
