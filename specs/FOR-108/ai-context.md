# FOR-108 AI Context

## Story

FOR-108 — Expose shopping item quantity, unit & servings over HTTP
(https://dbhlab.atlassian.net/browse/FOR-108)

## Intent

FOR-106 already threaded `productId` and `category` onto the shopping list
read model so the UI can group/filter and resolve edits by id (FOR-111 picks
that up). This story is the direct sibling: add `unit` and `servings` per
item and `generatedAt` on the list, so FOR-117 can show a richer item line
and a "generated on" timestamp instead of the current bare
`quantity`/`estimatedCostEur` line.

## Blocked by

None. This story blocks: FOR-117.

## Relevant Documents

- `AGENTS.md`
- `docs/architecture-overview.md`
- `docs/adr/ADR-001-architecture.md` (framework-free domain, thin
  controllers)
- `docs/adr/ADR-003-persistence.md` (migration-driven schema)
- `docs/adr/ADR-005-api-design.md` (DTOs distinct from domain)
- `specs/FOR-106/spec.md` and `specs/FOR-106/api.md` — the read-model
  enrichment this story mirrors field-by-field
- Jira: https://dbhlab.atlassian.net/browse/FOR-108

## Domain Notes

- `backend/src/main/java/dev/diegobarrioh/forma/domain/ShoppingListItem.java`
  — record `(productId, quantity, estimatedCostEur, checked)`, no unit or
  servings field.
- `backend/.../domain/ShoppingList.java` — record `(weekStartDate, status,
  items, notes)`, no timestamp field.
- `backend/.../application/ShoppingListView.java` — the `Entry` record to
  extend, same file FOR-106 extended for `productId`/`category`.
- `backend/.../domain/ShoppingCategory.java` — the enum pattern FOR-106
  used; a `unit` enum should follow the same shape if the enum approach is
  chosen (see Open Questions in `spec.md`).

## Architectural Constraints

- Domain stays framework-free (ADR-001); no persistence/DTO leakage into
  `ShoppingListItem`/`ShoppingList`.
- Extend, don't replace, the existing FOR-39 response contract — additive
  fields only.
- One `ALTER TABLE ADD COLUMN` per migration statement (H2 gotcha from
  FOR-100/FOR-106).

## Common Pitfalls

- Breaking the existing FOR-39 checklist/budget contract while adding
  fields — run the existing shopping test suite, not just new tests.
- Fabricating `servings` for non-food items instead of returning `null`.
- Forgetting to backfill `generatedAt` for lists that predate the migration.

## Suggested Implementation Order

1. Domain field additions (`unit` on `ShoppingListItem`, `generatedAt` on
   `ShoppingList`) + migration.
2. Application: extend `ShoppingListView.Entry`/`ShoppingListView` to carry
   the new fields, resolving `servings` from the linked product where
   applicable.
3. Delivery: extend `ShoppingListResponse`/`ShoppingListResponse.Item`.
4. Tests per `tests.md`; confirm FOR-38/FOR-39 behavior is unchanged.

## Validation

Backend build + tests (AGENTS.md Verification guidance). Call `GET /api/v1/
shopping/list` before/after and confirm the new fields appear without
altering existing field values.
