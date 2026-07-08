# FOR-37: Create weekly shopping list model

Jira: https://dbhlab.atlassian.net/browse/FOR-37
Epic: FOR-5 Shopping Assistant

## Summary

Create the `ShoppingList` and `ShoppingListItem` domain models: a weekly list of
product items (each with quantity, estimated cost and a checked flag) that can be
budgeted (FOR-38) and shown as a checklist (FOR-39). Items reference FOR-35/FOR-36
products by id.

## User/System Flow

This story has no user-facing flow yet. It defines the types consumed by later
stories:

1. FOR-38 sums item costs into weekly/monthly budgets.
2. FOR-39 renders the list, letting the user check/uncheck items.

## Functional Requirements

- Add `ShoppingList` and `ShoppingListItem` under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`, framework-free
  (ADR-001), per docs/domain-model.md ("ShoppingItem").
- `ShoppingList` fields: `weekStartDate`, `status`, `notes`.
- `ShoppingListItem` fields: product reference (`productId`), `quantity`,
  `estimatedCostEur`, `checked` status.
- Items reference shopping products (FOR-35) by id.
- Items can be checked / unchecked.
- Estimated item cost can be stored or calculated (from product price ×
  quantity) — document the rule.
- `quantity` is flexible enough for units or package counts.

## Non-Functional Requirements

- Framework-free value types. Persistence, if needed for FOR-38/FOR-39, follows
  the FOR-16/FOR-36 pattern (additive Flyway migration) — see Open Questions.
- Deterministic.

## Data Model Notes

Mirrors docs/domain-model.md's `ShoppingItem` (grouped under a `ShoppingList`
keyed by `weekStartDate`). `status` is a small set (e.g. DRAFT / ACTIVE / DONE) —
model as an enum; confirm the exact values. First version may be manually created
or seeded; automatic generation from nutrition templates is a later concern.

## Edge Cases

- `quantity` zero or negative — reject at construction.
- Empty list (no items) — valid (a fresh weekly list).
- `estimatedCostEur` stored vs. derived — keep consistent with FOR-38.

## Open Questions

- **Status values**: define the `ShoppingList.status` enum (e.g. `DRAFT`,
  `ACTIVE`, `DONE`) — pick and document.
- **Persistence**: whether lists are persisted now or in FOR-38/FOR-39. Since a
  checklist (FOR-39) needs to persist checked state, recommend persisting the
  list (Flyway table after the latest migration) — document when it lands.
- **Cost source**: store `estimatedCostEur` on the item vs. derive from the
  product price × quantity at read time — document (affects FOR-38).
