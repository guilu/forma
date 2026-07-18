# FOR-150 API Spec

> No new endpoint. The 6 rules surface through the existing insights/recommendations read model
> (assembled by `AdherenceService`, exposed by the progress/insights controller). Confirm the exact
> path against `ApiPaths.java` (`/api/v1`) and the existing insights endpoint. Aligns with ADR-005.

## Endpoints

### GET /api/v1/insights (existing — confirm path)

Returns the current weekly `Recommendation`s. This slice changes the *content* (thresholds, amounts,
4 new rules), not the contract.

## Request

`GET` — no body. Owner is the fixed OWNER_ID (ADR-002).

## Response

```json
{
  "recommendations": [
    {
      "category": "BODY",
      "severity": "ACTION",
      "message": "El peso baja rápido; sube 100–150 kcal para no perder masa magra.",
      "reason": "El peso baja 0.5 kg/semana, por encima del límite de 0.4 kg/semana.",
      "sourceField": "weeklyWeightChangeKg"
    },
    {
      "category": "SHOPPING",
      "severity": "ACTION",
      "message": "Coste alto; cambia salmón por merluza/atún/huevos.",
      "reason": "La compra semanal supera 120 €.",
      "sourceField": "weeklyCostEur"
    }
  ]
}
```
- Field names/categories follow the existing `Recommendation` DTO; categories for new rules (e.g. cost, hunger, running-HR) to match the existing enum or extend it consistently.
- Rules gated on missing data (4/5/6, possibly 3) simply do not appear until their source slice lands.

## Errors

- No client input beyond auth context. No new error cases introduced by this slice.

## Authorization

Single-user MVP (ADR-002), owner-scoped; consistent with the existing insights endpoint.

## Validation

- No request body. Rule inputs are validated in the domain (thresholds), not the API layer.
