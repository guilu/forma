package dev.diegobarrioh.forma.domain;

/**
 * The cuisine a plan request leans toward (ADR-015 decision 6, {@code
 * docs/FORMA_Contrato_Agente_Plan.md}).
 *
 * <p>Orthogonal to {@link DietPattern}: this is a cuisine, that is an exclusion rule. {@link
 * #UNSPECIFIED} is a real, storable answer — {@code plan_request.cuisine_style} is {@code NOT NULL}
 * — distinct from the funnel's {@code eatingStyle} free-form string ({@code PlanDraftRequest}),
 * which this does not replace.
 */
public enum CuisineStyle {
  ESPANOLA,
  MEDITERRANEA,
  UNSPECIFIED
}
