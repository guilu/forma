# FOR-37 AI Context

## Story

FOR-37 — Create weekly shopping list model
(https://dbhlab.atlassian.net/browse/FOR-37)

## Intent

Model a weekly shopping list of product items that can be checked and budgeted.
Success is `ShoppingList` + `ShoppingListItem` domain types referencing FOR-35
products, ready for FOR-38 budget and FOR-39 checklist.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Shopping → ShoppingItem)
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-003-persistence.md`,
  `docs/adr/ADR-007-testing.md`
- `specs/FOR-35/`, `specs/FOR-36/` (products these items reference)
- `specs/FOR-16/` (persistence precedent, if lists are persisted)
- Jira: https://dbhlab.atlassian.net/browse/FOR-37

## Domain Notes

- `ShoppingListItem` references a `ShoppingProduct` by id (FOR-35), not by
  embedding it.
- A `ShoppingList` is keyed by `weekStartDate`; `status` is a small enum.
- Checked state is user data — if a checklist persists it, the list is a
  persisted mutable entity (like FOR-36 products), not an in-code catalog.

## Architectural Constraints

- Types in `.../domain/`, framework-free (ADR-001), no ORM.
- If persisted, add a Flyway migration after the latest present (never edit
  existing migrations — ADR-003); use `NUMERIC` for cost.
- Keep cost derivation consistent with FOR-38 (store vs. derive).

## Common Pitfalls

- Free-form `status` string instead of an enum.
- Embedding product/nutrition data into the item instead of referencing by id.
- Rigidly requiring generation from nutrition templates (that is a later story).
- Floating-point money.

## Suggested Implementation Order

1. Define the `status` enum and the `ShoppingListItem` / `ShoppingList` records.
2. Add construction-time validation (positive `quantity`, required refs).
3. Decide cost source (store vs. derive) and persistence timing; document both.
4. Unit-test creation, check/uncheck, and multi-item lists.

## Validation

Run `./gradlew test` from `backend/`.
