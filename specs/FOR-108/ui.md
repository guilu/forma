# FOR-108 UI Spec

No UI — backend/read-model story.

## API surface

`GET /api/v1/shopping/list` (FOR-39) response gains, per item: `unit`,
`servings` (nullable); and at the list level: `generatedAt`. No new routes,
no breaking changes to existing fields (`id`, `productId`, `productName`,
`category`, `quantity`, `estimatedCostEur`, `checked`, `budget`).

FOR-117 consumes these fields to render a richer item line; it owns its own
`ui.md`.
