package dev.diegobarrioh.forma.domain;

/**
 * An exclusion rule a plan request has to respect (ADR-015 decision 6, {@code
 * docs/FORMA_Contrato_Agente_Plan.md}).
 *
 * <p>Orthogonal to {@link CuisineStyle} on purpose: this says what is excluded, that says what
 * cuisine to lean on, and a single field cannot say both — a vegan Mediterranean diet is not a
 * contradiction. {@link #UNSPECIFIED} is a real, storable answer for "nobody said", distinct from
 * omitting the question: {@code plan_request.diet_pattern} is {@code NOT NULL}.
 */
public enum DietPattern {
  OMNIVORE,
  VEGETARIAN,
  VEGAN,
  GLUTEN_FREE,
  UNSPECIFIED
}
