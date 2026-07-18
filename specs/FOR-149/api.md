# FOR-149 API Spec

> Extends the existing profile read model (FOR-107, `/api/v1/profile`) with the new personal targets.
> Confirm the exact path against `ApiPaths.java` (`/api/v1`) and the existing profile controller. Aligns with ADR-005.

## Endpoints

### GET /api/v1/profile

Returns the single-user profile including the new personal targets (base kcal, fat/weight target
ranges, protein/fat/carb targets). Extends the FOR-107 read model — no new endpoint if profile
already exposes objectives; add the target fields to the existing payload.

## Request

`GET /api/v1/profile` — no body. Owner is the fixed OWNER_ID (ADR-002).

## Response

```json
{
  "ownerId": "…",
  "name": "Diego",
  "heightCm": 180.0,
  "targets": {
    "baseCaloriesKcal": 2300,
    "bodyFatTargetMinPct": 12.0,
    "bodyFatTargetMaxPct": 13.0,
    "weightTargetMinKg": 73.0,
    "weightTargetMaxKg": 75.0,
    "proteinTargetG": 160,
    "fatTargetG": 70,
    "carbsTargetG": 260
  }
}
```
- Field names/nesting to match the existing profile DTO conventions; the block above is the data contract, not a mandated shape.
- Unseeded profile → target fields null (not 404).

## Errors

- No client input beyond auth context. Standard 500 on persistence failure.
- No 404: a missing row returns defaults (FOR-107 behavior).

## Authorization

Single-user MVP (ADR-002), owner-scoped; consistent with the existing `/api/v1/profile` endpoint.

## Validation

- Targets non-negative when present; range min ≤ max.
- No request body (read model). Writes (if any) go through the existing profile update path — out of scope for this slice's seed.
