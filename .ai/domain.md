# FORMA AI Domain Context

## Domain summary

FORMA connects body composition, training, nutrition, shopping and insights.

## Important rules

- Body weight alone is not enough to evaluate progress.
- Smart-scale data can be noisy; avoid fake precision.
- Training and nutrition should be evaluated together.
- Running, strength and rest days may have different nutrition needs.
- External provider data must be normalized before entering the domain.
- Insights must be explainable and actionable.
- FORMA is not a medical diagnosis product.

## Core records

- Measurement
- Training session
- Nutrition plan
- Meal template
- Product
- Shopping list
- Insight
- Integration connection
- Synchronization status

## Source handling

Records may come from manual entry or external providers. The source should be visible when it matters, but provider-specific payloads should not become the core domain model.
