# API Notes: FOR-80

## Applicability

This story may add only a smoke or health-adjacent endpoint if useful for startup verification.

## Constraints

- Do not create product endpoints.
- Do not expose domain data.
- Do not expose stack traces or internal configuration.
- Keep any endpoint clearly marked as infrastructure/bootstrap behavior.

## Suggested Shape

- A minimal health/smoke response can return service status and version/build metadata if already available.
- Do not invent a broader API contract here; FOR-88 owns the API skeleton and error baseline.
